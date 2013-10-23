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
package org.apache.stanbol.entityhub.indexing.freebase.processor;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;

import org.apache.stanbol.commons.namespaceprefix.NamespaceMappingUtils;
import org.apache.stanbol.commons.namespaceprefix.NamespacePrefixProvider;
import org.apache.stanbol.commons.namespaceprefix.NamespacePrefixService;
import org.apache.stanbol.entityhub.core.model.InMemoryValueFactory;
import org.apache.stanbol.entityhub.indexing.core.EntityProcessor;
import org.apache.stanbol.entityhub.indexing.core.config.IndexingConfig;
import org.apache.stanbol.entityhub.servicesapi.defaults.NamespaceEnum;
import org.apache.stanbol.entityhub.servicesapi.model.Reference;
import org.apache.stanbol.entityhub.servicesapi.model.Representation;
import org.apache.stanbol.entityhub.servicesapi.model.Text;
import org.apache.stanbol.entityhub.servicesapi.model.ValueFactory;
import org.apache.stanbol.entityhub.servicesapi.yard.Yard;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FreebaseKeyProcessor implements EntityProcessor {

    private static final Logger log = LoggerFactory.getLogger(FreebaseKeyProcessor.class);
    
    /**
     * Allows to enable/disable <code>owl:sameAs</code> links to dbpedia
     */
    public static final String PARAM_DBPEDIA_STATE = "dbpedia";
    public static final boolean DEFAULT_DBPEDIA_STATE = true;
    
    /**
     * Allows to enable/disable <code>owl:sameAs</code> links to musicbrainz
     */
    public static final String PARAM_MUSICBRAINZ_STATE = "musicbrainz";
    public static final boolean DEFAULT_MUSICBRAINZ_STATE = true;

//    public static final String PARAM__STATE = "";
//    public static final boolean DEFAULT__STATE = true;
//
//    public static final String PARAM__STATE = "";
//    public static final boolean DEFAULT__STATE = true;
    
    
    private static final String KEY_NS = "http://rdf.freebase.com/key/";
    private static final String FB_NS = "http://rdf.freebase.com/ns/";
    private static final int FB_NS_LEN = FB_NS.length();
    
    private static final String WP_PREFIX = "wikipedia.";
    private static final int WP_PREFIX_LEN = WP_PREFIX.length();
    private static final String WP_EN = KEY_NS + WP_PREFIX + "en";
    
    private static final String MB_KEY = KEY_NS + "authority.musicbrainz";
    
    private static final String MB_NS = "http://musicbrainz.org/";
    private static final CharSequence MUSIC_PROP_PREFIX = "music.";
    private static final int MUSIC_PROP_PREFIX_LEN = MUSIC_PROP_PREFIX.length();
    private static final Set<String> MB_TYPES = new HashSet<String>();
    static{
        MB_TYPES.add("recording");
        MB_TYPES.add("artist");
        MB_TYPES.add("release");
    }
    
    private static final String SAME_AS = NamespaceEnum.owl + "sameAs";
    private static final String RDF_TYPE = NamespaceEnum.rdf + "type";
    private static final String RDFS_LABEL = NamespaceEnum.rdfs + "label";
   
    public static final String PARAM_LINK_PROPERTY = "link-property";
    public static final String DEFAULT_LINK_PROPERTY = SAME_AS;
    private String linkProperty;
    private boolean dbpediaState;
    private boolean musicbrainzState;
    
    @Override
    public void setConfiguration(Map<String,Object> config) {
        IndexingConfig indexingConfig = (IndexingConfig)config.get(IndexingConfig.KEY_INDEXING_CONFIG);
        NamespacePrefixService nsPrefixService = indexingConfig.getNamespacePrefixService();
        
        Object value = config.get(PARAM_LINK_PROPERTY);
        if(value != null){
            linkProperty = nsPrefixService.getFullName(value.toString());
            if(linkProperty == null){
                throw new IllegalArgumentException("Unknown Namespace Prefix use in "
                    + PARAM_LINK_PROPERTY+'='+value+"!");
            }
        } else {
            linkProperty = DEFAULT_LINK_PROPERTY;
        }
        value = config.get(PARAM_DBPEDIA_STATE);
        if(value != null){
            dbpediaState = Boolean.parseBoolean(value.toString());
        } else {
            dbpediaState = DEFAULT_DBPEDIA_STATE;
        }
        value = config.get(PARAM_MUSICBRAINZ_STATE);
        if(value != null){
            musicbrainzState = Boolean.parseBoolean(value.toString());
        } else {
            musicbrainzState = DEFAULT_MUSICBRAINZ_STATE;
        }
        
    }

    @Override
    public boolean needsInitialisation() {
        return false;
    }

    @Override
    public void initialise() {
    }

    @Override
    public void close() {

    }

    @Override
    public Representation process(Representation rep) {
        //wikipedia
        if(dbpediaState){
            //we try to link only a single page. So get the English label and
            //search for the according dbpedia key 
            Text enLabel = rep.getFirst(RDFS_LABEL, "en");
            String mainKey = enLabel != null ? decodeKey(enLabel.getText()).replace(' ', '_') : null;
            Iterator<Text> wpEnKeys = rep.getText(WP_EN);
            Collection<String> keys = new ArrayList<String>();
            boolean foundMain = false;
            if(wpEnKeys.hasNext()){ //link to the English dbpedia
                while(!foundMain & wpEnKeys.hasNext()){
                    String key = decodeKey(wpEnKeys.next().getText());
                    if(key.equals(mainKey)){
                        foundMain = true;
                        rep.addReference(linkProperty, linkeDbPedia(null, key));
                    } else {
                        keys.add(key);
                    }
                }
                if(!foundMain){ //add all links
                    for(String key : keys){
                        rep.addReference(linkProperty, linkeDbPedia(null, key));
                    }
                }
            } else { //search for other wikipedia keys
                Map<String,String> wikipediaFields = new HashMap<String,String>();
                //(1) collect the fields
                for(Iterator<String> fields = rep.getFieldNames();fields.hasNext();){
                    String field = fields.next();
                    int nsIndex = field.lastIndexOf('/')+1;
                    if(field.indexOf(WP_PREFIX, nsIndex) == nsIndex &&
                            //no '_' in the property name
                            field.indexOf('_',nsIndex+WP_PREFIX_LEN+2) < 1){
                        String language = field.substring(nsIndex+WP_PREFIX.length(), field.length());
                        wikipediaFields.put(field, language);
                    } // else no key:wikipedia.* field
                }
                //(2) add the values to avoid concurrent modification exceptions
                for(Entry<String,String> entry : wikipediaFields.entrySet()){
                    for(Iterator<Text> langWpKeys = rep.getText(entry.getKey()); langWpKeys.hasNext();){
                        rep.addReference(linkProperty,
                            linkeDbPedia(entry.getValue(),langWpKeys.next().getText()));
                    }
                }
            }
        }
        if(musicbrainzState){
            Iterator<Text> mbKeys = rep.getText(MB_KEY);
            if(mbKeys.hasNext()){
                String key = mbKeys.next().getText();
                //we need the type
                Iterator<Reference> types = rep.getReferences(RDF_TYPE);
                String type = null;
                while(types.hasNext() && !MB_TYPES.contains(type)){
                    String fbType = types.next().getReference();
                    if(MUSIC_PROP_PREFIX.equals(fbType.subSequence(FB_NS_LEN, 
                        FB_NS_LEN+MUSIC_PROP_PREFIX_LEN))){
                        type = fbType.substring(FB_NS_LEN+MUSIC_PROP_PREFIX_LEN);
                    }
                }
                if(type != null){ 
                    StringBuilder uri = new StringBuilder(MB_NS);
                    uri.append(type).append('/').append(key).append("#_");
                    rep.addReference(linkProperty, uri.toString());
                }
            }
        }
        return rep;
    }

    private String linkeDbPedia(String language, String key) {
        final StringBuilder uri;
        if(language == null){
            uri = new StringBuilder("http://dbpedia.org/resource/");
        } else {
            uri = new StringBuilder("http://").append(language).append(".dbpedia.org/resource/");
        }
        return uri.append(key).toString();
    }
    /**
     * Decodes Freebase.com keys using the '<code>$0000</code>' encoding for chars.
     * This encoding uses a 4 digit hex number to represent chars See the
     * Freebase documentation for details. 
     * @param encodedKey
     * @return
     */
    public static String decodeKey(String encodedKey){
        StringBuilder key = null; //lazy initialisation for performance
        int index = 0;
        final int length = encodedKey.length();
        while(index < length){
            int next = encodedKey.indexOf('$', index);
            if(next < 0){
                if(key == null){
                    return encodedKey; //no decoding needed
                }
                next = length;
            }
            if(key == null){
                //init the StringBuilder with the maximum possible size
                key = new StringBuilder(encodedKey.length());
            }
            if(next > index){ //add chars that do not need decoding
                key.append(encodedKey, index, next);
            }
            if(next < length){ //decode char
                try {
                    if(next+4 < length){
                        key.appendCodePoint(Integer.parseInt(
                            encodedKey.substring(next+1, next+5), 16));
                    } else {
                        String section = encodedKey.substring(next, length);
                        log.warn("Unable to decode Secton ["+next+"-"+(length)+"|'"
                                + section+"'] from key '"+ encodedKey+"'! -> add plain "
                                + "section instead!");
                        key.append(section);
                    }
                } catch (NumberFormatException e) {
                    String section = encodedKey.substring(next, next+5);
                    log.warn("Unable to decode Secton ["+next+"-"+(next+5)+"|'"
                            + section+"'] from key '"+ encodedKey+"'! -> add plain "
                            + "section instead!");
                    key.append(section);
                }
            }
            index = next+5; //add the $0000
        }
        return key.toString();
    }
}
