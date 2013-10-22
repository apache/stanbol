/*
 * Copyright (c) 2012 Sebastian Schaffert
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.apache.stanbol.enhancer.engines.sentiment.classifiers;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.apache.commons.io.IOUtils;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.stanbol.commons.stanboltools.datafileprovider.DataFileListener;
import org.apache.stanbol.commons.stanboltools.datafileprovider.DataFileTracker;
import org.apache.stanbol.enhancer.engines.sentiment.api.LexicalCategoryClassifier;
import org.apache.stanbol.enhancer.engines.sentiment.api.SentimentClassifier;
import org.apache.stanbol.enhancer.engines.sentiment.util.WordSentimentDictionary;
import org.apache.stanbol.enhancer.nlp.pos.LexicalCategory;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A German word classifier based on SentiWS. Reads the SentiWS positive and negative word lists and parses them
 * into an appropriate hash table, so lookups should be extremely fast.
 * <p/>
 * @author Sebastian Schaffert
 * @author Rupert Westenthaler
 */
@Component(immediate=true)
public class SentiWSComponent {

    private static Logger log = LoggerFactory.getLogger(SentiWSComponent.class);
    private static final Map<String,String> modelProperties = new HashMap<String,String>();
    static {
        modelProperties.put("Description", "Sentiment Word List (German)");
        modelProperties.put("Download Location", "http://wortschatz.informatik.uni-leipzig.de/download/");
    }
    @Reference
    private DataFileTracker dataFileProvider;

    protected BundleContext bundleContext;
    
    /**
     * Registration for the {@link SentiWsClassifierDE}. Will be created as
     * soon as both required data files are available and successfully loaded
     */
    protected ServiceRegistration sentiWsClassifierService;
    protected SentiWsClassifierDE sentiWsClassifier;
    
    
    private ModelListener modelListener = new ModelListener();

    protected Set<String> sentiWsFileNames = new HashSet<String>(); 
    protected Set<String> loadedSentiWsFiles = new HashSet<String>();
    
    
    public SentiWSComponent() {}
    
    /**
     * Tracks the SentiWS files and triggers the registration of the service
     */
    private class ModelListener implements DataFileListener {

        @Override
        public boolean available(String resourceName, InputStream is) {
            if(sentiWsFileNames.contains(resourceName)){
                log.info("sentiWs resource {} available",resourceName);
                try {
                    long start = System.currentTimeMillis();
                    if(sentiWsClassifier != null){
                        sentiWsClassifier.parseSentiWS(is);
                        loadedSentiWsFiles.add(resourceName);
                        log.info("   ... loaded in {} ms",(System.currentTimeMillis()-start));
                    }
                } catch (IOException e) {
                    log.warn("Unable to load sentiWs resource '"+resourceName+"!",e);
                    return false; //keep tracking
                } catch (RuntimeException e) {
                    log.error("RuntimeException while loading sentiWs resource '"
                            +resourceName+"!",e);
                    return false; //keep tracking
                }
            } else {
                log.warn("Tracker notified event for non-tracked resource '{}'"
                    + "(tracked: {})!",resourceName,sentiWsFileNames);
            }
            //all resources available ... start the service
            if(loadedSentiWsFiles.equals(sentiWsFileNames)){
                log.info("register Sentiment Classifier for SentiWs (german)");
                registerService();
            } else {
                log.info("loaded {} (required: {})",loadedSentiWsFiles,sentiWsFileNames);
            }
            //remove registration
            return true;
        }

        @Override
        public boolean unavailable(String resourceName) {
            //not used;
            return false;
        }
        
    }
    
    @Activate
    protected void activate(ComponentContext ctx){
        bundleContext = ctx.getBundleContext();
        //TODO: make Filenames configurable
        sentiWsFileNames.add("SentiWS_v1.8b_Negative.txt");
        sentiWsFileNames.add("SentiWS_v1.8b_Positive.txt");
        
        //register files with the DataFileTracker
        for(String sentiWsFile : sentiWsFileNames){
            dataFileProvider.add(modelListener, sentiWsFile, modelProperties);
        }
        sentiWsClassifier = new SentiWsClassifierDE();
    }
    
    protected void registerService() {
        Dictionary<String,Object> serviceProperties = new Hashtable<String,Object>();
        serviceProperties.put("language", "de"); //set the language
        BundleContext bc = bundleContext;
        if(bc != null && sentiWsClassifierService == null){
            sentiWsClassifierService = bc.registerService(
                SentimentClassifier.class.getName(), sentiWsClassifier, 
                serviceProperties);
        }
        
    }

    protected void deactivate(ComponentContext ctx) {
        //end datafile tracking
        dataFileProvider.removeAll(modelListener);
        sentiWsFileNames.clear();
        loadedSentiWsFiles.clear();
        //remove service registration
        if(sentiWsClassifierService != null){
            sentiWsClassifierService.unregister();
        }
        //free up resources
        if(sentiWsClassifier != null){
            sentiWsClassifier.close();
            sentiWsClassifier = null;
        }
        bundleContext = null;
    }
    
    /**
     * The OSGI service registered as soon as the required DataFiles are
     * available
     */
    public static class SentiWsClassifierDE extends LexicalCategoryClassifier implements SentimentClassifier {
    
        private WordSentimentDictionary dict = new WordSentimentDictionary(Locale.GERMAN);

        protected SentiWsClassifierDE(){}
        
        protected void parseSentiWS(InputStream is) throws IOException {
            log.debug("parsing SentiWS word lists ...");
            BufferedReader in = new BufferedReader(new InputStreamReader(is));
            try {
                for(String line = in.readLine(); line != null; line = in.readLine()) {
                    // input file will have a space- or tab-separated list per line:
                    // - first component is the main word with a specification what kind of word it is (can be result from POS tagging)
                    // - second component is the positive or negative sentiment associated with the word
                    // - third argument is a comma-separated list of deflections of the word as they might also occur in text
                    String[] components = line.split("\\s");

                    // parse the weight
                    Double weight = Double.valueOf(components[1]);

                    // get the main word
                    String[] wordPart = components[0].split("\\|");
                    String mainWord = wordPart[0];
                    LexicalCategory cat = getLexicalCategory(wordPart[1]);
                    dict.updateSentiment(cat, mainWord, weight);

                    // get the remaining words (deflections)
                    if(components.length > 2) {
                        for(String word : components[2].split(",")) {
                            dict.updateSentiment(cat, word, weight);
                        }
                    }
                }
            } finally {
                IOUtils.closeQuietly(in);
            }
        }
    
        private LexicalCategory getLexicalCategory(String posTag){
            char c = posTag.charAt(0);
            switch (c) {
                case 'N':
                    return LexicalCategory.Noun;
                case 'V':
                    return LexicalCategory.Verb;
                case 'A':
                    return LexicalCategory.Adjective;
                default: //TODO: change this to a warning and return NULL
                    throw new IllegalStateException("Unsupported posTag '"+posTag+"'!");
            }
        }
        
        @Override
        public String getLanguage() {
            return "de";
        }
    
        /**
         * Given the word passed as argument, return a value between -1 and 1 indicating its sentiment value from
         * very negative to very positive. Unknown words should return the value 0.
         *
         * @param word
         * @return
         */
        @Override
        public double classifyWord(LexicalCategory cat, String word) {
            Double sentiment = dict.getSentiment(cat, word);
            return sentiment != null ? sentiment.doubleValue() : 0.0;
        }
        /**
         * Internally used to free up resources when the service is
         * unregistered
         */
        protected void close(){
            dict.clear();
        }
    }

}
