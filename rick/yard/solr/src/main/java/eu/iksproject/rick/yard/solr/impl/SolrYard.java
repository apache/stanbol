package eu.iksproject.rick.yard.solr.impl;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Dictionary;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.ConfigurationPolicy;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.PropertyOption;
import org.apache.felix.scr.annotations.Service;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServer;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.CommonsHttpSolrServer;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.client.solrj.response.SolrPingResponse;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrInputDocument;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.org.apache.bcel.internal.classfile.Code;

import eu.iksproject.rick.core.model.InMemoryValueFactory;
import eu.iksproject.rick.core.query.DefaultQueryFactory;
import eu.iksproject.rick.core.query.QueryResultListImpl;
import eu.iksproject.rick.core.utils.AdaptingIterator;
import eu.iksproject.rick.core.yard.AbstractYard;
import eu.iksproject.rick.servicesapi.model.Representation;
import eu.iksproject.rick.servicesapi.model.rdf.RdfResourceEnum;
import eu.iksproject.rick.servicesapi.query.Constraint;
import eu.iksproject.rick.servicesapi.query.FieldQuery;
import eu.iksproject.rick.servicesapi.query.QueryResultList;
import eu.iksproject.rick.servicesapi.yard.Yard;
import eu.iksproject.rick.servicesapi.yard.YardException;
import eu.iksproject.rick.yard.solr.defaults.IndexDataTypeEnum;
import eu.iksproject.rick.yard.solr.impl.SolrQueryFactory.SELECT;
import eu.iksproject.rick.yard.solr.model.FieldMapper;
import eu.iksproject.rick.yard.solr.model.IndexField;
import eu.iksproject.rick.yard.solr.model.IndexValue;
import eu.iksproject.rick.yard.solr.model.IndexValueFactory;
import eu.iksproject.rick.yard.solr.query.IndexConstraintTypeEnum;
import eu.iksproject.rick.yard.solr.utils.SolrUtil;

/**
 * Implementation of the {@link Yard} interface based on a Solr Server.<p>
 * This Yard implementation supports to store data of multiple yard instances
 * within the same Solr index. The {@link FieldMapper#getDocumentDomainField()}
 * with the value of the Yard ID ({@link #getId()}) is used to mark documents
 * stored by the different Yards using the same Index. Queries are also restricted
 * to documents stored by the actual Yard by adding a
 * <a href="http://wiki.apache.org/solr/CommonQueryParameters#fq">FilterQuery</a>
 * <code>fq=fieldMapper.getDocumentDomainField()+":"+getId()</code> to all
 * queries. This feature can be activated by setting the
 * {@link #MULTI_YARD_INDEX_LAYOUT} in the configuration. However this requires,
 * that the documents in the index are already marked with the ID of the Yard.
 * So setting this property makes usually only sense when the Solr index do not
 * contain any data.<p>
 * Also note, that the different Yards using the same index MUST NOT store
 * Representations with the same ID. If that happens, that the Yard writing the
 * Representation last will win and the Representation will be deleted for the
 * other Yard!<p>
 * The SolrJ library is used for the communication with the SolrServer.<p>
 * TODO: There is still some refactoring needed, because a lot of the code
 *       within this bundle is more generic and usable regardless what kind of
 *       "document based" store is used. Currently the Solr specific stuff is in
 *       the impl and the default packages. All the other classes are intended
 *       to be generally useful. However there might be still some unwanted
 *       dependencies.<p>
 * TODO: It would be possible to support for multi cores (see
 *       http://wiki.apache.org/solr/CoreAdmin for more Information)<br>
 *       However it is not possible to create cores on the fly (at least not directly;
 *       one would need to create first the needed directories and than call
 *       CREATE via the CoreAdmin). As soon as Solr is directly started via
 *       OSGI and we do know the Solr home, than it would be possible to
 *       implement "on the fly" generation of new cores. this would also allow
 *       a configuration where - as default - a new core is created automatically
 *       on the integrated Solr Server for any configured SolrYard.
 *
 * @author Rupert Westenthaler
 *
 */
@Component(
        metatype=true,
        configurationFactory=true,
        policy=ConfigurationPolicy.REQUIRE, //the ID and SOLR_SERVER_URI are required!
        specVersion="1.1")
