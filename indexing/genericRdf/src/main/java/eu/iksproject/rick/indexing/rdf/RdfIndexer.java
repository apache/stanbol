package eu.iksproject.rick.indexing.rdf;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Dictionary;
import java.util.List;
import java.util.zip.GZIPInputStream;

import org.apache.clerezza.rdf.core.access.TcProvider;
import org.apache.clerezza.rdf.jena.tdb.storage.TdbTcProvider;
import org.apache.commons.compress.compressors.bzip2.BZip2CompressorInputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import eu.iksproject.rick.core.utils.ModelUtils;
import eu.iksproject.rick.servicesapi.mapping.FieldMapper;
import eu.iksproject.rick.servicesapi.model.ValueFactory;
import eu.iksproject.rick.servicesapi.yard.Yard;

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
 * <li> Support for the RICK Representation Mapping Infrastructure (currently
 *      this means using the {@link FieldMapper}
 * <li> Support for filtering Entities based on rdf:type (will be in future 
 *      version supported by the RICK Representation Mapping Infrastructure)
 * <li> any more?
 * <li>
 * <li>
 * <li>
 * </ul>
 *  
 * @author Rupert Westenthaler
 *
 */
public class RdfIndexer {
	Logger log = LoggerFactory.getLogger(RdfIndexer.class);
	/**
	 * Key used to parse the Yard used for indexing
	 */
	public static final String KEY_YARD = "eu.iksproject.rick.indexing.yard";
	/**
	 * Key used to parse reference(s) to the RDF files to be indexed!<p>
	 * This supports both single values as well as {@link Iterable}s over several
	 * values. All parsed sources are loaded within one TripleStore and are
	 * indexed at once! Use several {@link RdfIndexer} instances to index them
	 * one after the other.
	 */
	public static final String KEY_RDF_FILES = "eu.iksproject.rick.indexing.rdf.rdfFiles";
	/**
	 * Key used to configure the fieldMappings used to determine what properties
	 * are indexed for entities
	 */
	public static final String KEY_FIELD_MAPPINGS = "eu.iksproject.rick.indexing.rdf.fieldMappings";
	
	/**
	 * Key used to configure the directory to store RDF data needed during the
	 * indexing process. This data might be reused when resuming an indexing
	 * process.
	 */
	public static final String KEY_INDEXING_DIR = "eu.iksproject.rick.indexing.rdf.indexingDir";
	/**
	 * Key used to configure the name of the model used to store the parsed
	 * RDF data before the indexing process. Parsing this name can be used to
	 * resume indexing based on previously parsed RDF data.
	 */
	public static final String KEY_MODEL_NAME = "eu.iksproject.rick.indexing.rdf.modelName";
	
	private final Yard yard;
	private final ValueFactory vf;
	private final List<File> rdfFiles;
	private final File indexingDir;
	private final String modelName;
	
	public RdfIndexer(Dictionary<String, Object> config){
		this.yard = (Yard)config.get(KEY_YARD);
		if(yard == null){
			throw new IllegalArgumentException("Parsed config MUST CONTAIN a Yard. Use the key "+KEY_YARD+" to parse the YardInstance used to store the geonames.org index!");
		} else {
			log.info(String.format("Using Yard %s (id=%s) to index geonames.org",
					yard.getName(),yard.getId()));
		}
		this.vf = yard.getValueFactory();
		Object rdfFiles = config.get(KEY_RDF_FILES);
		if(rdfFiles instanceof Iterable<?>){
			this.rdfFiles = new ArrayList<File>();
			for(Object value : (Iterable[])rdfFiles){
				this.rdfFiles.add(checkFile(value.toString()));
			}
		} else {
			this.rdfFiles = Collections.singletonList(checkFile(rdfFiles.toString()));
		}
		Object indexingDir = config.get(KEY_INDEXING_DIR);
		if(indexingDir == null){
			indexingDir = "indexingData";
			config.put(KEY_INDEXING_DIR, indexingDir);
		}
		this.indexingDir = checkFile(indexingDir.toString(), false, true);
		Object modelName = config.get(KEY_MODEL_NAME);
		if(modelName == null){
			modelName = "indexingModel-"+ModelUtils.randomUUID().toString();
			config.put(KEY_MODEL_NAME, modelName);
		}
		this.modelName = modelName.toString();
	}
	private File checkFile(String value) {
		return checkFile(value,true,false);
	}
	private File checkFile(String value,boolean file, boolean create) {
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
				throw new IllegalStateException("File "+value+" does not exist!");
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
		TcProvider provider = new IndexingModelProvider(indexingDir);

        for (File modelFile : rdfFiles) {
            System.out.printf("loading '%s' into model '%s'...", modelFile,
                    modelName);
            InputStream is;
			try {
				is = new FileInputStream(modelFile);
			} catch (FileNotFoundException e) {
				//during init it is checked that files exists and are files and there is read access
				//so this can only happen if someone deletes the file inbetween
				throw new IllegalStateException(e);
			}
            String name = modelFile.getName();
            if (name.endsWith(".gz")) {
                try {
					is = new GZIPInputStream(is);
				} catch (IOException e) {
					//during init it is checked that files exists and are files and there is read access
					//so this can only happen if someone deletes the file inbetween
					throw new IllegalStateException(e);
				}
                name = name.replaceFirst("\\.gz$", "");
            } else if (name.endsWith(".bz2")) {
                try {
					is = new BZip2CompressorInputStream(is);
				} catch (IOException e) {
					//during init it is checked that files exists and are files and there is read access
					//so this can only happen if someone deletes the file inbetween
					throw new IllegalStateException(e);
				}
                name = name.replaceFirst("\\.bz2$", "");
            }

            String format = null;
            if (name.endsWith(".nt")) {
                format = "N-TRIPLE";
            } else if (name.endsWith(".n3")) {
                format = "N3";
            } // XML is the default format

//            model.read(is, null, format);
//            System.out.println(" done");
        }
		
	}
	public static void main(String[] args) {
		System.out.println(Integer.MAX_VALUE);
	}
	
	/**
	 * Simple wrapper to use {@link TdbTcProvider} without an OSGI Environment
	 * @author Rupert Westenthaler
	 *
	 */
	protected static final class IndexingModelProvider extends TdbTcProvider {
		
		protected IndexingModelProvider(File baseDir){
			super();
			activate(null);
		}
		
		public void close(){
			deactivate(null);
		}
	}
}
