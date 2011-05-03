package org.apache.stanbol.entityhub.indexing.core.source;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Arrays;
import java.util.Map;

import org.apache.stanbol.entityhub.indexing.core.EntityIterator;
import org.apache.stanbol.entityhub.indexing.core.config.IndexingConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
/**
 * Implementation of the {@link EntityIterator} based on reading data line wise
 * from an {@link InputStream}
 * @author Rupert Westenthaler
 *
 */
public class LineBasedEntityIterator implements EntityIterator {

    private static final Logger log = LoggerFactory.getLogger(LineBasedEntityIterator.class);
    /**
     * The default separator to split the entity id with the score "\t" (tab)
     */
    public static final String DEFAULT_SEPARATOR = "\t";
    /**
     * The default encoding used to read the data from the parsed {@link InputStream}
     * (UTF-8)
     */
    public static final String DEFAULT_ENCODING = "UTF-8";
    /**
     * The default if Entity ids should be {@link URLEncoder URLEncoded} (false)
     */
    public static final boolean DEFAULT_ENCODE_ENTITY_IDS = false;
    /**
     * Parameter used to configure the name of the source file within the
     * {@link IndexingConfig#getSourceFolder()}
     */
    public static final String PARAM_ENTITY_SCORE_FILE = "source";
    /**
     * The default name used for the {@link #PARAM_ENTITY_SCORE_FILE}
     */
    public static final String DEFAULT_ENTITY_SCORE_FILE = "entityScores.tsv";
    /**
     * Parameter used to configure if the Entity IDs should be {@link URLEncoder
     * URL encoded} (the default is {@value #DEFAULT_ENCODE_ENTITY_IDS})
     */
    public static final String PARAM_URL_ENCODE_ENTITY_IDS = "encodeIds";
    /**
     * Parameter used to configure the text encoding used by the source file
     * (the default is {@value #DEFAULT_ENCODING})
     */
    public static final String PARAM_CHARSET = "charset";
    
    private BufferedReader reader;
    private String separator;
    private String charset;
    private boolean encodeEntityIds;
    private long lineCounter = 0;
    private String nextLine;
    
    /**
     * Default constructor relaying on {@link #setConfiguration(Map)} is used
     * to provide the configuration
     */
    public LineBasedEntityIterator(){
        this(null);
    }
    /**
     * Constructs an EntityScoreIterator that reads {@link EntityScore}s based 
     * on lines provided by the parsed InputStream. <p> Separator, Charset and
     * encoding of Entity ids are initialised based on the default values.
     * @param is the InputStream to read the data from
     * @throws IOException On any error while initialising the {@link BufferedReader}
     * based on the parsed {@link InputStream}
     */
    public LineBasedEntityIterator(InputStream is) {
        this(is,null,null,null);
    }
    /**
     * Constructs an EntityScoreIterator based on the parsed parameters. The
     * default values are used if <code>null</code> is parsed for any parameter
     * other than the InputStream.
     * @param is the InputStream to read the data from
     * @param charset
     * @param separator
     * @param encodeIds
     * @throws IOException On any error while initialising the {@link BufferedReader}
     * based on the parsed {@link InputStream}
     * @throws IllegalArgumentException if <code>null</code> is parsed as InputStream
     */
    public LineBasedEntityIterator(InputStream is,String charset,String separator,Boolean encodeIds) throws IllegalArgumentException {
        if(charset == null){
            this.charset = DEFAULT_ENCODING;
        } else {
            this.charset = charset;
        }
        if(separator == null){
            this.separator = DEFAULT_SEPARATOR;
        } else {
            this.separator = separator;
        }
        if(encodeIds == null){
            encodeEntityIds = DEFAULT_ENCODE_ENTITY_IDS;
        } else {
            encodeEntityIds = encodeIds;
        }
        if(is != null){
            initReader(is);
        }
    }
    @Override
    public void setConfiguration(Map<String,Object> config) {
        IndexingConfig indexingConfig = (IndexingConfig)config.get(IndexingConfig.KEY_INDEXING_CONFIG);
        File score;
        Object value = config.get(PARAM_CHARSET);
        if(value != null && value.toString() != null){
            this.charset = value.toString();
        }
        value = config.get(PARAM_URL_ENCODE_ENTITY_IDS);
        if(value != null){
            this.encodeEntityIds = Boolean.parseBoolean(value.toString());
        }
        value = config.get(PARAM_ENTITY_SCORE_FILE);
        if(value == null || value.toString().isEmpty()){
            score = indexingConfig.getSourceFile(DEFAULT_ENTITY_SCORE_FILE);
        } else {
            score = indexingConfig.getSourceFile(value.toString());
        }
        try {
            initReader(new FileInputStream(score));
        } catch (FileNotFoundException e) {
            throw new IllegalArgumentException("The File with the entity scores "+score.getAbsolutePath()+" does not exist",e);
        }
    }
    /**
     * used by the constructors and {@link #setConfiguration(Map)} to initialise
     * the reader based on the provided File/InputStream.
     * @param is the input stream
     * @param charset the charset
     */
    private void initReader(InputStream is) {
        try {
            reader = new BufferedReader(new InputStreamReader(is, this.charset));
        } catch (UnsupportedEncodingException e) {
            throw new IllegalArgumentException("The parsed encoding "+charset+" is not supported",e);
        }
    }
    @Override
    public boolean hasNext() {
        if(nextLine == null){ //consumed
            getNext();
        }
        return nextLine != null;
    }

    @Override
    public EntityScore next() {
        String line = nextLine;
        nextLine = null; //consume
        String[] parts = line.split(separator);
        if(parts.length > 2){
            log.warn("Line {} does have more than 2 parts {}",
                lineCounter,Arrays.toString(parts));
        }
        Float score;
        if(parts.length >=2){
            try {
                score = Float.parseFloat(parts[1]);
            } catch (NumberFormatException e) {
                log.warn(String.format("Unable to parse the score for " +
                		"Entity %s from value %s in line %s! Use NULL as score 0",
                		parts[0],parts[1],lineCounter));
                score = null;
            }
        } else {
            log.debug("No score for Entity {} in line {}! Use NULL as score",parts[0],lineCounter);
            score = null;
        }
        try {
            return new EntityScore(
                encodeEntityIds ? URLEncoder.encode(parts[0], charset) : parts[0],
                score);
        } catch (UnsupportedEncodingException e) {
            throw new IllegalStateException("Unable to URLEncode EntityId",e);
        }
    }
    private void getNext(){
        try {
            nextLine = reader.readLine();
            lineCounter++;
        } catch (IOException e) {
           throw new IllegalStateException("Unable to read next EntityScore",e);
        }
    }

    @Override
    public void remove() {
        throw new UnsupportedOperationException("Removal form the EnityScore list is not supported");
    }
    @Override
    public boolean needsInitialisation() {
        return false;
    }
    @Override
    public void initialise() {
    }
        
    @Override
    public void close(){
        if(reader != null){
            try {
                reader.close();
            }catch (IOException e) {
                //ignore
            }
        }
    }
}
