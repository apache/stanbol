package org.apache.stanbol.entityhub.indexing.rdf;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import java.util.zip.GZIPInputStream;

import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipFile;
import org.apache.commons.compress.compressors.bzip2.BZip2CompressorInputStream;
import org.apache.stanbol.entityhub.core.mapping.DefaultFieldMapperImpl;
import org.apache.stanbol.entityhub.core.mapping.FieldMappingUtils;
import org.apache.stanbol.entityhub.core.mapping.ValueConverterFactory;
import org.apache.stanbol.entityhub.core.site.CacheUtils;
import org.apache.stanbol.entityhub.core.utils.ModelUtils;
import org.apache.stanbol.entityhub.servicesapi.mapping.FieldMapper;
import org.apache.stanbol.entityhub.servicesapi.mapping.FieldMapping;
import org.apache.stanbol.entityhub.servicesapi.model.Representation;
import org.apache.stanbol.entityhub.servicesapi.model.ValueFactory;
import org.apache.stanbol.entityhub.servicesapi.model.rdf.RdfResourceEnum;
import org.apache.stanbol.entityhub.servicesapi.yard.Yard;
import org.apache.stanbol.entityhub.servicesapi.yard.YardException;
import org.openjena.riot.Lang;
import org.openjena.riot.RiotReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.hp.hpl.jena.datatypes.BaseDatatype;
import com.hp.hpl.jena.datatypes.DatatypeFormatException;
import com.hp.hpl.jena.datatypes.RDFDatatype;
import com.hp.hpl.jena.datatypes.xsd.XSDDateTime;
import com.hp.hpl.jena.datatypes.xsd.XSDDuration;
import com.hp.hpl.jena.graph.Node;
import com.hp.hpl.jena.graph.Triple;
import com.hp.hpl.jena.graph.impl.LiteralLabel;
import com.hp.hpl.jena.query.Query;
import com.hp.hpl.jena.query.QueryExecutionFactory;
import com.hp.hpl.jena.query.QueryFactory;
import com.hp.hpl.jena.query.QuerySolution;
import com.hp.hpl.jena.query.ResultSet;
import com.hp.hpl.jena.query.Syntax;
import com.hp.hpl.jena.rdf.model.Literal;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.tdb.TDBFactory;
import com.hp.hpl.jena.tdb.TDBLoader;
import com.hp.hpl.jena.tdb.base.file.Location;
import com.hp.hpl.jena.tdb.store.DatasetGraphTDB;
import com.hp.hpl.jena.tdb.store.bulkloader.BulkLoader;
import com.hp.hpl.jena.tdb.store.bulkloader.Destination;
import com.hp.hpl.jena.tdb.store.bulkloader.LoadMonitor;
import com.hp.hpl.jena.tdb.store.bulkloader.LoaderNodeTupleTable;
import com.hp.hpl.jena.util.iterator.ExtendedIterator;


/**
 * This Class indexes Entities based on Information provided in a RDF Graph
 *
 * Features (currently Brainstorming)<ul>
 * <li> Parse also Archive Files (nobody likes to extract such stuff)
 * <li> Parse different RDF formats (esay with Clerezza)
 * <li> Replace/Append Mode: In replace Mode existing data for an Entity are
 *      replaced in the Yard. In the Append Mode first Data for a found Entity
 *      are loaded and than new data are added. The Append Mode is important when
 *      working with dumps that split up data not by entity but by properties
 * <li> Support for the Entityhub Representation Mapping Infrastructure (currently
 *      this means using the {@link FieldMapper}
 * <li> Support for filtering Entities based on rdf:type (will be in future
 *      version supported by the Entityhub Representation Mapping Infrastructure)
 * <li> Entity Rank support: It is not feasible to calculate the rankings in a
 *      generic fashion. Therefore this implementation supports two different
 *      ways. First it uses {@link RdfResourceEnum#signRank} relations in the
 *      parsed RDF Data and secondly it allows to provide entity rank information
 *      by parsing a map with the id as key and the rank as value by using the
 *      {@link #KEY_ENTITY_RANKINGS} in the configuration. Mappings parsed by the
 *      map will override mappings in the RDF data.
 * <li>
 * <li>
 * <li>
 * </ul>
 *  Implementation Notes:<ul>
 * <li> The Idea was to use the Clerezza TdbTcProvider and the JenaParserProvider
 *      outside of an OSGI Environment and than use the Clerezza Yard to
 *      access Resources to be indexed directly as Representations to perform
 *      the field mappings by using the parsed {@link FieldMapper}.
 * <li> I am a bit worried with the ParsingProvide} because the interface
 *      allows no streaming. One needs to check if this causes problems for
 *      very big RDF files.<br>
 *        After looking into the source code of Clerezza, I am even more worried!
 *      The Parser creates an SimpleMGraph and wrapped it with an
 *      Jena Adapter. Than all Triples are parsed into memory. So I do not only
 *      have the triples in memory but also the instances of the jena adapter.
 * <li> Maybe it is better to directly use Jena, because Clerezza does not support
 *      COUNT(..) for SPARQL queries. Without that feature it would be hard to
 *      support page ranks!<br>
 *      Maybe I can use both, because using a Clerezza wrapper over Jena would
 *      have the advantage to directly use the ClerezzaYard for creating
 *      RDF Representations for resource to be indexed!<br>
 *      Parsing and especially SPARQL queries would be better to do directly
 *      via the Jena API!<p>
 *      This should be no problem, because indexing is a read only operation!
 * <li> However based on this findings I plan now to implement the loading of the
 *      RDF data to directly use the Jena TDB API because Clerezza seams not to
 *      be designed to handle the loading of RDF datasets that can not be
 *      kept in memory.<br>
 * </ul>
 *
 * @author Rupert Westenthaler
 * @author ogrisel (as parts of that code is taken from iks-autotagger)
 *
 */
