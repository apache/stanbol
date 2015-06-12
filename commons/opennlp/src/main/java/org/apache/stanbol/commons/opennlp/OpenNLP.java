/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.stanbol.commons.opennlp;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.nio.charset.Charset;
import java.security.AccessController;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import opennlp.tools.chunker.Chunker;
import opennlp.tools.chunker.ChunkerME;
import opennlp.tools.chunker.ChunkerModel;
import opennlp.tools.namefind.NameFinderME;
import opennlp.tools.namefind.TokenNameFinder;
import opennlp.tools.namefind.TokenNameFinderModel;
import opennlp.tools.postag.POSModel;
import opennlp.tools.postag.POSTagger;
import opennlp.tools.postag.POSTaggerME;
import opennlp.tools.sentdetect.SentenceDetector;
import opennlp.tools.sentdetect.SentenceDetectorME;
import opennlp.tools.sentdetect.SentenceModel;
import opennlp.tools.tokenize.SimpleTokenizer;
import opennlp.tools.tokenize.Tokenizer;
import opennlp.tools.tokenize.TokenizerME;
import opennlp.tools.tokenize.TokenizerModel;
import opennlp.tools.util.InvalidFormatException;

import org.apache.commons.io.IOUtils;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.stanbol.commons.stanboltools.datafileprovider.DataFileProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * OSGI service that let you load OpenNLP Models via the Stanbol 
 * {@link DataFileProvider} infrastructure. This allows users to copy models
 * to the 'datafiles' directory or developer to provide models via via OSGI
 * bundles.<p>
 * This service also provides methods that directly return the OpenNLP component
 * wrapping the model.
 */
@Component(immediate=true)
@Service(value=OpenNLP.class)
public class OpenNLP {
    /**
     * added as link to the download location for requested model files
     * Will show up in the DataFilePorivder tab in the Apache Felix Web Console
     */
    private static final String DOWNLOAD_ROOT = "http://opennlp.sourceforge.net/models-1.5/";

    /**
     * The logger
     */
    private final Logger log = LoggerFactory.getLogger(getClass());
    
    @Reference
    private DataFileProvider dataFileProvider;
     /**
     * Map holding the already built models
     * TODO: change to use a WeakReferenceMap
     */
    protected Map<String,Object> models = new HashMap<String,Object>();
    /**
     * used to sync access to the {@link #models} and {@link #modelCreationLock}
     */
    protected ReadWriteLock modelLock = new ReentrantReadWriteLock();
    /**
     * used to avoid loading the same model multiple times in parallel.
     * The value is a int array with an single element. The int at index zero is
     * used as reference count. When it reaches zero the mapping can be deleted
     * from the map. 
     */
    protected Map<String,int[]> modelCreationLock = new HashMap<String,int[]>();
    /**
     * Default constructor
     */
    public OpenNLP(){ 
        super(); 
    }
    /**
     * Constructor intended to be used when running outside an OSGI environment
     * (e.g. when used for UnitTests)
     * @param dataFileProvider the dataFileProvider used to load Model data.
     */
    public OpenNLP(DataFileProvider dataFileProvider){
        this();
        this.dataFileProvider = dataFileProvider;
    }
    /**
     * Getter for the sentence detection model of the parsed language. 
     * If the model is not yet available a new one is built. The required data
     * are loaded by using the {@link DataFileProvider} service.  
     * @param language the language
     * @return the model or <code>null</code> if no model data are found
     * @throws InvalidFormatException in case the found model data are in the wrong format
     * @throws IOException on any error while reading the model data
     */
    public SentenceModel getSentenceModel(String language) throws InvalidFormatException, IOException {
        return initModel(String.format("%s-sent.bin", language),
            SentenceModel.class);
    }
    