@Service
@Properties(value={
        //TODO: Added propertied from AbstractYard to fix ordering!
        @Property(name=Yard.ID,value="rickYard"),
        @Property(name=Yard.NAME,value="Rick Yard"),
        @Property(name=Yard.DESCRIPTION,value="Default values for configuring the RickYard without editing"),
        @Property(name=AbstractYard.DEFAULT_QUERY_RESULT_NUMBER,intValue=-1),
        @Property(name=AbstractYard.MAX_QUERY_RESULT_NUMBER,intValue=-1),
        //BEGIN SolrYard specific Properties
        @Property(name=SolrYard.SOLR_SERVER_URI,value="http://localhost:8181/solr/rick"),
        @Property(name=SolrYard.MULTI_YARD_INDEX_LAYOUT,options={
            @PropertyOption(name="true",value="true"),
            @PropertyOption(name="false",value="false")},value="false"),
        @Property(name=SolrYard.MAX_BOOLEAN_CLAUSES,intValue=SolrYard.defaultMaxBooleanClauses)
})
public class SolrYard extends AbstractYard implements Yard {
    /**
     * The key used to configure the URL for the SolrServer
     */
    public static final String SOLR_SERVER_URI = "eu.iksproject.rick.yard.solr.solrUri";
    /**
     * The key used to configure if data of multiple Yards are stored within the
     * same index (<code>default=false</code>)
     */
    public static final String MULTI_YARD_INDEX_LAYOUT = "eu.iksproject.rick.yard.solr.multiYardIndexLayout";
    /**
     * The maximum boolean clauses as configured in the solrconfig.xml of the
     * SolrServer. The default value for this config in Solr 1.4 is 1024.<p>
     * This value is important for generating queries that search for multiple
     * documents, because it determines the maximum number of OR combination for
     * the searched document ids.
     */
    public static final String MAX_BOOLEAN_CLAUSES = "eu.iksproject.rick.yard.solr.maxBooleanClauses";
    /**
     * This property allows to define a field that is used to parse the boost
     * for the parsed representation. Typically this will be the pageRank of
     * that entity within the referenced site (e.g. {@link Math#log1p(double)}
     * of the number of incoming links
     */
    public static final String DOCUMENT_BOOST_FIELD = "eu.iksproject.rick.yard.solr.documentBoost";
    /**
     * Key used to configure {@link Entry Entry&lt;String,Float&gt;} for fields
     * with the boost. If no Map is configured or a field is not present in the
     * Map, than 1.0f is used as Boost. If a Document boost is present than the
     * boost of a Field is documentBoost*fieldBoost.
     */
    public static final String FIELD_BOOST_MAPPINGS = "eu.iksproject.rick.yard.solr.fieldBoosts";
    /**
     * The default value for the maxBooleanClauses of SolrQueries. Set to
     * {@value #defaultMaxBooleanClauses} the default of Slor 1.4
     */
    protected static final int defaultMaxBooleanClauses = 1024;
    /**
     * What a surprise it's the logger!
     */
    private Logger log = LoggerFactory.getLogger(SolrYard.class);
    /**
     * The SolrServer used for this Yard. Initialisation is done based on the
     * configured parameters in {@link #activate(ComponentContext)}.
     */
    private SolrServer server;
    /**
     * The {@link FieldMapper} is responsible for converting fields of
     * {@link Representation} to fields in the {@link SolrInputDocument} and
     * vice versa
     */
    protected FieldMapper fieldMapper;
    /**
     * The {@link IndexValueFactory} is responsible for converting values of
     * fields in the {@link Representation} to the according {@link IndexValue}.
     * One should note, that some properties of the {@link IndexValue} such as
     * the language ({@link IndexValue#getLanguage()}) and the dataType
     * ({@link IndexValue#getType()}) are encoded within the field name inside
     * the {@link SolrInputDocument} and {@link SolrDocument}. This is done by
     * the configured {@link FieldMapper}.
     */
    protected IndexValueFactory indexValueFactory;
    /**
     * The {@link SolrQueryFactory} is responsible for converting the
     * {@link Constraint}s of a query to constraints in the index. This requires
     * usually that a single {@link Constraint} is described by several
     * constraints in the index (see {@link IndexConstraintTypeEnum}).<p>
     * TODO: The encoding of such constraints is already designed correctly, the
     * {@link SolrQueryFactory} that implements logic of converting the
     * Incoming {@link Constraint}s and generating the {@link SolrQuery} needs
     * to undergo some refactoring!
     *
     */
    private SolrQueryFactory solrQueryFactoy;
    /**
     * Used to store the name of the field used to get the
     * {@link SolrInputDocument#setDocumentBoost(float)} for a Representation.
     * This name is available via {@link SolrYardConfig#getDocumentBoostFieldName()}
     * however it is stored here to prevent lookups for field of every
     * stored {@link Representation}.
     */
    private String documentBoostFieldName;
    /**
     * Map used to store boost values for fields. The default Boost for fields
     * is 1.0f. This is used if this map is <code>null</code>, a field is not
     * a key in this map, the value of a field in that map is <code>null</code> or
     * lower equals zero. Also NOTE that the boost for fields is multiplied with
     * the boost for the Document if present.
     */
    private Map<String,Float> fieldBoostMap;
    /**
     * Default constructor as used by the OSGI environment.<p> DO NOT USE to
     * manually create instances! The SolrYard instances do need to be configured.
     * YOU NEED TO USE {@link #SolrYard(SolrYardConfig)} to parse the configuration
     * and the initialise the Yard if running outside a OSGI environment.
     */
    public SolrYard() { super(); }
    /**
     * Constructor to be used outside of an OSGI environment
     * @param config the configuration for the SolrYard
     * @throws IllegalArgumentException if the configuration is not valid
     * @throws YardException on any Error while initialising the Solr Server for
     * this Yard
     */
    public SolrYard(SolrYardConfig config) throws IllegalArgumentException, YardException {
        //we need to change the exceptions, because this will be called outside
        //of an OSGI environment!
        try {
            activate(config);
        } catch (IOException e) {
            new YardException("Unable to access SolrServer" +config.getSolrServerUrl());
        } catch (SolrServerException e) {
            new YardException("Unable to initialize SolrServer" +config.getSolrServerUrl());
        } catch (ConfigurationException e) {
            new IllegalArgumentException("Unable to initialise SolrYard with the provided configuration",e);
        }
    }
    @SuppressWarnings("unchecked")
    @Activate
    protected void activate(ComponentContext context) throws ConfigurationException,IOException,SolrServerException {
        log.info("in "+SolrYard.class+" activate with context "+context);
        if(context == null){
            throw new IllegalStateException("No valid"+ComponentContext.class+" parsed in activate!");
        }
        activate(new SolrYardConfig((Dictionary<String, Object>)context.getProperties()));
    }
    /**
     * Internally used to configure an instance (within and without an OSGI
     * container
     * @param config The configuration
     * @throws ConfigurationException
     * @throws IOException
     * @throws SolrServerException
     */
    private void activate(SolrYardConfig config) throws ConfigurationException,IOException,SolrServerException {
        //init with the default implementations of the ValueFactory and the QueryFactory
        super.activate(InMemoryValueFactory.getInstance(), DefaultQueryFactory.getInstance(), config);
        server = new CommonsHttpSolrServer(((SolrYardConfig)this.config).getSolrServerUrl());
        //test the server
        SolrPingResponse pingResponse = server.ping();
        log.info(String.format("Successful ping for SolrServer %s ( %d ms) Details: %s",config.getSolrServerUrl(),pingResponse.getElapsedTime(),pingResponse));
        //the fieldMapper need the Server to store it's namespace prefix configuration
        this.fieldMapper = new SolrFieldMapper(server);
        this.indexValueFactory = IndexValueFactory.getInstance();
        this.solrQueryFactoy = new SolrQueryFactory(valueFactory, indexValueFactory, fieldMapper);
        if(((SolrYardConfig)this.config).isMultiYardIndexLayout()){ // set the yardID as domain if multiYardLayout is activated
            solrQueryFactoy.setDomain(config.getId());
        }
        solrQueryFactoy.setDefaultQueryResults(this.config.getDefaultQueryResultNumber());
        solrQueryFactoy.setMaxQueryResults(this.config.getMaxQueryResultNumber());
        this.documentBoostFieldName = config.getDocumentBoostFieldName();
        this.fieldBoostMap = config.getFieldBoosts();
    }
    @Deactivate
    protected void deactivate(ComponentContext context) {
        log.info("in "+SolrYard.class+" deactivate with context "+context);
        try {
            this.server.commit();
        } catch (SolrServerException e) {
            log.error(String.format("Unable to commit unsaved changes to SolrServer %s during deactivate!",((SolrYardConfig)this.config).getSolrServerUrl()),e);
        } catch (IOException e) {
            log.error(String.format("Unable to commit unsaved changes to SolrServer %s during deactivate!",((SolrYardConfig)this.config).getSolrServerUrl()),e);
        }
        this.server = null;
        this.fieldMapper = null;
        this.indexValueFactory = null;
        this.solrQueryFactoy = null;
        this.documentBoostFieldName  = null;
        this.fieldBoostMap = null;
        super.deactivate(); //deactivate the super implementation
    }


