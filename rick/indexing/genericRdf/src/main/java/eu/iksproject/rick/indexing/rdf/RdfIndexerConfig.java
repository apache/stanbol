package eu.iksproject.rick.indexing.rdf;

import static eu.iksproject.rick.indexing.rdf.RdfIndexer.*;

import java.io.File;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Dictionary;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import eu.iksproject.rick.core.mapping.FieldMappingUtils;
import eu.iksproject.rick.servicesapi.mapping.FieldMapper;
import eu.iksproject.rick.servicesapi.mapping.FieldMapping;
import eu.iksproject.rick.servicesapi.model.Representation;
import eu.iksproject.rick.servicesapi.model.rdf.RdfResourceEnum;
import eu.iksproject.rick.servicesapi.yard.Yard;

/**
 * API based Configuration for the {@link Dictionary} based configuration of the 
 * {@link RdfIndexer} class.
 * 
 * @author Rupert Westenthaler
 * 
 */
public class RdfIndexerConfig {
    
    Logger log = LoggerFactory.getLogger(RdfIndexerConfig.class);
    Dictionary<String,Object> config = new Hashtable<String,Object>();
    
/**
     * Constructs a minimal configuration for a Yard and a list of RDF Files
     * to be indexed. Parsed files are checked for existences and read access.<p>
     * Note that:<ul>
     * <li><code>{@link Yard#getId()}+"_indexingData"</code> is used as the
     * directory for indexing (use {@link #setIndexingDir(File)} to change this
     * default).
     * <li><code>{@link Yard#getId()}+"_model"</code> is used as modelName for
     * the name of the Model (use {@link #setModelName(String)} to change this
     * default).
     * <li> if the list of the parsed files is <code>null</code> or empty, than
     * the {@link #setSkipRead(Boolean) is set to <code>true</code>. This means
     * that it is assumed that the RDF Model (name: #getModelName() and part of
     * the RDF triple store located at #getIndexingDir()) already contains all
     * the RDF data needed for the indexing. Otherwise the all parsed files are
     * checked for existence and read access and #setSkipRead(Boolean) is set to
     * <code>false</code>. This will trigger to read the RDF data of that files
     * to be stored in the RDF model with the configured name and the Triple
     * Store with the configured location.
     * </ul>
     * @param yard The {@link Yard} used to store the RDF data
     * @param rdfFiles the RDF files to index.
     */
    public RdfIndexerConfig(Yard yard, File... rdfFiles) {
        if (yard == null) {
            throw new IllegalArgumentException("The parsed Yard MUST NOT be NULL");
        }
        config.put(KEY_YARD, yard);
        setIndexingDir(new File(yard.getId() + "_indexingData"));
        setModelName(yard.getId() + "_model");
        if (rdfFiles == null || rdfFiles.length < 1) {
            log.info("no RDF Files parsed -> set skipRead to TRUE");
            setSkipRead(Boolean.TRUE);
        } else {
            addSourceFile(rdfFiles);
        }
    }
    
    /**
     * Adds the parsed source files. Checks for {@link File#exists()}, {@link File#isFile()} and
     * {@link File#canRead()}. Files that do not pase this test are ignored.
     * 
     * @param sourceFiles
     *            the source files to add
     */
    public void addSourceFile(File... sourceFiles) {
        if (sourceFiles != null) {
            for (File sourceFile : sourceFiles) {
                checkAndAddSrouceFile(sourceFile);
            }
        }
    }
    
    /**
     * Remove the parsed source files
     * 
     * @param sourceFile
     *            the source files to remove
     */
    public void removeSourceFile(File... sourceFile) {
        if (sourceFile == null) {
            return;
        }
        Set<File> files = (Set<File>) config.get(KEY_RDF_FILES);
        if (files != null) {
            files.removeAll(Arrays.asList(sourceFile));
        }
    }
    
    /**
     * Getter for the list of RDF files used as source for indexing.
     * 
     * @return the unmodifiable list of files or <code>null</code>if no source files are present.
     */
    public Collection<File> getSourceFiles() {
        Collection<File> sourceFiles = (Collection<File>) config.get(KEY_RDF_FILES);
        return sourceFiles == null ? null : Collections.unmodifiableCollection((Collection<File>) config
                .get(KEY_RDF_FILES));
    }
    
