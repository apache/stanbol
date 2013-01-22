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
package org.apache.stanbol.entityhub.indexing.geonames;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.apache.stanbol.entityhub.core.model.InMemoryValueFactory;
import org.apache.stanbol.entityhub.indexing.core.EntityProcessor;
import org.apache.stanbol.entityhub.indexing.core.config.IndexingConfig;
import org.apache.stanbol.entityhub.indexing.geonames.AlternateLabelProcessor.FeatureName.NameType;
import org.apache.stanbol.entityhub.servicesapi.model.Representation;
import org.apache.stanbol.entityhub.servicesapi.model.Text;
import org.apache.stanbol.entityhub.servicesapi.model.ValueFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AlternateLabelProcessor implements EntityProcessor {

    private static final String PARAM_ALTERNATE_LABELS = "alt-labels";

    private static final String ALTERNATE_LABELS_FILE = "alternateNames.zip";

    private final Logger log = LoggerFactory.getLogger(AlternateLabelProcessor.class);
    
    private final ValueFactory vf = InMemoryValueFactory.getInstance();
    
    /**
     * Names for features. This includes also postal codes, abbreviations, 
     * airport codes and so on.
     * @author westei
     *
     */
    public static final class FeatureName {
        enum NameType {
            naturalLanguage,
            postal,
            link,
            abbreviation,
            airportCode,
            unknown
        }
        private final NameType type;
        private final int labelID;
        private final Integer geonameID;
        private final String name;
        private final String lang;
        private final boolean preferred;
        private final boolean shortName;
        private boolean colloquial;
        private boolean historic;
        private static final String TRUE = "1";
        protected FeatureName(String line){
            LineTokenizer t = new LineTokenizer(line);
            labelID = Integer.parseInt(t.next()); //first Elem the labelID
            geonameID = Integer.parseInt(t.next());
            String language = t.next();
            if(language != null && (language.length() == 2 || language.length() == 3)){
                this.lang = language;
            } else {
                this.lang = null; //no valied lang Code
            }
            if(language == null || language.length()<=3){
                type = NameType.naturalLanguage;
            } else if("post".equals(language)){
                type = NameType.postal;
            } else if("link".equals(language)) {
                type = NameType.link;
            } else if("abbr".equals(language)) {
                type = NameType.abbreviation;
            } else if("iata".equals(language) || "icao".equals(language) || "faac".equals(language)){
                type = NameType.airportCode;
            } else {
                type = NameType.unknown; // e.g. fr_1793 for French Revolution names
            }
            name = t.next();
            if(name == null){
                throw new IllegalStateException(" Unable to parse name from line:" + line);
            }
            String act = t.next();
            this.preferred = act != null && act.equals(TRUE);
            act = t.next();
            this.shortName = act != null && act.equals(TRUE);
            act = t.next();
            this.colloquial = act != null && act.equals(TRUE);
            act = t.next();
            this.historic = act != null && act.equals(TRUE);
        }
        public final Integer getGeonameID() {
            return geonameID;
        }
        public final String getName() {
            return name;
        }
        public final String getLang() {
            return lang;
        }
        public final boolean isPreferred() {
            return preferred;
        }
        public final boolean isShortName() {
            return shortName;
        }
        public final boolean isColloquial() {
            return colloquial;
        }
        public final boolean isHistoric() {
            return historic;
        }
        public final boolean isNaturalLanguageLabel(){
            return type == NameType.naturalLanguage;
        }
        public final NameType getLabelType(){
            return type;
        }
        @Override
        public final boolean equals(Object obj) {
            return obj instanceof FeatureName && ((FeatureName)obj).labelID == labelID;
        }
        @Override
        public final int hashCode() {
            return labelID;
        }
        public final String toString(){
            return name+(lang!=null?('@'+lang):"");
        }
    }
    
    private final Map<Integer,List<FeatureName>> featureNames = new HashMap<Integer,List<FeatureName>>();

    private File alternateNamesFile;

    private IndexingConfig indexingConfig;

    @Override
    public void setConfiguration(Map<String,Object> config) {
        indexingConfig = (IndexingConfig)config.get(IndexingConfig.KEY_INDEXING_CONFIG);
        Object value = config.get(PARAM_ALTERNATE_LABELS);
        if(value == null){ //if not set use the default
            value = GeonamesConstants.DEFAULT_SOURCE_FOLDER_NAME + ALTERNATE_LABELS_FILE;
            log.info("No Geonames.org alternate label source set use the default: {}",value);
        }
        alternateNamesFile = indexingConfig.getSourceFile(value.toString());
    }

    @Override
    public boolean needsInitialisation() {
        return true;
    }

    @Override
    public void initialise() {
        if(!alternateNamesFile.isFile()){
            throw new IllegalArgumentException("The configured geonames.org alternate label file "
                +alternateNamesFile+" does not exist. Plase change the configuration or copy the "
                + "tile to that location.");
        }
        BufferedReader reader;
        if(alternateNamesFile.getName().endsWith(".zip")){
            ZipFile alternateNamesArchive;
            try {
                alternateNamesArchive = new ZipFile(alternateNamesFile);
            } catch (IOException e) {
                //in the init we check if this is a file, exists and we can read ...
                // .. so throw a runtime exception here!
                throw new IllegalArgumentException("Unable to access geonames.org DB Dump file",e);
            }
            Enumeration<? extends ZipEntry> e = alternateNamesArchive.entries();
            ZipEntry entry = null;
            while(e.hasMoreElements()){
                ZipEntry cur = e.nextElement();
                if(!cur.isDirectory() && cur.getName().equalsIgnoreCase("alternatenames.txt")){
                    entry = cur;
                    break;
                }
            }
            if(entry == null){
                throw new IllegalStateException("Archive with alternate Names does not contain the \"alternateNames.txt\" file!");
            } else {
                log.info("read alternate names from Archive Entry "+entry.getName());
                try {
                    reader = new BufferedReader(new InputStreamReader(alternateNamesArchive.getInputStream(entry), Charset.forName("utf-8")));
                } catch (IOException ex) {
                    throw new IllegalArgumentException("Unable to read Entry '" + entry.getName() 
                        + "' from alternate names file "+alternateNamesFile,ex);
                }
            }
        } else {
            try {
                reader = new BufferedReader(new InputStreamReader(new FileInputStream(alternateNamesFile), Charset.forName("utf-8")));
            } catch (FileNotFoundException e) {
                throw new IllegalArgumentException("Unable to read Alternate names "
                        + "' from alternate names file "+alternateNamesFile,e);
            }
        }
        FeatureName name;
        int lineCount = 0;
        EnumMap<NameType, int[]> labelTypeCounts = new EnumMap<NameType, int[]>(NameType.class);
        for(NameType entry :NameType.values()){
            labelTypeCounts.put(entry, new int[]{0});
        }
        String line;
        long start = System.currentTimeMillis();
        try {
            while((line = reader.readLine()) != null){
                try {
                    name = new FeatureName(line);
                } catch (RuntimeException e) {
                    log.warn("Unable to parse Featurname for line: "+line,e);
                    continue;
                }
                List<FeatureName> names = featureNames.get(name.geonameID);
                if(names == null){
                    names = new ArrayList<FeatureName>();
                    featureNames.put(name.geonameID, names);
                }
                if(name.isPreferred()){
                    names.add(0, name);
                } else {
                    names.add(name);
                }
                lineCount++;
                labelTypeCounts.get(name.getLabelType())[0]++; //increase the count for this type!
                if(log.isDebugEnabled() && lineCount%10000==0){
                    log.debug("processed "+lineCount+" labels");
                }
            }
        } catch (IOException e) {
            throw new IllegalStateException("Unable to read data from alternate label file "
                +alternateNamesFile,e);
        }
        log.info("read "+lineCount+" alternate Names for "+featureNames.size()+" Features in "+(System.currentTimeMillis()-start)+"ms");
        for(Entry<NameType, int[]> count : labelTypeCounts.entrySet()){
            log.info("   "+count.getKey().toString()+": "+count.getValue()[0]);
        }
    }

    @Override
    public void close() {
        featureNames.clear();
        alternateNamesFile = null;
    }

    @Override
    public Representation process(Representation source) {
        Integer id = source.getFirst(GeonamesPropertyEnum.idx_id.toString(), Integer.class);
        if(id == null){
            log.warn("The <{}> field MUST contain the integer ID!",GeonamesPropertyEnum.idx_id);
            return source;
        }
        List<FeatureName> alternateNames = featureNames.remove(id); //use remove, because we need not need it a 2nd time!
        if(alternateNames != null){
            List<Text> altList = new ArrayList<Text>(alternateNames.size());
            List<Text> officialList = new ArrayList<Text>(alternateNames.size());
            List<String> postalCodes = new ArrayList<String>();
            List<URL> wikipediaLinks = new ArrayList<URL>();
            List<Text> shortNames = new ArrayList<Text>();
            List<Text> colloquialNames = new ArrayList<Text>();
            for(FeatureName name : alternateNames){
                if(name.isNaturalLanguageLabel()){
                    Text act = vf.createText(name.getName(),name.getLang());
                    if(name.isPreferred()){
                        officialList.add(act);
                    } else {
                        altList.add(act);
                    }
                    if(name.isShortName()){
                        shortNames.add(act);
                    }
                    if(name.isColloquial()){
                        colloquialNames.add(act);
                    }
                } else if(name.getLabelType() == NameType.postal){
                    postalCodes.add(name.getName());
                } else if(name.getLabelType() == NameType.link){
                    if(name.getName().contains("wikipedia.org")){
                        try {
                            wikipediaLinks.add(new URL(name.getName()));
                        } catch (MalformedURLException e) {
                            log.warn("Unable to parse URL for link label "+name.getName());
                            //ignore
                        }
                    }
                }
            }
            if(!altList.isEmpty()){
                source.add(GeonamesPropertyEnum.gn_alternateName.toString(),altList);
            }
            if(!officialList.isEmpty()){
                source.add(GeonamesPropertyEnum.gn_officialName.toString(),officialList);
            }
            if(!postalCodes.isEmpty()){
                source.add(GeonamesPropertyEnum.gn_postalCode.toString(), postalCodes);
            }
            if(!wikipediaLinks.isEmpty()){
                source.add(GeonamesPropertyEnum.gn_wikipediaArticle.toString(), wikipediaLinks);
            }
            if(!shortNames.isEmpty()){
                source.add(GeonamesPropertyEnum.gn_shortName.toString(), shortNames);
            }
            if(!colloquialNames.isEmpty()){
                source.add(GeonamesPropertyEnum.gn_colloquialName.toString(), colloquialNames);
            }
        }
        return source;
    }
}