    @Override
    public QueryResultList<Representation> find(final FieldQuery parsedQuery) throws YardException{
        return find(parsedQuery,SELECT.QUERY);
    }
    private QueryResultList<Representation> find(final FieldQuery parsedQuery,SELECT select) throws YardException {
        log.debug(String.format("find %s",parsedQuery));
        long start = System.currentTimeMillis();
        SolrQuery query = solrQueryFactoy.parseFieldQuery(parsedQuery,select);
        long queryGeneration = System.currentTimeMillis();
        final Set<String> selected;
        if(select == SELECT.QUERY){
			//if query set the fields to add to the result Representations
			selected = new HashSet<String>(parsedQuery.getSelectedFields());
			//add the score to query results!
			selected.add(RdfResourceEnum.resultScore.getUri());
        } else {
            //otherwise add all fields
            selected = null;
        }
        QueryResponse respone;
        try {
            respone = server.query(query);
        } catch (SolrServerException e) {
            throw new YardException("Error while performing Query on SolrServer!",e);
        }
        long queryTime = System.currentTimeMillis();
        //return a queryResultList
        QueryResultListImpl<Representation> resultList = new QueryResultListImpl<Representation>(parsedQuery,
                // by adapting SolrDocuments to Representations
                new AdaptingIterator<SolrDocument, Representation>(respone.getResults().iterator(),
                        //inline Adapter Implementation
                        new AdaptingIterator.Adapter<SolrDocument, Representation>() {
                            @Override
                            public Representation adapt(SolrDocument doc, Class<Representation> type) {
                                //use this method for the conversion!
                                return createRepresentation(doc, selected);
                            }
                },Representation.class), Representation.class);
        long resultProcessing = System.currentTimeMillis();
        log.debug(String.format("  ... done [queryGeneration=%dms|queryTime=%dms|resultProcessing=%dms|sum=%dms]",
                (queryGeneration-start),(queryTime-queryGeneration),(resultProcessing-queryTime),(resultProcessing-start)));
        return resultList;
    }