    /**
     * Getter for the sentence detector of the parsed language. 
     * @param language the language
     * @return the model or <code>null</code> if no model data are found
     * @throws InvalidFormatException in case the found model data are in the wrong format
     * @throws IOException on any error while reading the model data
     */
    public SentenceDetector getSentenceDetector(String language) throws IOException {
        SentenceModel sentModel = getSentenceModel(language);
        if(sentModel != null){
            return new SentenceDetectorME(sentModel);
        } else {
            log.debug("No Sentence Detection Model for language '{}'",language);
            return null;
        }
    }
    
    /**
     * Getter for the named entity finder model for the parsed entity type and language.
     * If the model is not yet available a new one is built. The required data
     * are loaded by using the {@link DataFileProvider} service.  
     * @param type the type of the named entities to find (person, organization)
     * @param language the language
     * @return the model or <code>null</code> if no model data are found
     * @throws InvalidFormatException in case the found model data are in the wrong format
     * @throws IOException on any error while reading the model data
     */
    public TokenNameFinderModel getNameModel(String type, String language) throws InvalidFormatException, IOException {
        return initModel(String.format("%s-ner-%s.bin", language, type),
            TokenNameFinderModel.class);
    }
    
    /**
     * Getter for the {@link TokenNameFinder} for the parsed entity type and language.
     * @param type the type of the named entities to find (person, organization)
     * @param language the language
     * @return the model or <code>null</code> if no model data are found
     * @throws InvalidFormatException in case the found model data are in the wrong format
     * @throws IOException on any error while reading the model data
     */
    public TokenNameFinder getNameFinder(String type, String language) throws IOException {
        TokenNameFinderModel model = getNameModel(type, language);
        if(model != null){
            return new NameFinderME(model);
        } else {
            log.debug("TokenNameFinder model for type {} and langauge {} not present",type,language);
            return null;
        }
    }
    
    /**
     * Getter for the tokenizer model for the parsed language.
     * If the model is not yet available a new one is built. The required data
     * are loaded by using the {@link DataFileProvider} service.  
     * @param language the language
     * @return the model or <code>null</code> if no model data are found
     * @throws InvalidFormatException in case the found model data are in the wrong format
     * @throws IOException on any error while reading the model data
     */
    public TokenizerModel getTokenizerModel(String language) throws InvalidFormatException, IOException {
        return initModel(String.format("%s-token.bin", language),TokenizerModel.class);
    }
    /**
     * Getter for the Tokenizer of a given language. This first tries to
     * create an {@link TokenizerME} instance if the required 
     * {@link TokenizerModel} for the parsed language is available. if such a
     * model is not available it returns the {@link SimpleTokenizer} instance.
     * @param language the language or <code>null</code> to build a 
     * {@link SimpleTokenizer}
     * @return the {@link Tokenizer} for the parsed language.
     */
    public Tokenizer getTokenizer(String language) {
        Tokenizer tokenizer = null;
        if(language != null){
            try {
                TokenizerModel model = getTokenizerModel(language);
                if(model != null){
                    tokenizer = new TokenizerME(model);
                }
            } catch (InvalidFormatException e) {
                log.warn("Unable to load Tokenizer Model for "+language+": " +
                		"Will use Simple Tokenizer instead",e);
            } catch (IOException e) {
                log.warn("Unable to load Tokenizer Model for "+language+": " +
                    "Will use Simple Tokenizer instead",e);
            }
        }
        if(tokenizer == null){
            log.debug("Use Simple Tokenizer for language {}",language);
            tokenizer = SimpleTokenizer.INSTANCE;
        } else {
            log.debug("Use ME Tokenizer for language {}",language);
        }
        return tokenizer;
    }
    /**
     * Getter for the "part-of-speech" model for the parsed language.
     * If the model is not yet available a new one is built. The required data
     * are loaded by using the {@link DataFileProvider} service.  
     * @param language the language
     * @return the model or <code>null</code> if no model data are found
     * @throws InvalidFormatException in case the found model data are in the wrong format
     * @throws IOException on any error while reading the model data
     */
    public POSModel getPartOfSpeechModel(String language) throws IOException, InvalidFormatException {
        //typically there are two versions
        //we prefer the perceptron variant but if not available try to build the other
        IOException first = null;
        POSModel model;
        try {
            model = initModel(String.format("%s-pos-perceptron.bin",language), POSModel.class);
        } catch (IOException e) {
            first = e;
            log.warn("Unable to laod preceptron based POS model for "+language,e);
            model = null;
        }
        if(model == null){
            log.debug("No perceptron based POS model for language "+language+
                "available. Will try to load maxent model");
            try {
                model = initModel(String.format("%s-pos-maxent.bin",language), POSModel.class);
            } catch (IOException e) {
                if(first != null){
                    throw first;
                } else {
                    throw e;
                }
            }
        }
        return model;
    }
    