    /**
     * Setter for the directory used to store the data of the RDF triple store used for indexing.
     * 
     * @param file
     *            the directory used to store the data (created if not exist)
     * @return <code>true</code> if the parsed file can be used as indexing
     *     directory (exists and is a directory or !exists) 
     */
    public boolean setIndexingDir(File file) throws IllegalArgumentException {
        if (checkFile(file, false, null)) { // isDirectory or !exists
            config.put(KEY_RDF_STORE_DIR, file);
            return true;
        } else {
            return false;
        }
    }
    /**
     * Getter for the indexing directory
     * @return the directory used for indexing
     */
    public File getIndexingDir() {
        return (File) config.get(KEY_RDF_STORE_DIR);
    }
    /**
     * Setter for the RDF model name used to store/access the RDF data used
     * for indexing
     * @param modelName the RDF model name
     */
    public void setModelName(String modelName) {
        if (modelName != null && !modelName.isEmpty()) {
            config.put(KEY_MODEL_NAME, modelName);
        } else {
            log.warn("Unable to set modelName to NULL or an empty String");
        }
    }
    /**
     * Getter for the RDF model name used to store/access the RDF data used
     * for indexing
     * @return the RDF model name
     */
    public String getModelName() {
        return (String) config.get(KEY_MODEL_NAME);
    }
    /**
     * Setter for the (optional) map that uses entity ids as key and there 
     * ranking as value. see {@link RdfResourceEnum#signRank} for more information
     * about ranking of entities)
     * @param entityRankings the entity ranking map
     */
    public void setEntityRankings(Map<String,Float> entityRankings) {
        if (entityRankings == null) {
            config.remove(KEY_ENTITY_RANKINGS);
        } else {
            config.put(KEY_ENTITY_RANKINGS, entityRankings);
        }
    }
    /**
     * Getter for the (optional) map that uses entity ids as key and there 
     * ranking as value. see {@link RdfResourceEnum#signRank} for more information
     * about ranking of entities)
     * @return the entity rankings or <code>null</code> if no entity rankings
     * are present
     */
    public Map<String,Float> getNetityRankings() {
        return (Map<String,Float>) config.get(KEY_ENTITY_RANKINGS);
    }
    /**
     * Setter for the indexing mode (expert use only). Please carefully read
     * {@link RdfIndexer#KEY_INDEXING_MODE} before setting this property.
     * @param mode the indexing mode
     */
    public void setIndexingMode(IndexingMode mode) {
        if (mode != null) {
            config.put(KEY_INDEXING_MODE, mode);
        } else {
            config.remove(KEY_INDEXING_MODE);
        }
    }
    /**
     * Getter for the Indexing Mode
     * @return the indexing mode or <code>null</code> if not set by this
     * configuration.
     */
    public IndexingMode getIndexingMode() {
        return (IndexingMode) config.get(KEY_INDEXING_MODE);
    }
    /**
     * Setter for the skip reading mode. If set to <code>true</code> no RDF
     * data are read. This can be useful if the RDF data are
     * already available as an Jena TDB store (e.g. when interrupting an
     * indexing session that has already completed with reading the RDF data.
     * @param state the state or <code>null</code> to remove any present config
     */
    public void setSkipRead(Boolean state) {
        if (state == null) {
            config.remove(KEY_SKIP_READ);
        } else {
            config.put(KEY_SKIP_READ, state);
        }
    }
    /**
     * Getter for the skip reading state.
     * @return the state or <code>null</code> if not set
     */
    public Boolean getSkipRead() {
        return (Boolean) config.get(KEY_SKIP_READ);
    }
    /**
     * This is the number of documents stored in the {@link Yard} at once. During
     * indexing {@link Representation} are created based on the RDF data of the
     * configured RDF source files. As soon as chink size Representations are
     * created they are stored by a single call to {@link Yard#store(Iterable)}.
     * @param size the number of {@link Representation} stored at in the {@link Yard}
     * at once. Parse <code>null</code> or a value smaller equals zero to remove
     * this optional configuration.
     */
    public void setChunkSize(Integer size) {
        if (size == null || size < 1) {
            config.remove(KEY_CHUNK_SIZE);
        } else {
            config.put(KEY_CHUNK_SIZE, size);
        }
    }
    /**
     * Getter for the chunk size (number of {@link Representation} sotred at once
     * in the {@link Yard}.
     * @return the chunk size or <code>null</code> if not present
     */
    public Integer getChunkSize() {
        return (Integer) config.get(KEY_CHUNK_SIZE);
    }
    /**
     * Allows to set the state if Entities without ranking information should be
     * ignored by the indexer.
     * @param state the state or <code>null</code> to remove the configuration
     * and go with the default.
     */
    public void setIgnoreEntitiesWithoutRanking(Boolean state) {
        if (state == null) {
            config.remove(KEY_IGNORE_ENTITIES_WITHOUT_ENTITY_RANKING);
        } else {
            config.put(KEY_IGNORE_ENTITIES_WITHOUT_ENTITY_RANKING, state);
        }
    }
    /**
     * Getter for the state if entities without available ranking should be
     * ignored.
     * @return the state or <code>null</code> if not present
     */
    public Boolean getIgnoreEntitiesWithoutRanking() {
        return (Boolean) config.get(KEY_IGNORE_ENTITIES_WITHOUT_ENTITY_RANKING);
    }
    /**
     * Setter for the minimal ranking required by an entity to be processed
     * by the indexer.
     * @param minRanking the minimum ranking. Parsed <code>null</code> or a
     * value smaller equals 0 to remove this configuration and go with the
     * default.
     */
    public void setMinEntityRanking(Float minRanking) {
        if (minRanking == null || minRanking <= 0) {
            config.remove(KEY_REQUIRED_ENTITY_RANKING);
        } else {
            config.put(KEY_REQUIRED_ENTITY_RANKING, minRanking);
        }
    }
    /**
     * The minimum required ranking required by an entity to be indexed.
     * @return the minimum required ranking or <code>null</code> if not defined
     * by this configuration
     */
    public Float getMinEntityRanking() {
        return (Float) config.get(KEY_REQUIRED_ENTITY_RANKING);
    }
    /**
     * Setter for the ranking used for entities for them no ranking is
     * available. The value need to be greater than zero to be accepted.
     * @param defaultRanking the ranking used for entities without ranking
     * information. Parse <code>null</code> or a value smaller equals zero to
     * remove this configuration.
     */
    public void setDefaultEntityRanking(Float defaultRanking) {
        if (defaultRanking == null || defaultRanking <= 0) {
            config.remove(defaultRanking);
        } else {
            config.put(KEY_DEFAULT_ENTITY_RANKING, defaultRanking);
        }
    }
    /**
     * Getter for the default ranking value that is used for entities no ranking
     * information are available.
     * @return the default ranking or <code>null</code> if not present within
     * this configuration.
     */
    public Float getDefaultEntityRanking() {
        return (Float) config.get(KEY_DEFAULT_ENTITY_RANKING);
    }
    
