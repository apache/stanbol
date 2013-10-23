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
import java.util.Hashtable;
import java.util.Locale;
import java.util.Map;

import org.apache.commons.io.IOUtils;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.lucene.analysis.en.EnglishMinimalStemmer;
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
 * A word classifier for the english language based on SentiWordNet. Reads in a SentiWordNet file and
 * represents mappings from word to sentiment score between -1 and 1 in a hashmap.
 * <p/>
 * Future versions might make use of a disk-based storage of the hashmap to improve memory performance.
 * <p/>
 * Note that a license for SentiWordNet is required if you intend to use the classifier in commercial
 * settings.
 * <p/>
 * @author Sebastian Schaffert
 * @author Rupert Westenthaler
 */
@Component(immediate = true)
public class SentiWordNet {

    private static final Map<String,String> modelProperties = new HashMap<String,String>();
    static {
        modelProperties.put("Description", "Sentiment Word List (German)");
        modelProperties.put("Download Location", "http://wordnet.princeton.edu/");
    }
    private static Logger log = LoggerFactory.getLogger(SentiWordNet.class);

    private static final String SENTIWORDNET_RESOURCE = "SentiWordNet_3.0.0_20120206.txt";

    protected String sentiWordNetFile;
    
    private ModelListener modelListener = new ModelListener();
    
    @Reference
    private DataFileTracker dataFileTracker;

    private BundleContext bundleContext;

    protected SentiWordNetClassifierEN classifier;
    
    protected ServiceRegistration classifierRegistration;
    
    public SentiWordNet() {}
    
    @Activate
    protected void activate(ComponentContext ctx){
        bundleContext = ctx.getBundleContext();
        //TODO: make configurable
        sentiWordNetFile = SENTIWORDNET_RESOURCE;
        
        classifier = new SentiWordNetClassifierEN();

        dataFileTracker.add(modelListener, sentiWordNetFile, modelProperties);
    }

    @Deactivate
    protected void deactivate(ComponentContext ctx){
        if(classifierRegistration != null){
            classifierRegistration.unregister();
            classifierRegistration = null;
        }
        if(classifier != null){
            classifier.close();
            classifier = null;
        }
        dataFileTracker.removeAll(modelListener);
        sentiWordNetFile = null;
    }
    
    /**
     * Tracks the SentiWS files and triggers the registration of the service
     */
    private class ModelListener implements DataFileListener {

        @Override
        public boolean available(String resourceName, InputStream is) {
            if(sentiWordNetFile.equals(resourceName)){
                log.info("{} resource available",resourceName);
                try {
                    long start = System.currentTimeMillis();
                    if(classifier != null){
                        classifier.parseSentiWordNet(is);
                        log.info("   ... loaded in {} ms",(System.currentTimeMillis()-start));
                        registerService(); //register the service
                    }
                } catch (IOException e) {
                    log.warn("Unable to load '"+resourceName+"'!",e);
                    return false; //keep tracking
                } catch (RuntimeException e) {
                    log.error("RuntimeException while loading '"
                            +resourceName+"!",e);
                    return false; //keep tracking
                }
            } else {
                log.warn("Tracker notified event for non-tracked resource '{}'"
                    + "(tracked: {})!",resourceName,sentiWordNetFile);
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
    
    protected void registerService() {
        Dictionary<String,Object> serviceProperties = new Hashtable<String,Object>();
        serviceProperties.put("language", "en"); //set the language
        BundleContext bc = bundleContext;
        if(bc != null){
            classifierRegistration = bc.registerService(
                SentimentClassifier.class.getName(), classifier, 
                serviceProperties);
        }
    }
    /**
     * The OSGI service registered as soon as the required DataFiles are
     * available
     */
    public static class SentiWordNetClassifierEN extends LexicalCategoryClassifier implements SentimentClassifier {

        WordSentimentDictionary dict = new WordSentimentDictionary(Locale.ENGLISH);
        
        private org.apache.lucene.analysis.en.EnglishMinimalStemmer stemmer = new EnglishMinimalStemmer();

        protected SentiWordNetClassifierEN() {}

        protected void parseSentiWordNet(InputStream is) throws IOException {
            BufferedReader in = new BufferedReader(new InputStreamReader(is));
            try {
                // read line by line:
                // - lines starting with # are ignored
                // - valid lines have the format POS ID POSSCORE NEGSCORE SYNONYMS GLOSS separated by tags
                for (String line = in.readLine(); line != null; line = in.readLine()) {
                    line = line.trim();
                    if (line.length() > 0 && line.charAt(0) != '#') {
                        String[] components = line.split("\t");
    
                        try {
                            LexicalCategory cat = parseLexCat(components[0]);
                            double posScore = Double.parseDouble(components[2]);
                            double negScore = Double.parseDouble(components[3]);
                            String synonyms = components[4];
    
                            Double score = posScore - negScore;
    
                            if (score != 0.0) {
                                for (String synonymToken : synonyms.split(" ")) {
                                    // synonymTokens are of the form word#position, so we strip out the position
                                    // part
                                    String[] synonym = synonymToken.split("#");
                                    String stemmed = getStemmed(synonym[0]);
                                    dict.updateSentiment(cat, stemmed, score);
                                }
                            }
    
                        } catch (RuntimeException ex) {
                            log.warn("could not parse SentiWordNet line '{}': {}", line, ex.getMessage());
                        }
                    }
                }
            } finally {
                IOUtils.closeQuietly(in);
            }
        }

        private LexicalCategory parseLexCat(String val) {
            switch (val.charAt(0)) {
                case 'a':
                    return LexicalCategory.Adjective;
                case 'v':
                    return LexicalCategory.Verb;
                case 'n':
                    return LexicalCategory.Noun;
                case 'r':
                    return LexicalCategory.Adverb;
                default:
                    throw new IllegalStateException("Uncown POS tag '"+val+"'!");
            }
        }


        /**
         * Given the word passed as argument, return a value between -1 and 1 indicating its sentiment value
         * from very negative to very positive. Unknown words should return the value 0.
         * 
         * @param word
         * @return
         */
        @Override
        public double classifyWord(LexicalCategory cat, String word) {
            Double sentiment = dict.getSentiment(cat, getStemmed(word));
            return sentiment != null ? sentiment.doubleValue() : 0.0;
        }

        private String getStemmed(String word) {
            return word.substring(0, stemmer.stem(word.toCharArray(), word.length()));
        }

        @Override
        public String getLanguage() {
            return "en";
        }
        
        protected void close(){
            dict.clear();
        }
    }
}