    @Override
    public QueryResultList<String> findReferences(FieldQuery parsedQuery) throws YardException {
        SolrQuery query = solrQueryFactoy.parseFieldQuery(parsedQuery,SELECT.ID);
        QueryResponse respone;
        try {
            respone = server.query(query);
        } catch (SolrServerException e) {
            throw new YardException("Error while performing query on the SolrServer",e);
        }
        //return a queryResultList
        return new QueryResultListImpl<String>(parsedQuery,
                // by adapting SolrDocuments to Representations
                new AdaptingIterator<SolrDocument, String>(respone.getResults().iterator(),
                        //inline Adapter Implementation
                        new AdaptingIterator.Adapter<SolrDocument, String>() {
                            @Override
                            public String adapt(SolrDocument doc, Class<String> type) {
                                //use this method for the conversion!
                                return doc.getFirstValue(fieldMapper.getDocumentIdField()).toString();
                            }
                },String.class), String.class);
    }

    @Override
    public QueryResultList<Representation> findRepresentation(FieldQuery parsedQuery) throws YardException {
        return find(parsedQuery,SELECT.ALL);
    }

    @Override
    public Representation getRepresentation(String id) throws YardException {
        SolrDocument doc;
        long start = System.currentTimeMillis();
        try {
            doc = getSolrDocument(id);
        } catch (SolrServerException e) {
            throw new YardException("Error while getting SolrDocument for id"+id,e);
        } catch (IOException e) {
            throw new YardException("Unable to access SolrServer",e);
        }
        long retrieve = System.currentTimeMillis();
        Representation rep;
        if(doc != null){
            //create an Representation for the Doc! retrieve
            log.debug(String.format("Create Representation %s from SolrDocument",doc.getFirstValue(fieldMapper.getDocumentIdField())));
            rep =  createRepresentation(doc,null);
        } else {
            rep = null;
        }
        long create = System.currentTimeMillis();
        log.debug(String.format("  ... %s [retrieve=%dms|create=%dms|sum=%dms]",
                rep==null?"not found":"done",(retrieve-start),(create-retrieve),(create-start)));
        return rep;
    }
    /**
     * Creates the Representation for the parsed SolrDocument!
     * @param doc The Solr Document to convert
     * @param fields if NOT NULL only this fields are added to the Representation
     * @return the Representation
     */
    protected Representation createRepresentation(SolrDocument doc, Set<String> fields) {
        Object id = doc.getFirstValue(fieldMapper.getDocumentIdField());
        if(id == null){
            throw new IllegalStateException(
                    String.format("The parsed Solr Document does not contain a value for the %s Field!",
                            fieldMapper.getDocumentIdField()));
        }
        Representation rep = valueFactory.createRepresentation(id.toString());
        for(String fieldName : doc.getFieldNames()){
//            log.debug(String.format(" > process SolrDocument.field: %s",fieldName));
            IndexField indexField = fieldMapper.getField(fieldName);
            if(indexField != null && indexField.getPath().size() == 1){
                String lang = indexField.getLanguages().isEmpty()?null:indexField.getLanguages().iterator().next();
                if(fields == null || fields.contains(indexField.getPath().get(0))){
//                    log.debug(String.format("   -> process IndexField %s ...",indexField));
                    for(Object value : doc.getFieldValues(fieldName)){
//                        log.debug(String.format("   -> index value %s (type=%s)",value,value!=null?value.getClass():"---"));
                        if(value != null){
                            IndexDataTypeEnum dataTypeEnumEntry = IndexDataTypeEnum.forIndexType(indexField.getDataType());
                            if(dataTypeEnumEntry != null){
                                Object javaValue = indexValueFactory.createValue(dataTypeEnumEntry.getJavaType(), indexField.getDataType(),value,lang);
                                if(javaValue != null){
//                                    log.debug(String.format("   <- java value %s (type=%s)",javaValue,javaValue.getClass()));
                                    rep.add(indexField.getPath().iterator().next(), javaValue);
                                } else {
                                    log.warn(String.format("java value=null for index value %s",value));
                                }
                            } else {
                                log.warn(String.format("No DataType Configuration found for Index Data Type %s!",indexField.getDataType()));
                            }
                        } //else index value == null -> ignore
                    } //end for all values
//                } else {
//                    log.debug(String.format("   - IndexField %s filtered, because Path is not selected",indexField));
                }
            } else {
                if(indexField != null){
                    log.warn(String.format("Unable to prozess Index Field %s (for IndexDocument Field: %s)",indexField,fieldName));
//                } else {
//                    log.debug(String.format("IndexDocument Field %s does not represent a IndexField", fieldName));
                }
            }
        } //end for all fields
        return rep;
    }