    /**
     * Tests if a RDF source file is valid (exists, isFile and canRead) and
     * if OK add them to the configuration.
     * @param sourceFile the file to add
     */
    private void checkAndAddSrouceFile(File sourceFile) {
        if (checkFile(sourceFile, true, true)) {
            Set<File> files = (Set<File>) config.get(KEY_RDF_FILES);
            if (files == null) {
                files = new HashSet<File>();
                config.put(KEY_RDF_FILES, files);
            }
            log.info("  > add source file " + sourceFile);
            files.add(sourceFile);
        }
    }
    /**
     * Setter for the field mappings used by the indexer to create the
     * {@link Representation}s based on the RDF input.<br>
     * Parsd strings must represent valid field mappings.
     * @param mappings A collection field mappings in the string representation.
     * If <code>null</code> or an empty collection is parsed the configuration
     * is removed.
     */
    public void setMappings(Collection<String> mappings){
        if(mappings == null || mappings.isEmpty()){
            config.remove(KEY_FIELD_MAPPINGS);
        } else {
            config.put(KEY_FIELD_MAPPINGS, mappings);
        }
    }
    /**
     * Setter for the field mappings that allows to parse an existing
     * {@link FieldMapper} instance. Note that the string representation of all
     * the {@link FieldMapping}s part of the FieldMapper will be stored within
     * the configuration
     * @param mapper the FieldMapper instance with the FieldMappings to be used
     * for the configuration of the indexer.
     */
    public void setMappings(FieldMapper mapper){
        if(mapper != null){
            String[] mappingStrings = FieldMappingUtils.serialiseFieldMapper(mapper);
            setMappings(mappingStrings != null ? Arrays.asList(mappingStrings):null);
        } else {
            setMappings((Collection<String>)null);
        }
    }
    /**
     * Getter for the field mappings
     * @return the field mappings or <code>null</code> if no are defined for
     * this configuration.
     */
    public Collection<String> getMappings(){
        Collection<String> mappings = (Collection<String>)config.get(KEY_FIELD_MAPPINGS);
        return mappings == null ? null: Collections.unmodifiableCollection(mappings);
    }
    /**
     * checks a files against the parsed parameter
     * @param file the file to check
     * @param isFile if <code>true</code> than the parsed {@link File} is tested
     * to be a file, otherwise it is test to be a directory
     * @param exists if <code>null</code> it is indicated that the file/directory
     * can bee created if necessary. <code>true</code> indicated that the parsed
     * file must exist where <code>false</code> indicate that the file MUST NOT
     * exists.
     * @return <code>true</code> if the parsed {@link File} fulfils the stated
     * requirements.
     */
    private boolean checkFile(File file, boolean isFile, Boolean exists) {
        //exists null means that it will be created if not existence
        //therefore we need only to check the state if not null.
        if (exists != null) {
            if (file.exists() != exists) {
                log.warn(String.format("parsed File %s does %s exist, but the other state was requested", file, file.exists() ? "" : "not"));
                return false;
            }
        } else if (!file.exists()) {
            //in case of exists == null && file does not exist we assume that
            //the File (or Directory) will be created. Therefore no further
            //checks are required
            return true;
        } //else the parsed file exists -> perform the other checks
        if (file.isFile() != isFile) {
            log.warn(String.format("parsed File %s is a %s but %s was requested!", 
                file, file.isFile() ? "File" : "Directory",isFile ? "File" : "Directory"));
            return false;
        }
        if (!file.canRead()) {
            log.warn(String.format("Unable to read parsed File %s", file));
            return false;
        }
        return true;
    }
}