    /**
     * Getter for the "part-of-speech" tagger for the parsed language.
     * @param language the language
     * @return the model or <code>null</code> if no model data are found
     * @throws InvalidFormatException in case the found model data are in the wrong format
     * @throws IOException on any error while reading the model data
     */
    public POSTagger getPartOfSpeechTagger(String language) throws IOException {
        POSModel posModel = getPartOfSpeechModel(language);
        if(posModel != null){
            return new POSTaggerME(posModel);
        } else {
            log.debug("No POS Model for language '{}'",language);
            return null;
        }
    }
    
    /**
     * Getter for the Model with the parsed type, name and properties.
     * @param modelType the type of the Model (e.g. {@link ChunkerModel})
     * @param modelName the name of the model file. MUST BE available via the
     * {@link DataFileProvider}.
     * @param properties additional properties about the model (parsed to the
     * {@link DataFileProvider}. NOTE that "Description", "Model Type" and
     * "Download Location" are set to default values if not defined in the
     * parsed value.
     * @return the loaded (or cached) model
     * @throws InvalidFormatException in case the found model data are in the wrong format
     * @throws IOException on any error while reading the model data
     */
    public <T> T getModel(Class<T> modelType,String modelName, Map<String,String> properties) throws InvalidFormatException, IOException {
        return initModel(modelName, modelType, properties);
    }
    
    /**
     * Getter for the chunker model for the parsed language.
     * If the model is not yet available a new one is built. The required data
     * are loaded by using the {@link DataFileProvider} service.  
     * @param language the language
     * @return the model or <code>null</code> if no model data are present
     * @throws InvalidFormatException in case the found model data are in the wrong format
     * @throws IOException on any error while reading the model data
     */
    public ChunkerModel getChunkerModel(String language) throws InvalidFormatException, IOException {
        return initModel(String.format("%s-chunker.bin", language), ChunkerModel.class);
    }
    