public class RdfIndexer {
    /**
     * The indexing mode defines if the RDF data are appended to existing
     * {@link Representation}s in the target {@link Yard} or if {@link Representation}
     * are replaced with RDF data used for indexing!
     * @author Rupert Westenthaler
     *
     */
    public static enum IndexingMode{ NORMAL, RANKING_MAP_BASED }

    public static final String RDF_XML = "application/rdf+xml";
    public static final String TURTLE = "text/turtle";
    public static final String X_TURTLE = "application/x-turtle";
    public static final String N_TRIPLE = "text/rdf+nt";
    public static final String N3 = "text/rdf+n3";
    public static final String RDF_JSON = "application/rdf+json";
    //both html and xhtml can be rdf formats with RDFa
    public static final String XHTML = "application/xhtml+xml";
    public static final String HTML = "text/html";

    protected static final String resourceQuery;
    static {
        StringBuilder cqb = new StringBuilder();
        cqb.append("SELECT ?field ?value ");//, count(distinct ?incoming) AS ?count ");
        cqb.append("{ ");
        cqb.append("  <%s> ?field ?value .");
//        cqb.append("  OPTIONAL {?incoming ?relationship <%s> . } .");
        cqb.append("} ");
        resourceQuery = cqb.toString();
    }

    Logger log = LoggerFactory.getLogger(RdfIndexer.class);
    /**
     * Key used to parse the Yard used for indexing
     */
    public static final String KEY_YARD = "org.apache.stanbol.entityhub.indexing.yard";
    /**
     * Key used to parse reference(s) to the RDF files to be indexed!<p>
     * This supports both single values as well as {@link Iterable}s over several
     * values. All parsed sources are loaded within one TripleStore and are
     * indexed at once! Use several {@link RdfIndexer} instances to index them
     * one after the other.
     */
    public static final String KEY_RDF_FILES = "org.apache.stanbol.entityhub.indexing.rdf.rdfFiles";
    /**
     * Key used to configure the fieldMappings used to determine what properties
     * are indexed for entities. Values must implement {@link Iterable} and the
     * {@link Object#toString()} is used to parse the different mappings.
     */
    public static final String KEY_FIELD_MAPPINGS = "org.apache.stanbol.entityhub.indexing.rdf.fieldMappings";

    /**
     * Key used to configure the directory to store RDF data needed during the
     * indexing process. This data might be reused when resuming an indexing
     * process.
     */
    public static final String KEY_RDF_STORE_DIR = "org.apache.stanbol.entityhub.indexing.rdf.indexingDir";
    /**
     * Key used to configure the name of the model used to store the parsed
     * RDF data before the indexing process. Parsing this name can be used to
     * resume indexing based on previously parsed RDF data.
     */
    public static final String KEY_MODEL_NAME = "org.apache.stanbol.entityhub.indexing.rdf.modelName";
    /**
     * Key used to parse the Iterable over all the rdf:types to be indexed. If
     * not parsed or set to <code>null</code> or an empty list, than all Resources are
     * accepted!
     * The {@link Object#toString()} method is used on elements to get the actual type!
     */
    public static final String KEY_RDF_TYPES = "org.apache.stanbol.entityhub.indexing.rdf.indexedTypes";
	/**
	 * Expert only: This allows to set the indexing based on the keys in the map parsed with the
	 * entity rankings. This will only index entities that are keys in that map.
	 * If no Map is parsed by {@link #KEY_ENTITY_RANKINGS}, than activating this mode
	 * will not be successful and a warning will be written.<p>
	 * This mode is about 50% slower than the usual indexing mode. Therefore this
	 * mode makes only sense id less than 50% of the entities are indexed.
	 */
	public static final String KEY_INDEXING_MODE = "org.apache.stanbol.entityhub.indexing.rdf.indexingMode";
	/**
	 * If <code>true</code> than no RDF data are loaded. Instead it is assumed, that
	 * the Graph of the parsed {@link #KEY_MODEL_NAME} already contains all the needed
	 * data!<p>
	 * This can be useful if one first wants to index rdf:type A and than rdf:type B
	 * based on the same set of data
	 */
	public static final String KEY_SKIP_READ = "org.apache.stanbol.entityhub.indexing.rdf.skipRead";
	/**
	 * The number of {@link Representation}s stored at once in the SolrYard!
	 */
	public static final String KEY_CHUNK_SIZE = "org.apache.stanbol.entityhub.indexing.rdf.chunkSize";
	/**
	 * Can be used to parse a map with {@link String} entity id, {@link Float} rank
	 * for entities.<p>
	 * Such values are added to Representations for the {@link RdfResourceEnum#signRank}
	 * field.
	 */
	public static final String KEY_ENTITY_RANKINGS = "org.apache.stanbol.entityhub.indexing.rdf.entityRankings";
	/**
	 * Can be used to activate ignoring of Entities without a page rank
	 */
	public static final String KEY_IGNORE_ENTITIES_WITHOUT_ENTITY_RANKING = "org.apache.stanbol.entityhub.indexing.rdf.ignoreEntitiesWithoutRankings";
	/**
	 * If set to a value >= 0 this is used to exclude Entities with a lower or 
	 * missing entity rank
	 */
	public static final String KEY_REQUIRED_ENTITY_RANKING = "org.apache.stanbol.entityhub.indexing.rdf.requiredRanking";
	/**
	 * The rank for entities with a missing rank. This takes only effect if
	 * {@link #KEY_IGNORE_ENTITIES_WITHOUT_ENTITY_RANKING} is set to <code>false</code>
	 * (the default)
	 */
	public static final String KEY_DEFAULT_ENTITY_RANKING = "org.apache.stanbol.entityhub.indexing.rdf.defaultRanking";
	/**
	 * The resume Mode first checks if a Resource is already present in the parsed
	 * Yard. If this is the case, than the representation is not indexes again.<p>
	 * This mode is intended to resume indexing after stopping a previous call before
	 * finished. The default value = false.
	 */
	public static final String KEY_RESUME_MODE = "org.apache.stanbol.entityhub.indexing.rdf.resumeMode";
	
