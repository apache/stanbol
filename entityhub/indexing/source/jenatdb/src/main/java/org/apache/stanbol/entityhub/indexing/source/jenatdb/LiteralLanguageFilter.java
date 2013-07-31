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
package org.apache.stanbol.entityhub.indexing.source.jenatdb;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.osgi.service.cm.ConfigurationException;

import com.hp.hpl.jena.graph.Node;

/**
 * Allows to filter Tiples based on the language of the value. Triples with
 * values other than <code>{@link Node#isLiteral()} == true</code> are accepted.
 * This is also true for all Literals that do not have a language assigned.
 * @author Rupert Westenthaler
 *
 */
public class LiteralLanguageFilter implements RdfImportFilter {
    /**
     * Allows to configure the literal languages included/excluded during the
     * import of RDF data<p>
     * <b>Syntax: </b><code>{lang1},!{lang2},*</code>
     * <ul>
     * <li>'{lang}' includes an language
     * <li>'!{lang}'excludes an language
     * <li>',' is the separator, additional spaces are trimmed
     * <li>'*' will include all properties not explicitly excluded
     * </ul>
     */
    public static final String PARAM_LITERAL_LANGUAGES = "if-literal-language";
    private Set<String> configuredLanguages;
    private Set<String> excludedLanguages;
    private boolean includeAll;
    
    public LiteralLanguageFilter(){}
    
    /**
     * For unit tests
     * @param config the test config
     */
    protected LiteralLanguageFilter(String config){
        parseLanguages(config);
    }
    
    
    @Override
    public void setConfiguration(Map<String,Object> config) {
        
        Object value = config.get(PARAM_LITERAL_LANGUAGES);
        if(value == null){
            includeAll = true;
            excludedLanguages = Collections.emptySet();
            configuredLanguages = Collections.emptySet();
        } else {
            parseLanguages(value.toString());
        }
    }

    private void parseLanguages(String config){
        configuredLanguages = new HashSet<String>();
        excludedLanguages = new HashSet<String>();
        String[] languages = config.split(",");
        for(int i = 0;i < languages.length;i++){
            languages[i] = languages[i].trim().toLowerCase(Locale.ROOT);
            if(includeAll == false && languages[i].equals("*")){
                includeAll = true;
            }
        }
        for(String lang : languages) {
            if(lang.isEmpty() || lang.equals("*")){
                continue; //ignore null values and * is already processed
            }
            //lang = lang.toLowerCase(); //country codes are upper case
            if(lang.charAt(0) == '!'){ //exclude
                lang = lang.substring(1);
                if(lang.isEmpty()){
                    continue; //only a '!' without an lanugage
                }
                if(configuredLanguages.contains(lang)){
                    throw new IllegalArgumentException(
                        "Langauge '"+lang+"' is both included and excluded (config: "
                        + config+")");
                }
                excludedLanguages.add(lang);
            } else{
                if(excludedLanguages.contains(lang)){
                    throw new IllegalArgumentException( 
                        "Langauge '"+lang+"' is both included and excluded (config: "
                        + config+")");
                }
                configuredLanguages.add(lang);
            }
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
    public boolean accept(Node s, Node p, Node o) {
        if(o.isLiteral()){
            if(includeAll && excludedLanguages.isEmpty()){
                return true; //deactivated
            }
            String lang = o.getLiteralLanguage();
            if(lang != null && !lang.isEmpty()){
                if(includeAll){
                    return !excludedLanguages.contains(lang);
                } else {
                    return configuredLanguages.contains(lang);
                }
            } else { //no plain literal (null) or default language (empty)
                return true; //accept it
            }
        } else {
            return true; //accept all none literals
        }
    }

}
