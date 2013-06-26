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
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.apache.stanbol.entityhub.core.model.InMemoryValueFactory;
import org.apache.stanbol.entityhub.indexing.core.EntityProcessor;
import org.apache.stanbol.entityhub.indexing.core.config.IndexingConfig;
import org.apache.stanbol.entityhub.servicesapi.model.Reference;
import org.apache.stanbol.entityhub.servicesapi.model.Representation;
import org.apache.stanbol.entityhub.servicesapi.model.ValueFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HierarchyProcessor implements EntityProcessor {

    public static final String PARAM_ADMIN1 = "admin1";
    public static final String PARAM_ADMIN2 = "admin2";
    public static final String PARAM_HIERARCHY = "hierarchy";
    public static final String PARAM_COUNTRY_INFO = "country-info";
    
    public static final String DEFAULT_ADMIN1_FILE = "admin1CodesASCII.txt";
    public static final String DEFAULT_ADMIN2_FILE = "admin2Codes.txt";
    public static final String DEFAULT_HIERARCHY_FILE = "hierarchy.zip";
    public static final String DEFAULT_COUNTRY_INFO_FILE = "countryInfo.txt";
    
    
    private final Logger log = LoggerFactory.getLogger(HierarchyProcessor.class);

    private File countryInfoFile;
    private List<File> adminCodesFiles;
    private File hierarchyFile;
    
    private final Map<String, Integer> adminCode2featureId = new HashMap<String, Integer>();

    private final Map<Integer,Collection<Integer>> parentFeature = new HashMap<Integer, Collection<Integer>>();
    private final Map<Integer,Collection<Integer>> adminParentFeature = new HashMap<Integer, Collection<Integer>>();

    private final Map<String, Integer> countryCode2featureId = new HashMap<String, Integer>();
    private IndexingConfig indexingConfig;

    private final ValueFactory vf = InMemoryValueFactory.getInstance();
    public static final int COUNTRY_ID_INDEX = 17;
    
    
    @Override
    public void setConfiguration(Map<String,Object> config) {
        indexingConfig = (IndexingConfig)config.get(IndexingConfig.KEY_INDEXING_CONFIG);
        adminCodesFiles = Arrays.asList(
            getConfiguredFile(config, PARAM_ADMIN1, DEFAULT_ADMIN1_FILE),
            getConfiguredFile(config, PARAM_ADMIN2, DEFAULT_ADMIN2_FILE));
        hierarchyFile = getConfiguredFile(config, PARAM_HIERARCHY, DEFAULT_HIERARCHY_FILE);
        countryInfoFile = getConfiguredFile(config, PARAM_COUNTRY_INFO, DEFAULT_COUNTRY_INFO_FILE);
    }

    /**
     * @param value
     */
    private File getConfiguredFile(Map<String,Object> config, String param, String defaultValue) {
        Object value = config.get(param);
        if(value == null){ //if not set use the default
            value = GeonamesConstants.DEFAULT_SOURCE_FOLDER_NAME + defaultValue;
            log.info("No Geonames.org Admin1 code file configured use the default: {}",value);
        }
        File file = indexingConfig.getSourceFile(value.toString());
        return file;
    }

    @Override
    public boolean needsInitialisation() {
        return true;
    }

    @Override
    public void initialise() {
        for(File af : adminCodesFiles){
            if(!af.isFile()){
                throw new IllegalArgumentException("The configured AdminCodes file "+
                        af +"does not exist. Change the configureation "
                        + "or copy the file to this location!");
            }
        }
        if(!hierarchyFile.isFile()){
            throw new IllegalArgumentException("The configured hierarchy data file "+
                    hierarchyFile +"does not exist. Change the configureation "
                    + "or copy the file to this location!");
        }
        if(!countryInfoFile.isFile()){
            throw new IllegalArgumentException("The configured hierarchy data file "+
                    countryInfoFile +"does not exist. Change the configureation "
                    + "or copy the file to this location!");
        }
        try {
            readAdminCodes();
        } catch (IOException e) {
            throw new IllegalStateException("Unable to read geonames.org administration codes",e);
        }
        try {
            readHierarchy();
        } catch (IOException e) {
            throw new IllegalStateException("Unable to read geonames.org hierarchy codes",e);
        }
    }

    /**
     * There are two sources of hierarchy in the geonames.org dumps. <p>
     * First the Admin Region Codes stored in the main table in combination with
     * the CountryInfo and the AdminRegion infos for the first two levels. This
     * uses  the ISO country code and the additional number for linking the
     * Regions. Second the Hierarchy table providing parentID, childId, [type]
     * information. This uses featureIDs for linking. <p>
     * This Method reads the first data source into memory. For the country
     * related information it calls {@link #readCountryInfos()}.
     * @throws IOException
     */
    private void readAdminCodes() throws IOException{
        long start = System.currentTimeMillis();
        //first read adminCodes based on the countryInfos
        int lineCount = readCountryInfos();
        for(File adminCodeFile : adminCodesFiles){
            BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(adminCodeFile), Charset.forName("utf-8")));
            String line;
            while((line = reader.readLine()) != null){
                if(line.indexOf('#')!=0 && line.length()>0){ //# is used as comment
                    lineCount++;
                    //no tokenizer this time ... need only first and last column!
                    String code = line.substring(0, line.indexOf('\t'));
                    Integer geonamesId = Integer.valueOf(line.substring(line.lastIndexOf('\t')+1));
                    adminCode2featureId.put(code, geonamesId);
                }
            }
            reader.close();
            reader = null;
        }
        log.info("read "+lineCount+" AdminCodes in "+(System.currentTimeMillis()-start)+"ms");
    }
    
    private int readCountryInfos() throws IOException{
        BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(countryInfoFile), Charset.forName("utf-8")));
        String line;
        int lineCount = 0;
        while((line = reader.readLine()) != null){
            if(line.indexOf('#')!=0 && line.length()>0){ //# is used as comment
                LineTokenizer t = new LineTokenizer(line);
                String code = null;
                Integer geonamesId = null;
                int i=1;
                for(;t.hasNext();i++){
                    String actToken = t.next();
                    if(i==1){
                        code = actToken;
                    }
                    if(i == HierarchyProcessor.COUNTRY_ID_INDEX && actToken != null){
                        geonamesId = Integer.valueOf(actToken);
                        break;
                    }
                }
                if(i == HierarchyProcessor.COUNTRY_ID_INDEX && code != null &&
                        geonamesId != null){
                    adminCode2featureId.put(code,geonamesId);
                    countryCode2featureId.put(code,geonamesId);
                    lineCount++;
                } else {
                    log.warn("Unable to parse countryInfo from Line "+line);
                }
            }
        }
        reader.close();
        reader = null;
        return lineCount;
    }
    /**
     * There are two sources of hierarchy in the geonames.org dumps. <p>
     * First the Admin Region Codes stored in the main table in combination with
     * the CountryInfo and the AdminRegion infos for the first two levels. This
     * uses  the ISO country code and the additional number for linking the
     * Regions. Second the Hierarchy table providing parentID, childId, [type]
     * information. This uses featureIDs for linking. <p>
     * This Method processes the second datasource and stores the child -&gt;
     * parents mappings in memory. Administrative hierarchies are stored in a
     * different map. Note also that also for Administrative regions there are
     * some cases where a child has more than one parent.
     * @throws IOException
     */
    private void readHierarchy() throws IOException{
        BufferedReader reader;
        if(hierarchyFile.getName().endsWith(".zip")){
            ZipFile hierarchyArchive;
            try {
                hierarchyArchive = new ZipFile(hierarchyFile);
            } catch (IOException e) {
                //in the init we check if this is a file, exists and we can read ...
                // .. so throw a runtime exception here!
                throw new IllegalArgumentException("Unable to access geonames.org DB Dump hirarchy File",e);
            }
            Enumeration<? extends ZipEntry> e = hierarchyArchive.entries();
            ZipEntry entry = null;
            while(e.hasMoreElements()){
                ZipEntry cur = e.nextElement();
                if(!cur.isDirectory() && cur.getName().equalsIgnoreCase("hierarchy.txt")){
                    entry = cur;
                    break;
                }
            }
            if(entry ==null){
                throw new IllegalStateException("Archive with alternate Names does not contain the \"alternateNames.txt\" file!");
            } else {
                log.info("read hierarchy data fromArchive Entry "+entry.getName());
                reader = new BufferedReader(new InputStreamReader(hierarchyArchive.getInputStream(entry), Charset.forName("utf-8")));
            }
        } else {
            reader = new BufferedReader(new InputStreamReader(new FileInputStream(hierarchyFile), Charset.forName("utf-8")));
        }
        String line;
        int lineCount=0;
        long start = System.currentTimeMillis();
        while((line = reader.readLine()) != null){
            lineCount++;
            LineTokenizer t = new LineTokenizer(line);
            Integer parent = Integer.valueOf(t.next());
            Integer child = Integer.valueOf(t.next());
            String type;
            if(t.hasNext()){
                type = t.next();
            } else {
                type = null;
            }
            if("ADM".equals(type)){
                Collection<Integer> parents = adminParentFeature.get(child);
                if(parents == null){
                    parents = new ArrayList<Integer>(1); //there are only some exceptions with multiple parents
                    adminParentFeature.put(child, parents);
                }
                parents.add(parent);
            } else {
                Collection<Integer> parents = parentFeature.get(child);
                if(parents == null){
                    parents = new ArrayList<Integer>(3);
                    parentFeature.put(child, parents);
                }
                parents.add(parent);
            }
        }
        log.info(String.format("read %d hierarchy relations in %dms",lineCount,System.currentTimeMillis()-start));
    }
    
    @Override
    public void close() {
        // TODO Auto-generated method stub

    }

    @Override
    public Representation process(Representation source) {
        Integer id = source.getFirst(GeonamesPropertyEnum.idx_id.toString(), Integer.class);
        if(id == null){
            log.warn("The <{}> field MUST contain the integer ID!",GeonamesPropertyEnum.idx_id);
            return source;
        }
        //now add the parents based on the codes parsed from the main data
        addParents(source, id, new String[]{
                source.getFirst(GeonamesPropertyEnum.idx_CC.toString(),String.class),
                source.getFirst(GeonamesPropertyEnum.idx_ADM1.toString(),String.class),
                source.getFirst(GeonamesPropertyEnum.idx_ADM2.toString(),String.class),
                source.getFirst(GeonamesPropertyEnum.idx_ADM3.toString(),String.class),
                source.getFirst(GeonamesPropertyEnum.idx_ADM4.toString(),String.class)
        });
        return source;
    }
    /**
     * Recursive method the finds all parents and adds the childs of the current
     * node (not all childs, but only those of the current tree)
     * @param id the id of the lower level
     * @param parents the set used to add all the parents/child mappings
     */
    private void getParents(Integer id, Map<Integer,Collection<Integer>> parents){
        Collection<Integer> current = parentFeature.get(id);
        if(current != null){
            for(Integer parent : current){
                Collection<Integer> childs = parents.get(parent);
                if(childs == null){
                    childs = new HashSet<Integer>();
                    parents.put(parent, childs);
                }
                if(childs.add(id)){
                    getParents(parent, parents);
                }
            }
        }
        current = adminParentFeature.get(id);
        if(current != null){
            for(Integer parent : current){
                Collection<Integer> childs = parents.get(parent);
                if(childs == null){
                    childs = new HashSet<Integer>();
                    parents.put(parent, childs);
                }
                if(childs.add(id)){
                    getParents(parent, parents);
                }
            }
        }
    }
    
    private void addParents(Representation doc,Integer id,String[] adminCodes){
        Integer[] adminIds = new Integer[5];
        //now process the admin Codes (including the country at index 0)
        for(int i=0;i<adminCodes.length;i++){
            if(adminCodes[i] != null && !adminCodes[i].equals("00")){ //00 is used to indicate not known
                adminIds[i] =adminCode2featureId.get(adminCodes[i]); //might also add null!
            }
        }
        //now get the direct parents
        Map<Integer,Collection<Integer>> parents = new HashMap<Integer, Collection<Integer>>();
        getParents(id,parents);
        //add all parents (NOW done by the field mappings configuration)
        //doc.add(GeonamesPropertyEnum.gn_parentFeature.toString(), getFeatureReferences(parents.keySet()));
        //get admin hierarchy

        Set<Integer> parentLevel;
        //add country
        if(adminIds[0] != null){
            doc.add(GeonamesPropertyEnum.gn_parentCountry.toString(), vf.createReference(
                new StringBuilder(GeonamesConstants.GEONAMES_RESOURCE_NS).append(adminIds[0]).append('/').toString()));
            parentLevel = Collections.singleton(adminIds[0]);
        }  else {
            parentLevel = Collections.emptySet();
        }
        //add the admin codes for the 4 levels
        parentLevel = addAdminLevel(doc, GeonamesPropertyEnum.gn_parentADM1, parents, parentLevel, adminIds[1]);
        parentLevel = addAdminLevel(doc, GeonamesPropertyEnum.gn_parentADM2, parents, parentLevel, adminIds[2]);
        parentLevel = addAdminLevel(doc, GeonamesPropertyEnum.gn_parentADM3, parents, parentLevel, adminIds[3]);
        parentLevel = addAdminLevel(doc, GeonamesPropertyEnum.gn_parentADM4, parents, parentLevel, adminIds[4]);
    }
    /**
     * This Method combines the information of <ul>
     * <li> the adminIds originating form the information in the main feature table of geonames
     * <li> hierarchy information originating from the hierarchy table.
     * </ul>
     * and combines them to the full admin regions hierarchy.<br>
     * This code would be much simpler if one would trust one of the two data source.
     * However first tests have shown, that both structures contain some errors!
     * @param doc The doc to add the data
     * @param property the property used for the level
     * @param parents the parent->child mappings for the current geonames feature
     * @param parentLevel the regions of the parent level (should be only one, but sometimes there are more).
     *   This data are based on the hierarchy table.
     * @param adminId the region as stored in the geonames main table (only available for level 1 and 2)
     * @return the regions of this level (should be only one, but sometimes there are more)
     */
    private Set<Integer> addAdminLevel(Representation doc,GeonamesPropertyEnum property, Map<Integer,Collection<Integer>> parents,Set<Integer> parentLevel, Integer adminId){
        Set<Integer> currentLevel = new HashSet<Integer>();
        //first add the admin1 originating from the admin info file
        if(adminId!=null){
            currentLevel.add(adminId);
        }
        for(Integer parent : parentLevel){
            //second add the admin1 via the childs of the country
            Collection<Integer> tmp = parents.get(parent);
            if(tmp != null){
                currentLevel.addAll(tmp);
            }
        }
        if(!currentLevel.isEmpty()){ //now add all the adm1 we found
            doc.add(property.toString(), getFeatureReferences(currentLevel));
            if(currentLevel.size()>1){ //write warning if there are multiple ids
                log.warn("Multiple {} for ID {} (ids: {})",new Object[]{
                        property.name(),doc.getId(),currentLevel.toString()});
            }
        }
        return currentLevel;
    }
    private Collection<Reference> getFeatureReferences(Collection<Integer> ids){
        List<Reference> refs = new ArrayList<Reference>(ids.size());
        for(Integer id : ids){
            if(id != null){
                refs.add(vf.createReference(
                    new StringBuilder(GeonamesConstants.GEONAMES_RESOURCE_NS)
                    .append(id).append('/').toString()));
            }
        }
        return refs;
    }
    
    
}