	private final IndexingMode indexingMode;
	private final Yard yard;
	private final ValueFactory vf;
	private final List<File> rdfFiles;
	private final File indexingDir;
	private final String modelName;
//	private final ParsingProvider parser = new JenaParserProvider();
	//private final WeightedTcProvider provider;
	private final FieldMapper mapper;
	private final Set<String> types;
	//private MGraph indexingGraph;
	private final DatasetGraphTDB indexingDataset;
	private final boolean skipRead;
	private Location modelLocation;
	private int indexingChunkSize = 1000;
	
	//vars for entity rankings
	private Map<String,Float> entityRankings = null;
	private boolean ignoreEntitiesWithoutRank = false;
	private float defaultEntityRanking = -1;
	private String entityRankingField = RdfResourceEnum.signRank.getUri();
	private float minimumRequiredEntityRanking = -1;
	private boolean resumeMode;
	
	
	public RdfIndexer(Dictionary<String, Object> config){
		this.yard = (Yard)config.get(KEY_YARD);
		if(yard == null){
			throw new IllegalArgumentException("Parsed config MUST CONTAIN a Yard. Use the key "+KEY_YARD+" to parse the YardInstance used to store the geonames.org index!");
		} else {
			log.info(String.format("Using Yard %s (id=%s) to index parsed RDF data",
					yard.getName(),yard.getId()));
		}
		this.vf = yard.getValueFactory();
		Object rdfFiles = config.get(KEY_RDF_FILES);
		if(rdfFiles instanceof Iterable<?>){
			this.rdfFiles = new ArrayList<File>();
			for(Object value : (Iterable<?>)rdfFiles){
				this.rdfFiles.add(checkFile(value.toString()));
			}
		} else {
			this.rdfFiles = Collections.singletonList(checkFile(rdfFiles.toString()));
		}
		Object indexingDir = config.get(KEY_RDF_STORE_DIR);
		if(indexingDir == null){
			indexingDir = "indexingData";
			config.put(KEY_RDF_STORE_DIR, indexingDir);
		}
		this.indexingDir = checkFile(indexingDir.toString(), false, true);
		Object modelName = config.get(KEY_MODEL_NAME);
		if(modelName == null){
			modelName = "indexingModel-"+ModelUtils.randomUUID().toString();
			config.put(KEY_MODEL_NAME, modelName);
		}
		this.modelName = modelName.toString();
		//init the types!
		Iterable<?> types = (Iterable<?>)config.get(KEY_RDF_TYPES);
		if(types != null){
			Set<String> typeSet = new HashSet<String>();
			for(Object type : types){
				if(type != null){
					typeSet.add(type.toString());
					log.info("  - adding Resoures with rdf:type "+type);
				}
			}
			if(typeSet.isEmpty()){
				log.info("  - adding all Types (no rdf:type based restriction for RDF Reseource present)");
				this.types = null;
			} else {
				this.types = typeSet;
			}
		} else{
			log.info("  - adding all Types (no rdf:type based restriction for RDF Reseource present)");
			this.types = null; //null or an iterable with one or more elements!
		}
		//init the indexing mode
		Object indexingMode = config.get(KEY_INDEXING_MODE);
		if(indexingMode == null){
			this.indexingMode = IndexingMode.NORMAL; //default to replace
		} else if(indexingMode instanceof IndexingMode){
			this.indexingMode = (IndexingMode)indexingMode;
		} else {
			try {
				this.indexingMode = IndexingMode.valueOf(indexingMode.toString());
			}catch (IllegalArgumentException e) {
				//catch and re-throw with a better message!
				throw new IllegalArgumentException(
						String.format("Values of KEY \"%s\" MUST BE of Type %s or the toString() value MUST BE a member of this Enumeration. If the Key is missing %s is used!",
								KEY_INDEXING_MODE,IndexingMode.class,IndexingMode.NORMAL),e);
			}
		}
		//init the fieldMapper
		Iterable<?> mappings = (Iterable<?>)config.get(KEY_FIELD_MAPPINGS);
		List<FieldMapping> fieldMappings;
		if(mappings != null){
			fieldMappings = new ArrayList<FieldMapping>();
			for(Object mappingString : mappings){
				if(mappingString != null){
					FieldMapping fieldMapping = FieldMappingUtils.parseFieldMapping(mappingString.toString());
					if(fieldMapping != null){
						fieldMappings.add(fieldMapping);
					}
				}
			}
			if(!fieldMappings.isEmpty()){
				this.mapper = new DefaultFieldMapperImpl(ValueConverterFactory.getInstance(vf));
				for(FieldMapping mapping : fieldMappings){
					mapper.addMapping(mapping);
				}
				//we need to add a mapping for the field rankings (if a mapper is present)
				mapper.addMapping(new FieldMapping(this.entityRankingField));
			} else {
				this.mapper = null;
			}
		} else {
			this.mapper = null;
		}
		File modelDir = new File(this.indexingDir,this.modelName);
		if(!modelDir.exists()){
			modelDir.mkdir();
		} else if(!modelDir.isDirectory()){
			throw new IllegalStateException(String.format("A directory for %s already exists but is not a directory!",modelDir.getAbsoluteFile()));
		} //else exists and is a dir -> nothing to do
		Object skipRead = config.get(KEY_SKIP_READ);
		if(skipRead != null){
			if(skipRead instanceof Boolean){
				this.skipRead = ((Boolean)skipRead).booleanValue();
			} else {
				this.skipRead = Boolean.parseBoolean(skipRead.toString());
			}
		} else {
			this.skipRead = false;
		}
		Integer chunkSize = (Integer)config.get(KEY_CHUNK_SIZE);
		if(chunkSize != null && chunkSize>0){
			this.indexingChunkSize = chunkSize;
		} //else use default value of 1000

		this.modelLocation = new Location(modelDir.getAbsolutePath());
		this.indexingDataset =  TDBFactory.createDatasetGraph(modelLocation) ;
		//this.provider = new IndexingModelProvider(this.indexingDir);
		
		//init entity Ranking
		try{
			this.entityRankings = (Map<String,Float>)config.get(KEY_ENTITY_RANKINGS);
		}catch (RuntimeException e) {
			log.error("Parsed Entity Rankings MUST use the form Map<String,Float>");
			System.exit(0);
		}
		Object ignore = config.get(KEY_IGNORE_ENTITIES_WITHOUT_ENTITY_RANKING);
		if(ignore != null){
			if(ignore instanceof Boolean){
				this.ignoreEntitiesWithoutRank = (Boolean)ignore;
			} else {
				this.ignoreEntitiesWithoutRank = Boolean.parseBoolean(ignore.toString());
			}
		}
		Object defaultRankingObject = config.get(KEY_DEFAULT_ENTITY_RANKING);
		if(defaultRankingObject != null){
			float defaultranking = -1;
			if(defaultRankingObject instanceof Float){
				defaultranking = (Float)defaultRankingObject;
			} else {
				try {
					defaultranking = Float.parseFloat(defaultRankingObject.toString());
				} catch (Exception e) {
					log.error("Unable to parse Float value for the Default Entity Ranking from the value parsed for the KEY_DEFAULT_ENTITY_RANKING key (value: "+defaultRankingObject+")");
					System.exit(0);
				}
			}
			this.defaultEntityRanking = defaultranking;
		}
		Object minimumRequiredRankingObject = config.get(KEY_REQUIRED_ENTITY_RANKING);
		if(minimumRequiredRankingObject != null){
			float minRanking = -1;
			if(minimumRequiredRankingObject instanceof Float){
				minRanking = (Float)minimumRequiredRankingObject;
			} else {
				try {
					minRanking = Float.parseFloat(minimumRequiredRankingObject.toString());
				} catch (Exception e) {
					log.error("Unable to parse Float value for the Minimum Required Entity Ranking from the value parsed for the KEY_DEFAULT_ENTITY_RANKING key (value: "+minimumRequiredRankingObject+")");
					System.exit(0);
				}
			}
			if(minRanking>=0){ //setting a valid required ranking automatically
				//means that entities without a rank should be ignored!
				this.ignoreEntitiesWithoutRank = true;
			}
			this.minimumRequiredEntityRanking = minRanking;
		}

		Object resumeMode = config.get(KEY_RESUME_MODE);
		if(resumeMode != null) {
			if(resumeMode instanceof Boolean){
				this.resumeMode = (Boolean)resumeMode;
			} else {
				this.resumeMode = Boolean.parseBoolean(resumeMode.toString());
			}
		} else {
			this.resumeMode = false;
		}
	}
	public void index() throws YardException{
		log.info("initialize ...");
		if(!skipRead){
			loadRdfFiles();
		} else {
			log.info(" ... skiping loading of RDF data");
		}
		if(indexingMode == IndexingMode.RANKING_MAP_BASED){
			indexRanked();
		} else {
			indexResources();
		}
		writeCacheBaseConfiguration();
	}
	/**
	 * This Method is used to process the RDF Data if all Resource can be indexed,
	 * because it provides the best performance. Mainly because it reads everything
	 * from a single stream and therefore gives the OS the best opportunities to
	 * optimise file access.
	 * @throws YardException
	 */
	private void indexResources() throws YardException{
        StringBuilder qb = new StringBuilder();
        /*
         * NOTES:
         *  - selects all triples form the TDB store
         *  - GROUP BY is not needed because iteration order is anyway based
         *    on the resource (TDB uses internally a tree structure based on the
         *    subject
         *  - This is about tree times faster than first selecting all resource
         *    and than filtering for all triples with the resource. Mainly because
         *    hard disc  access is much more efficient.
         */
        qb.append("SELECT ?resource ?field ?value");
        qb.append("{ ");
        qb.append(" ?resource ?field ?value . ");
//        qb.append(" OPTIONAL { ?incoming ?relationship ?resource . } . ");
        //qb.append(" FILTER ( isURI(?resource) ) . ");
        qb.append("} ");
//        qb.append("GROUP BY ?resource "); //needed because the count counts by GROUP BY
//        qb.append("ORDER BY DESC ( ?count ) ");
//        qb.append(String.format("OFFSET 0 LIMIT %d", maxTopResources));
        Query q = QueryFactory.create(qb.toString(), Syntax.syntaxARQ);
        final ResultSet resultSet = QueryExecutionFactory.create(q, indexingDataset.toDataset()).execSelect();
        long count = 0;
        long indexed = 0;
        long lastIndexed = 0;
        long stdCount = 0;
        long indexedStdCount = 0;
        long repStdCount = 0;;
        long start = System.currentTimeMillis();
        long startCurrent = start;
        String current = null;
        Representation source = null;
        while(resultSet.hasNext()){
            stdCount++;
            repStdCount++;
            QuerySolution solution =resultSet.next();
            RDFNode subject = solution.get("resource");
            if(subject.isURIResource()){
                String resource = subject.asResource().toString();
                if(!resource.equals(current)){ //start of next resource -> index current
                    count++;
                    if(count%10000==0){
                        long thisOne = System.currentTimeMillis()-startCurrent;
                        long all = System.currentTimeMillis()-start;
                        log.info(String.format("processed %d resources (%dall %dlast indexed) in %dms (%sms/last | avg: %sms/indexed) std/resource (%s indexed| %s non indexed)",
                                count,indexed,indexed-lastIndexed,thisOne,(float)thisOne/(indexed-lastIndexed),(float)all/indexed,(float)indexedStdCount/indexed,((float)stdCount-indexedStdCount)/(count-indexed)));
                        startCurrent = System.currentTimeMillis();
                        lastIndexed = indexed;
                    }
                    if(source != null){
                        if(processRanking(source)){
                            if(resumeMode && yard.isRepresentation(source.getId())){ //resume mode check
                                //log.info("S<source Resource:\n"+ModelUtils.getRepresentationInfo(source));
                                indexed++;
                                indexedStdCount=indexedStdCount+repStdCount;
                                storeRepresentation(source);
                                //here we need todo the indexing!
                            } //else already indexed -> nothing to do
                        } // else rankging to low -> do not index
                    } //else the first item to index -> ignore
                    //init next resource
                    source = vf.createRepresentation(resource);
                    current = resource;
                    repStdCount = 0;
                }
                RDFNode fieldNode = solution.get("field");
                if(fieldNode.isURIResource()){
                    String field = fieldNode.asResource().getURI();
                    RDFNode value = solution.get("value");
                    if(value.isURIResource()){
                        source.addReference(field, value.asResource().getURI());
                    } else if(value.isLiteral()){
                        Literal literal = value.asLiteral();
                        if(literal.getDatatype() != null){
                            Object literalValue;
                            try {
                                literalValue = literal.getValue();
                            } catch (DatatypeFormatException e) {
                                log.warn(" Unable to convert "+literal.getLexicalForm()+" to "+literal.getDatatype()+"-> use lecicalForm");
                                literalValue = literal.getLexicalForm();
                            }
                            if(literalValue instanceof BaseDatatype.TypedValue){
                                source.add(field, ((BaseDatatype.TypedValue)literalValue).lexicalValue);
                            } else if(literalValue instanceof XSDDateTime) {
                                source.add(field, ((XSDDateTime)literalValue).asCalendar().getTime()); //Entityhub uses the time
                            } else if(literalValue instanceof XSDDuration) {
                                source.add(field, literalValue.toString());
                            } else {
                                source.add(field, literalValue);
                            }
                        } else {
                            String lang = literal.getLanguage();
                            if(lang != null && lang.isEmpty()){
                                lang = null;
                            }
                            source.addNaturalText(field, literal.getLexicalForm(),lang);
                        }
                    }
                }
            } else {
                log.warn(String.format("Current Subject %s is not a URI Resource -> ignored",subject));
            }
        } //end while
        long end = System.currentTimeMillis();
        log.info(String.format("%d in %dms (%sms/item | %sstd/resource)",count,end-start,""+((float)end - start)/count,""+(float)stdCount/count));
    }