    /**
     * Getter for the {@link Chunker} for a given language
     * @param language the language
     * @return the {@link Chunker} or <code>null</code> if no model is present
     * @throws InvalidFormatException in case the found model data are in the wrong format
     * @throws IOException on any error while reading the model data
     */
    public Chunker getChunker(String language) throws IOException {
        ChunkerModel chunkerModel = getChunkerModel(language);
        if(chunkerModel != null){
             return new ChunkerME(chunkerModel);
        } else {
            log.debug("No Chunker Model for language {}",language);
            return null;
        }
    }
    
//    /**
//     * Activates the component and re-enables all {@link DataFileProvider}s
//     * previously {@link #registerModelLocation(BundleContext, String...) registered}.
//     * @param context the context
//     */
//    @Activate
//    protected void activate(ComponentContext context){
//        synchronized (modelLocations) {
//            for(ModelLocation modelLocation : modelLocations.values()){
//                if(modelLocation.provider == null){
//                    modelLocation.provider = new BundleResourceProvider(
//                        modelLocation.bundleContext, 
//                        modelLocation.paths == null ? null : Arrays.asList(modelLocation.paths));
//                } // still registered -> should never happen unless activate is called twice
//            }
//        }
//    }
//    /**
//     * Deactivates this component. Deactivates all {@link DataFileProvider}s for
//     * {@link #registerModelLocation(BundleContext, String...) registered}
//     * locations to search for OpenNLP models and also 
//     * {@link Map#clear() clears} the {@link #models model cache}.
//     * @param context the context
//     */
//    @Deactivate
//    protected void deactivate(ComponentContext context){
//        synchronized (modelLocations) {
//            for(ModelLocation modelLocation : modelLocations.values()){
//                if(modelLocation.provider != null){
//                    modelLocation.provider.close();
//                    modelLocation.provider = null;
//                }
//            }
//        }
//        //clear the model cache
//        models.clear();
//    }
//    /**
//     * Registers the parsed paths as locations to lookup openNLP models.<p>
//     * This Method is a convenience for manually registering a 
//     * {@link DataFileProvider} that provides the openNLP model classes such as:
//     * <pre><code>
//     *    protected void activate(ComponentContext context){
//     *        this.modelProvider = new BundleResourceProvider(
//     *            context.getBundleContext, Arrays.asList("openNLP/models"));
//     *        ...
//     *    }
//     *    
//     *    protected void deactivate(ComponentContext context){
//     *        if(this.modelProvider != null){
//     *            modelProvider.close();
//     *            modelProvider = null;
//     *        }
//     *        ...
//     *    }
//     * </code></pre><p>
//     * Note that multiple calls with the same bundleContext will cause previous 
//     * registration for the same {@link BundleContext} to be removed.<p>
//     * {@link DataFileProvider}s created by this will be removed/added as this
//     * Component is activated/deactivated. However registrations are not 
//     * persisted and will be gone after an restart of the OSGI environment
//     * @param bundleContext The context of the bundle used to load openNLP models
//     * @param searchPaths The paths used to search openNLP models (via the
//     * bundles classpath). 
//     */
//    public void registerModelLocation(BundleContext bundleContext, String...searchPaths){
//        if(bundleContext == null){
//            throw new IllegalArgumentException("The parsed BundleContext MUST NOT be NULL!");
//        }
//        String bundleSymbolicName = bundleContext.getBundle().getSymbolicName();
//        synchronized (modelLocations) {
//            ModelLocation current = modelLocations.get(bundleSymbolicName);
//            if(current != null){
//                if(Arrays.equals(searchPaths, current.paths)) {
//                    log.debug("ModelLocations for Bundle {} and Paths {} already registered");
//                    return;
//                } else { //remove current registration
//                    log.debug("remove existing ModelLocations for Bundle {} and Paths {}",
//                        bundleSymbolicName,current.paths);
//                    if(current.provider != null){
//                        current.provider.close();
//                    }
//                }
//            } else {
//                current = new ModelLocation();
//                current.bundleContext = bundleContext;
//            }
//            current.paths = searchPaths;
//            current.provider = new BundleResourceProvider(bundleContext, 
//                searchPaths == null ? null : Arrays.asList(searchPaths));
//            modelLocations.put(bundleSymbolicName, current);
//        }
//        
//    }
//    /**
//     * Removes previously registerd openNLP model locations for the parsed bundle
//     * context.
//     * @param bundleContext
//     */
//    public void unregisterModelLocation(BundleContext bundleContext){
//        if(bundleContext == null){
//            throw new IllegalArgumentException("The parsed BundleContext MUST NOT be NULL!");
//        }
//        String bundleSymbolicName = bundleContext.getBundle().getSymbolicName();
//        synchronized (modelLocations) {
//            ModelLocation current = modelLocations.remove(bundleSymbolicName);
//            if(current != null){
//                log.debug("remove modelLocation for Bundle {} and paths {}",
//                    bundleSymbolicName,current.paths);
//                if(current.provider != null){
//                    current.provider.close();
//                }
//            }
//        }
//    }
    