    @Override
    public boolean isRepresentation(String id) throws YardException {
        try {
            return getSolrDocument(id,Arrays.asList(fieldMapper.getDocumentIdField()))!=null;
        } catch (SolrServerException e) {
            throw new YardException("Error while performing getDocumentByID request for id "+id,e);
        } catch (IOException e) {
            throw new YardException("Unable to access SolrServer",e);
        }
    }
    /**
     * Checks what of the documents referenced by the parsed IDs are present
     * in the Solr Server
     * @param ids the ids of the documents to check
     * @return the ids of the found documents
     * @throws SolrServerException on any exception of the SolrServer
     * @throws IOException an any IO exception while accessing the SolrServer
     */
    protected Set<String> checkRepresentations(Set<String> ids) throws SolrServerException, IOException{
        Set<String> found = new HashSet<String>();
        String field = fieldMapper.getDocumentIdField();
        for(SolrDocument foundDoc : getSolrDocuments(ids,Arrays.asList(field))){
            Object value = foundDoc.getFirstValue(field);
            if(value != null){
                found.add(value.toString());
            }
        }
        return found;
    }

    @Override
    public void remove(String id) throws YardException, IllegalArgumentException {
        try {
            server.deleteById(id);
            server.commit();
        } catch (SolrServerException e) {
            throw new YardException("Error while deleting document "+id+" from the Solr server",e);
        } catch (IOException e) {
            throw new YardException("Unable to access SolrServer",e);
        }
        //TODO: maybe we need also to update all Documents that refer this ID
    }
    @Override
    public void remove(Iterable<String> ids) throws IllegalArgumentException, YardException {
        if(ids == null){
            throw new IllegalArgumentException("The parsed IDs MUST NOT be NULL");
        }
        List<String> remove = new ArrayList<String>();
        for(String id :ids){
            if(id != null){
                remove.add(id);
            }
        }
        try {
            server.deleteById(remove);
        } catch (SolrServerException e) {
            throw new YardException("Error while deleting documents from the Solr server",e);
        } catch (IOException e) {
            throw new YardException("Unable to access SolrServer",e);
        }
        //TODO: maybe we need also to update all Documents that refer this ID

    }
    @Override
    public Representation store(Representation representation) throws YardException,IllegalArgumentException {
        log.debug(String.format("Store %s",representation!= null?representation.getId():null));
        if(representation == null){
            return null;
        }
        long start = System.currentTimeMillis();
        SolrInputDocument inputDocument = createSolrInputDocument(representation);
        long create = System.currentTimeMillis();
        try {
            server.add(inputDocument);
            server.commit();
            long stored = System.currentTimeMillis();
            log.debug(String.format("  ... done [create=%dms|store=%dms|sum=%dms]",
                    (create-start),(stored-create),(stored-start)));
        } catch (SolrServerException e) {
            throw new YardException(String.format("Exception while adding Document to Solr",representation.getId()),e);
        } catch (IOException e) {
            throw new YardException("Unable to access SolrServer",e);
        }
        return representation;
    }
    @Override
    public Iterable<Representation> store(Iterable<Representation> representations) throws IllegalArgumentException, YardException {
        if(representations == null){
            throw new IllegalArgumentException("The parsed Representations MUST NOT be NULL!");
        }
        long start = System.currentTimeMillis();
        List<SolrInputDocument> inputDocs = new ArrayList<SolrInputDocument>();
        for(Representation representation : representations){
            if(representation != null){
                inputDocs.add(createSolrInputDocument(representation));
            }
        }
        long created = System.currentTimeMillis();
        try {
            server.add(inputDocs);
            server.commit();
        } catch (SolrServerException e) {
            throw new YardException("Exception while adding Documents to the Solr Server!",e);
        } catch (IOException e) {
            throw new YardException("Unable to access Solr server",e);
        }
        long ready = System.currentTimeMillis();
        log.debug(String.format("Processed store request for %d documents in %dms (created %dms| stored%dms)",
                inputDocs.size(),ready-start,created-start,ready-created));
        return representations;
        }
    /**
     * boost if present!
     * @param representation
     * @return
     */
    protected SolrInputDocument createSolrInputDocument(Representation representation) {
        SolrInputDocument inputDocument = new SolrInputDocument();
        // If multiYardLayout is active, than we need to add the YardId as
        // domain for all added documents!
        if(((SolrYardConfig)this.config).isMultiYardIndexLayout()){
            inputDocument.addField(fieldMapper.getDocumentDomainField(), config.getId());
        } // else we need to do nothing
        inputDocument.addField(fieldMapper.getDocumentIdField(), representation.getId());
        //first process the document boost
        float documentBoost = documentBoostFieldName == null ? 1.0f : getDocumentBoost(representation);
        for(Iterator<String> fields = representation.getFieldNames();fields.hasNext();){
            //TODO: maybe add some functionality to prevent indexing of the
            //      field configured as documentBoostFieldName!
            String field = fields.next();
            Float fieldBoost = fieldBoostMap == null ? null : fieldBoostMap.get(field);
            float boost = fieldBoost == null ? documentBoost : fieldBoost >= 0 ? fieldBoost * documentBoost: documentBoost;
//            log.debug(String.format(" > Process Representation Field %s",field));
            for(Iterator<Object> values = representation.get(field);values.hasNext();){
                //now we need to get the indexField for the value
                Object next = values.next();
                IndexValue value;
                try {
                    value = indexValueFactory.createIndexValue(next);
                    for(String fieldName : fieldMapper.getFieldNames(Arrays.asList(field), value)){
//                        log.debug(String.format("  - add: %s=%s",fieldName,value));
                        inputDocument.addField(fieldName, value.getValue(),boost);
                    }
                }catch(Exception e){
                    log.warn(String.format("Unable to process value %s (type:%s) for field %s!",next,next.getClass(),field),e);
                }
            }
        }
        return inputDocument;
    }
    /**
     * Extracts the document boost from a {@link Representation}.
     * @param representation the representation
     * @return the Boost or <code>null</code> if not found or lower equals zero
     */
    private float getDocumentBoost(Representation representation) {
        if(documentBoostFieldName == null){
            return 1.0f;
        }
        Float documentBoost = null;
        for(Iterator<Object> values =representation.get(documentBoostFieldName);values.hasNext() && documentBoost == null;){
            Object value = values.next();
            if(value instanceof Float){
                documentBoost = (Float) value;
            } else {
                try {
                    documentBoost = Float.parseFloat(value.toString());
                } catch (NumberFormatException e) {
                    log.warn(String.format("Unable to parse the Document Boost from field %s=%s[type=%s] -> The Document Boost MUST BE a Float value!",documentBoostFieldName,value,value.getClass()));
                }
            }
        }
        return documentBoost == null? 1.0f : documentBoost >= 0 ? documentBoost : 1.0f;
    }