    private boolean processRanking(Representation source) {
        Float ranking = entityRankings == null ? null :entityRankings.get(source.getId());
        //ignore values lower than 0
        if(ranking != null){
            if(ranking < 0){
                ranking = null;
            }
        }
        if(ranking != null && ranking > 1){
            log.warn("Parse Ranking Map contains Entity Ranking > 1 (ranking="+ranking+") for Entity "+source.getId()+" -> use 1.0 as Ranking!");
            ranking = 1f;
        }
        if(ranking == null){
            for(Iterator<Object> values =source.get(entityRankingField);values.hasNext() && ranking == null;){
                Object value = values.next();
                if(value instanceof Float){
                    ranking = (Float) value;
                } else {
                    try {
                        ranking = Float.parseFloat(value.toString());
                    } catch (NumberFormatException e) {
                        log.warn(String.format("Unable to parse the Entity Ranking from field %s=%s[type=%s] -> The Document Boost MUST BE a Float value!",entityRankingField,value,value.getClass()));
                    }
                }
            }
        } else {
            source.set(entityRankingField, ranking);
        }
        if(ranking != null && ranking > 1){
            log.warn("Parse RDF data include a entity ranking > 1 (ranking="+ranking+") for Entity "+source.getId()+" and Field "+entityRankingField+"-> use 1.0 as Ranking!");
            ranking = 1f;
        }
        if(ranking == null && this.defaultEntityRanking >= 0){
            //set to default
            ranking = defaultEntityRanking;
            source.set(entityRankingField, ranking);
        }
        if(ranking == null){
            return !ignoreEntitiesWithoutRank; //return false to ignore
        } else {
            return ranking > minimumRequiredEntityRanking;
        }

    }
    private File checkFile(String value) {
        return checkFile(value,true,false);
    }
    private File checkFile(String value,boolean file, boolean create) {
        if(value.startsWith(File.pathSeparator)){
            //remove leading path separators!
            value = value.substring(File.pathSeparator.length());
        }
        File testFile = new File(value.toString());

        if(!testFile.exists()){
            if(create){ //create
                if(file){
                    try {
                        testFile.createNewFile();
                    } catch (IOException e) {
                        throw new IllegalStateException("Unable to create File "+testFile,e);
                    }
                } else {
                    if(!testFile.mkdir()){
                        throw new IllegalStateException("Unable to create Directory "+testFile);
                    }
                }
            } else { //not found
                throw new IllegalStateException("File "+testFile.getAbsolutePath()+" does not exist!");
            }
        }
        if(file && !testFile.isFile()){
            throw new IllegalStateException("parsed file "+value+"is not a file!");
        }
        if(!file && !testFile.isDirectory()){
            throw new IllegalStateException("parsed file "+value+"is not a directory!");
        }
        if(!testFile.canRead()){
            throw new IllegalStateException("Unable to read File "+value+"!");
        }
        return testFile;
    }