    /**
     * Uses generics to build models of the parsed type. The {@link #models}
     * map is used to lookup already created models.
     * @param <T> the type of the model to create
     * @param name the name of the file with the model data
     * @param modelType the class object representing the model to create
     * @return the model or <code>null</code> if the model data where not found
     * @throws InvalidFormatException if the model data are in an invalid format
     * @throws IOException on any error while loading the model data
     * @throws IllegalStateException on any Exception while creating the model
     */
    private <T> T initModel(String name,Class<T> modelType) throws InvalidFormatException, IOException {
        return initModel(name, modelType,null);
    }
    /**
     * Uses generics to build models of the parsed type. The {@link #models}
     * map is used to lookup already created models.
     * @param <T> the type of the model to create
     * @param name the name of the file with the model data
     * @param modelType the class object representing the model to create
     * @param modelProperties additional metadata about the requested model
     * @return the model or <code>null</code> if the model data where not found
     * @throws InvalidFormatException if the model data are in an invalid format
     * @throws IOException on any error while loading the model data
     * @throws IllegalStateException on any Exception while creating the model
     */
    private <T> T initModel(String name,Class<T> modelType, Map<String,String> modelProperties) throws InvalidFormatException, IOException {
        T model = getCachedModel(name, modelType);
        if(model != null){
            return model;
        } //else create the model
        //We need to avoid creating a model twice in parallel
        modelLock.writeLock().lock();
        int[] lock;
        try {
            lock = modelCreationLock.get(name);
            if(lock == null){
                lock = new int[]{0};
                modelCreationLock.put(name, lock);
            }
            lock[0]++;
        } finally {
            modelLock.writeLock().unlock();
        }
        try {
            //create only one model with the same name in parallel
            synchronized (lock) { 
                //now we have the lock ... 
                //  first check if it was created while we where waiting for the lock
                model = getCachedModel(name, modelType);
                if(model != null){
                    return model;
                }
                //not created in the meantime ... we need to create it!
                T built = loadModel(name, modelType, modelProperties);
                //register the model
                modelLock.writeLock().lock();
                try {
                    models.put(name, built);
                } finally {
                    modelLock.writeLock().unlock();
                }
                return built;
            }
        } finally {
            //we do no longer need the lock
            lock[0]--;
            //check if we need to clean up the modelCreationLock map
            if(lock[0] == 0){
                modelLock.writeLock().lock();
                try {
                    if(lock[0] == 0){
                        modelCreationLock.remove(name);
                    }
                } finally {
                    modelLock.writeLock().unlock();
                }
            }
        }
    }
    private <T> T loadModel(String name, Class<T> modelType,
            Map<String, String> modelProperties) throws InvalidFormatException,
            IOException {
        if(modelProperties != null){ //copy the data to avoid external modifications
            modelProperties = new HashMap<String,String>(modelProperties);
        }else {
            modelProperties = new HashMap<String,String>();
        }
        if(!modelProperties.containsKey("Description")){
            modelProperties.put("Description", "Statistical model for OpenNLP");
        }
        if(!modelProperties.containsKey("Model Type")){
            modelProperties.put("Model Type", modelType.getSimpleName());
        }
        if(!modelProperties.containsKey("Download Location")){
            modelProperties.put("Download Location", DOWNLOAD_ROOT+name);
        }
        InputStream modelDataStream;
        try {
            modelDataStream = lookupModelStream(name,modelProperties);
        } catch (IOException e) {
            log.debug("Unable to load Resource {} via the DataFileProvider",name);
            return null;
        }
        if(modelDataStream == null){
            log.debug("Unable to load Resource {} via the DataFileProvider",name);
            return null;
        }
        T built;
        try {
            Constructor<T> constructor;
            constructor = modelType.getConstructor(InputStream.class);
            built = constructor.newInstance(modelDataStream);
        } catch (SecurityException e) {
            throw new IllegalStateException(String.format(
                "Unable to create %s for %s!",modelType.getSimpleName(),
                name),e);
        } catch (NoSuchMethodException e) {
            throw new IllegalStateException(String.format(
                "Unable to create %s for %s!",modelType.getSimpleName(),
                name),e);
        } catch (IllegalArgumentException e) {
            throw new IllegalStateException(String.format(
                "Unable to create %s for %s!",modelType.getSimpleName(),
                name),e);
        } catch (InstantiationException e) {
            throw new IllegalStateException(String.format(
                "Unable to create %s for %s!",modelType.getSimpleName(),
                name),e);
        } catch (IllegalAccessException e) {
            throw new IllegalStateException(String.format(
                "Unable to create %s for %s!",modelType.getSimpleName(),
                name),e);
        } catch (InvocationTargetException e) {
            //this indicates an exception while creating the instance
            //for InvalidFormatException and IO Exceptions we shall
            //directly throw the cause. for all others wrap the thrown one
            //in an IllegalStateException
            Throwable checked = e.getCause();
            if (checked instanceof InvalidFormatException){
                throw (InvalidFormatException)checked;
            } else if(checked instanceof IOException){
                throw (IOException)checked;
            } else {
                throw new IllegalStateException(String.format(
                    "Unable to create %s for %s!",modelType.getSimpleName(),
                    name),e);
            }
        } finally {
            IOUtils.closeQuietly(modelDataStream);
        }
        return built;
    }
    /**
     * Used to retrieve a model of the parsed model type from the internal cache
     * @param name the name of the model
     * @param modelType the type of the model
     * @return the model or <code>null</code> if not cached
     * @throws IllegalStateException if the cached model does not have the
     * expected type
     */
    private <T> T getCachedModel(String name, Class<T> modelType) {
        modelLock.readLock().lock();
        try {
            Object model = models.get(name);
            if(model != null) {
                if(modelType.isAssignableFrom(model.getClass())){
                    return modelType.cast(model);
                } else {
                    throw new IllegalStateException(String.format(
                        "Incompatible Model Types for name '%s': present=%s | requested=%s",
                        name,model.getClass(),modelType));
                }
            } else {
                return null;
            }
        } finally {
            modelLock.readLock().unlock();
        }
    }
    /**
     * Lookup an openNLP data file via the {@link #dataFileProvider}
     * @param modelName the name of the model
     * @return the stream or <code>null</code> if not found
     * @throws IOException an any error while opening the model file
     */
    protected InputStream lookupModelStream(final String modelName, final Map<String,String> properties) throws IOException {
        try {
            return AccessController.doPrivileged(new PrivilegedExceptionAction<InputStream>() {
                public InputStream run() throws IOException {
                    return dataFileProvider.getInputStream(null, modelName,properties);
                }
            });
        } catch (PrivilegedActionException pae) {
            Exception e = pae.getException();
            if(e instanceof IOException){
                throw (IOException)e;
            } else {
                throw RuntimeException.class.cast(e);
            }
        }        
    }

    /**
     * Remove non UTF-8 compliant characters (typically control characters) so has to avoid polluting the
     * annotation graph with snippets that are not serializable as XML.
     */
    protected static String removeNonUtf8CompliantCharacters(final String text) {
        if (null == text) {
            return null;
        }
        Charset UTF8 = Charset.forName("UTF-8");
        byte[] bytes = text.getBytes(UTF8);
        for (int i = 0; i < bytes.length; i++) {
            byte ch = bytes[i];
            // remove any characters outside the valid UTF-8 range as well as all control characters
            // except tabs and new lines
            if (!((ch > 31 && ch < 253) || ch == '\t' || ch == '\n' || ch == '\r')) {
                bytes[i] = ' ';
            }
        }
        return new String(bytes, UTF8);
    }
}