    @Override
    public Representation update(Representation representation) throws YardException {
        if(representation == null){
            return null;
        }
        boolean found  = isRepresentation(representation.getId());
        if(found) {
            return store(representation); //there is no "update" for solr
        } else {
            throw new YardException("Parsed Representation "+representation.getId()+" in not managed by this Yard "+getName()+"(id="+getId()+")");
        }
    }
    @Override
    public Iterable<Representation> update(Iterable<Representation> representations) throws YardException, IllegalArgumentException {
        long start = System.currentTimeMillis();
        Set<String> ids = new HashSet<String>();

        for(Representation representation : representations){
            if(representation != null){
                ids.add(representation.getId());
            }
        }
        int numDocs = ids.size(); //for debuging
        try {
            ids = checkRepresentations(ids); //returns the ids found in the solrIndex
        } catch (SolrServerException e) {
            throw new YardException("Error while searching for alredy present documents before executing the actual update for the parsed Representations",e);
        } catch (IOException e) {
            throw new YardException("Unable to access SolrServer",e);
        }
        long checked = System.currentTimeMillis();
        List<SolrInputDocument> inputDocs = new ArrayList<SolrInputDocument>(ids.size());
        List<Representation> updated = new ArrayList<Representation>();
        for(Representation representation : representations){
            if(representation == null || !ids.contains(representation.getId())){ //null parsed or not already present
                updated.add(null); //we need to add null values to the output
            } else { //present in the yard -> perform the update
                inputDocs.add(createSolrInputDocument(representation));
                updated.add(representation);
            }
        }
        long created = System.currentTimeMillis();
        try {
            server.add(inputDocs);
            server.commit();
        } catch (SolrServerException e) {
            throw new YardException("Error while adding updated Documents to the SolrServer",e);
        } catch (IOException e) {
            throw new YardException("Unable to access Solr server",e);
        }
        long ready = System.currentTimeMillis();
        log.debug(String.format("Processed updateRequest for %d documents (%d updated) in %dms (checked %dms|created %dms| stored%dms)",
                numDocs,ids,ready-start,checked-start,created-checked,ready-created));
        return updated;
    }
    /**
     * Stores the parsed document within the Index. This Method is also used by
     * other classes within this package to store configurations directly within
     * the index
     * @param inputDoc the document to store
     */
    protected void storeSolrDocument(SolrInputDocument inputDoc) throws SolrServerException, IOException{
        server.add(inputDoc);
    }
    /**
     * Getter for a SolrDocument based on the ID. This Method is also used by
     * other classes within this package to load configurations directly from
     * the index
     * @param inputDoc the document to store
     */
    public SolrDocument getSolrDocument(String uri) throws SolrServerException, IOException {
        return getSolrDocument(uri, null);
    }
    protected Collection<SolrDocument> getSolrDocuments(Collection<String> uris,Collection<String> fields) throws SolrServerException, IOException {
        SolrQuery solrQuery = new SolrQuery();
        if(fields == null || fields.isEmpty()){
            solrQuery.addField("*"); //select all fields
        } else {
            for(String field : fields){
                if(field !=null && !field.isEmpty()){
                    solrQuery.addField(field);
                }
            }
        }
        //NOTE: If there are more requested documents than allowed boolean
        //      clauses in one query, than we need to send several requests!
        Iterator<String> uriIterator = uris.iterator();
        int maxClauses;
        Integer configuredMaxClauses = ((SolrYardConfig)config).getMaxBooleanClauses();
        if(configuredMaxClauses != null && configuredMaxClauses > 0){
            maxClauses = configuredMaxClauses;
        } else {
            maxClauses = defaultMaxBooleanClauses;
        }
        int num = 0;
        StringBuilder queryBuilder = new StringBuilder();
        boolean myList = false;
        Collection<SolrDocument> resultDocs = null;
        //do while more uris
        while(uriIterator.hasNext()){
            //do while more uris and free boolean clauses
            //num <= maxClauses because 1-items boolean clauses in the query!
            while(uriIterator.hasNext() && num <= maxClauses){
                String uri = uriIterator.next();
                if(uri !=null){
                    if(num > 0){
                        queryBuilder.append(" OR ");
                    }
                    queryBuilder.append(String.format("%s:%s",
                            fieldMapper.getDocumentIdField(),
                            SolrUtil.escapeSolrSpecialChars(uri)));
                    num++;
                }
            }
            //no more items or all boolean clauses used -> send a request
            num = 0; //reset to 0
            solrQuery.setQuery(queryBuilder.toString());
            queryBuilder = new StringBuilder(); // and a new StringBuilder
            solrQuery.setRows(num);
            QueryResponse queryResponse = server.query(solrQuery);
            if(resultDocs == null){
                resultDocs = queryResponse.getResults();
            } else {
                if(myList == false){
                    //most of the time there will be only one request, so only
                    //create my own list when the second response is processed
                    resultDocs = new ArrayList<SolrDocument>(resultDocs);
                    myList = true;
                }
                resultDocs.addAll(queryResponse.getResults());
            }
        } //end while more uris
        return resultDocs;
    }
    protected SolrDocument getSolrDocument(String uri,Collection<String> fields) throws SolrServerException, IOException {
        SolrQuery solrQuery = new SolrQuery();
        if(fields == null || fields.isEmpty()){
            solrQuery.addField("*"); //select all fields
        } else {
            for(String field : fields){
                if(field !=null && !field.isEmpty()){
                    solrQuery.addField(field);
                }
            }
        }
        solrQuery.setRows(1); //we query for the id, there is only one result
        String queryString = String.format("%s:%s",
                fieldMapper.getDocumentIdField(),SolrUtil.escapeSolrSpecialChars(uri));
        solrQuery.setQuery(queryString);
        QueryResponse queryResponse = server.query(solrQuery);
        if(queryResponse.getResults().isEmpty()){
            return null;
        } else {
            return queryResponse.getResults().get(0);
        }
    }
}