    private void loadRdfFiles(){
        //TcProvider provider = new IndexingModelProvider(indexingDir);
        long start=System.currentTimeMillis();
        log.info(String.format("Loding RDF %d File%s ...",rdfFiles.size(),rdfFiles.size()>1?"s":""));
        for (File modelFile : rdfFiles) {
            long startFile = System.currentTimeMillis();
            log.info(String.format(" > loading '%s' into model '%s'...", modelFile, modelName));
            String name = modelFile.getName();
            if(name.endsWith(".zip")){
                log.info("  - processing Zip-Archive Entries:");
                try {
                    ZipFile zipArchive = new ZipFile(modelFile);
                    Enumeration<ZipArchiveEntry> entries = zipArchive.getEntries();
                    while(entries.hasMoreElements()){
                        ZipArchiveEntry entry = entries.nextElement();
                        if(!entry.isDirectory()){
                            String entryName = entry.getName();
                            log.info(String.format("     o entry '%s' into model '%s'...", entryName, modelName));
                            importRdfData(zipArchive.getInputStream(entry), entryName);
                        }
                    }
                } catch (IOException e) {
                    throw new IllegalStateException(e);
                }
            } else {
                InputStream is;
                try {
                    is = new FileInputStream(modelFile);
                } catch (FileNotFoundException e) {
                    //during init it is checked that files exists and are files and there is read access
                    //so this can only happen if someone deletes the file inbetween
                    throw new IllegalStateException(e);
                }
                importRdfData(is, name);
            }

                //add the parsed Triples to the indexing graph!
            //QUESTION: Does that load the whole file into memory?
//            indexingGraph.addAll(parser.parse(is, format, null));
            log.info(String.format("   - completed in %d seconds", (System.currentTimeMillis()-startFile)/1000));
        }
        log.info(String.format(" ... %d files imported in %d seconds", rdfFiles.size(),(System.currentTimeMillis()-start)/1000));
    }
    /**
     * This method imports the data from an input stream. The name is used to
     * guess the RDF format used. The stream may be come directly form a file,
     * an archive, an URL or an entry in an ZIP file
     * @param is
     * @param name
     */
    private void importRdfData(InputStream is, String name) {
        if (name.endsWith(".gz")) {
            try {
                is = new GZIPInputStream(is);
            } catch (IOException e) {
                //during init it is checked that files exists and are files and there is read access
                //so this can only happen if someone deletes the file inbetween
                throw new IllegalStateException(e);
            }
            name = name.replaceFirst("\\.gz$", "");
            log.info("   - from GZIP Archive");
        } else if (name.endsWith(".bz2")) {
            try {
                is = new BZip2CompressorInputStream(is);
            } catch (IOException e) {
                //during init it is checked that files exists and are files and there is read access
                //so this can only happen if someone deletes the file inbetween
                throw new IllegalStateException(e);
            }
            name = name.replaceFirst("\\.bz2$", "");
            log.info("   - from BZip2 Archive");
        }//TODO: No Zip Files inside Zip Files supported :o( ^^
        Lang format = Lang.guess(name);
//        if (name.endsWith(".nt")) {
//            format = Lang.NTRIPLES;
//        } else if (name.endsWith(".n3")) {
//            format = Lang.N3;
//        } else {// XML is the default format
//            format = Lang.RDFXML;
//        }
        //For N-Triple we can use the TDBLoader
        if(format == Lang.NTRIPLES){
            TDBLoader.load(indexingDataset, is,true);
        } else if(format != Lang.RDFXML){
            //use RIOT to parse the format but with a special configuration
            //RiotReader!
            TDBLoader loader = new TDBLoader() ;
            loader.setShowProgress(true);
            Destination<Triple> dest = createDestination();
            dest.start() ;
            RiotReader.parseTriples(is, format, null, dest) ;
            dest.finish() ;
        } else { //RDFXML
            //in that case we need to use ARP
            Model model = ModelFactory.createModelForGraph(indexingDataset.getDefaultGraph());
            model.read(is, null);
        }
    }
    /**
     * Creates a triple destination for the default dataset of the
     * {@link #indexingDataset}.
     * This code is based on how Destinations are created in the {@link BulkLoader},
     * implementation. Note that
     * {@link BulkLoader#loadDefaultGraph(DatasetGraphTDB, InputStream, boolean)}
     * can not be used for formats other than {@link Lang#NTRIPLES} because it
     * hard codes this format for loading data form the parsed InputStream.
     * @return the destination!
     */
    private Destination<Triple> createDestination() {
            LoadMonitor monitor = new LoadMonitor(indexingDataset, log, "triples",50000,100000);
        final LoaderNodeTupleTable loaderTriples = new LoaderNodeTupleTable(indexingDataset.getTripleTable().getNodeTupleTable(), "triples", monitor) ;

        Destination<Triple> sink = new Destination<Triple>() {
            long count = 0 ;
            final public void start()
            {
                loaderTriples.loadStart() ;
                loaderTriples.loadDataStart() ;
            }
            final public void send(Triple triple)
            {
                loaderTriples.load(triple.getSubject(), triple.getPredicate(),  triple.getObject()) ;
                count++ ;
            }

            final public void flush() { }
            public void close() { }

            final public void finish()
            {
                loaderTriples.loadDataFinish() ;
                loaderTriples.loadIndexStart() ;
                loaderTriples.loadIndexFinish() ;
                loaderTriples.loadFinish() ;
            }
        } ;
        return sink ;
    }



    /**
     * The List used to cache up to {@link #indexingChunkSize} Representations
     * before they are stored in the Yard.
     */
    private List<Representation> chunkCache = new ArrayList<Representation>(this.indexingChunkSize);
    private void storeRepresentation(Representation source) throws YardException{
        if(source != null){
            chunkCache.add(
                mapper ==null?source: //if no mappings -> store the source
                    //else process the field mappings
                    mapper.applyMappings(source,vf.createRepresentation(source.getId())));
        }
        if(chunkCache.size()>=indexingChunkSize){
            yard.store(chunkCache);
            chunkCache.clear();
        }
    }

    /**
     * As the last step we need to create the baseMappings configuration
     * needed to used the Index as Entityhub full cache!
     * @throws YardException would be really bad if after successfully indexing
     * about 8 millions of documents we get an error from the yard at the
     * last possible opportunity :(
     */
    private void writeCacheBaseConfiguration() throws YardException {
        log.info("Write BaseMappings for geonames.org Cache");
        if(mapper != null){
            CacheUtils.storeBaseMappingsConfiguration(yard, mapper);
        }
        log.info(" < completed");
    }
//------------------------------------------------------------------------------
// Other implemented variants with less performance than indexResource3!
//------------------------------------------------------------------------------
//    private void indexResource2(Resource resource){
//        Query q = QueryFactory.create(String.format(resourceQuery,resource.getURI(),resource.getURI()), Syntax.syntaxARQ);
//        final ResultSet resultSet = QueryExecutionFactory.create(q, indexingDataset.toDataset()).execSelect();
//        Representation source = vf.createRepresentation(resource.getURI());
//        while(resultSet.hasNext()){
//            QuerySolution solution =resultSet.next();
//            RDFNode fieldNode = solution.get("field");
//            if(fieldNode.isURIResource()){
//                String field = fieldNode.asResource().getURI();
//                RDFNode value = solution.get("value");
//                if(value.isURIResource()){
//                    source.addReference(field, value.asResource().getURI());
//                } else if(value.isLiteral()){
//                    Literal literal = value.asLiteral();
//                    if(literal.getDatatype() != null){
//                        Object literalValue;
//                        try {
//                            literalValue = literal.getValue();
//                        } catch (DatatypeFormatException e) {
//                            log.warn(" Unable to convert "+literal.getLexicalForm()+" to "+literal.getDatatype()+"-> use lecicalForm");
//                            literalValue = literal.getLexicalForm();
//                        }
//                        if(literalValue instanceof BaseDatatype.TypedValue){
//                            source.add(field, literal.getLexicalForm());
//                        } else {
//                            source.add(field, literal.getValue());
//                        }
//                    } else {
//                        String lang = literal.getLanguage();
//                        if(lang != null && lang.isEmpty()){
//                            lang = null;
//                        }
//                        source.addNaturalText(field, literal.getLexicalForm(),lang);
//                    }
//                }
//            }
//        }
//        //log.info("S<source Resource:\n"+ModelUtils.getRepresentationInfo(source));
//    }
    private void indexRanked() throws YardException {
        if(entityRankings == null){
            throw new IllegalStateException("Unable to index with Etity Ranking Mode if no Entity Rankings are present!");
        }
        long count = 0;
        long alreadyIndexed = 0;
        long stdCount = 0;
        long notFound = 0;
        long start = System.currentTimeMillis();
        long startCurrent = System.currentTimeMillis();
        for(Entry<String,Float> entry : entityRankings.entrySet()){
            if(entry.getValue() < minimumRequiredEntityRanking){
                continue; //ignore entities with rank < the min required one
            }
            count++;
            if(count%1000 == 0){
                long thisOne = System.currentTimeMillis()-startCurrent;
                long all = System.currentTimeMillis()-start;
                log.info(String.format("processed %s resources %s indexed in %sms (%sms/item | avg: %sms/item) %s std/resourc | %s not found",
                        count, count-alreadyIndexed, thisOne,(float)thisOne/1000,(float)all/count,(float)stdCount/(count-alreadyIndexed),notFound));
                startCurrent = System.currentTimeMillis();
            }
            if(resumeMode && yard.isRepresentation(entry.getKey())){
                alreadyIndexed++;
                continue;
            }
            Representation source = vf.createRepresentation(entry.getKey());
            Node resource = Node.createURI(entry.getKey());
            ExtendedIterator<Triple> outgoing = indexingDataset.getDefaultGraph().find(resource, null, null);
            boolean found = outgoing.hasNext();
            while(outgoing.hasNext()){ //iterate over the statements for that resource
                stdCount++;
                Triple statement = outgoing.next();
                Node predicate = statement.getPredicate();
                if(predicate == null || !predicate.isURI()){
                    log.warn(String.format("Ignore field %s for resource %s because it is null or not an URI!",
                        predicate,resource));
                } else {
                    String field = statement.getPredicate().getURI();
                    Node object = statement.getObject();
                    if(object == null){
                        log.warn(String.format("Encountered NULL value for field %s and resource %s",
                                predicate,resource));
                    }else if(object.isURI()){ //add a reference
                        source.addReference(field, object.getURI());
                    } else if(object.isLiteral()){ //add a value or a text depending on the dataType
                        LiteralLabel ll = object.getLiteral();
                        //if the dataType == null , than we can expect a plain literal
                        RDFDatatype dataType = ll.getDatatype();
                        if(dataType != null){ //add a value
                            Object literalValue;
                            try {
                                literalValue = ll.getValue();
                                if(literalValue instanceof BaseDatatype.TypedValue){
                                    //used for unknown data types
                                    // -> in such cases yust use the lecial type
                                    source.add(field, ((BaseDatatype.TypedValue)literalValue).lexicalValue);
                                } else if(literalValue instanceof XSDDateTime) {
                                    source.add(field, ((XSDDateTime)literalValue).asCalendar().getTime()); //Entityhub uses the time
                                } else if(literalValue instanceof XSDDuration) {
                                    source.add(field, literalValue.toString());
                                } else {
                                    source.add(field, literalValue);
                                }
                            } catch (DatatypeFormatException e) {
                                log.warn(" Unable to convert "+ll.getLexicalForm()+" to "+ll.getDatatype()+"-> use lecicalForm");
                                literalValue = ll.getLexicalForm();
                            }
                        } else { //add a text
                            String language = ll.language();
                            if(language!=null && language.length()<1){
                                language = null;
                            }
                            source.addNaturalText(field, ll.getLexicalForm(), language);
                        }
                        // "" is parsed if there is no language
                    } else {
                        if(object.isBlank()){
                            log.info(String.format("ignoreing blank node value %s for field %s and Resource %s!",
                                    object,field,resource));
                        } else {
                            log.warn(String.format("ignoreing value %s for field %s and Resource %s because it is of an unsupported type!",
                                    object,field,resource));
                        }
                    } //end different value node type
                } //end else predicate != null
            } //end iteration over resource triple
            if(found) {
                storeRepresentation(source);
                //log.info("Resource: \n"+ModelUtils.getRepresentationInfo(source));
            } else {
                //log.info("No Statements found for "+entry.getKey()+" (ranking="+entry.getValue()+")!");
                notFound++;
            }
        }
    }
}
