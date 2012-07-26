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
import java.math.BigDecimal;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Dictionary;
import java.util.EnumMap;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.TreeMap;
import java.util.Map.Entry;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.apache.stanbol.entityhub.core.mapping.DefaultFieldMapperImpl;
import org.apache.stanbol.entityhub.core.mapping.FieldMappingUtils;
import org.apache.stanbol.entityhub.core.mapping.ValueConverterFactory;
import org.apache.stanbol.entityhub.core.site.CacheUtils;
import org.apache.stanbol.entityhub.core.utils.TimeUtils;
import org.apache.stanbol.entityhub.indexing.geonames.GeoNamesIndexer.FeatureName.NameType;
import org.apache.stanbol.entityhub.servicesapi.defaults.DataTypeEnum;
import org.apache.stanbol.entityhub.servicesapi.defaults.NamespaceEnum;
import org.apache.stanbol.entityhub.servicesapi.mapping.FieldMapper;
import org.apache.stanbol.entityhub.servicesapi.model.Reference;
import org.apache.stanbol.entityhub.servicesapi.model.Representation;
import org.apache.stanbol.entityhub.servicesapi.model.Text;
import org.apache.stanbol.entityhub.servicesapi.model.ValueFactory;
import org.apache.stanbol.entityhub.servicesapi.yard.Yard;
import org.apache.stanbol.entityhub.servicesapi.yard.YardException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class GeoNamesIndexer {

    public static final String[] fieldMappings;
    static {
        ArrayList<String> mappings = new ArrayList<String>();
        mappings.add(Properties.gn_name.toString());
        //While indexing I use the UTF8 name as RDFS label (ASKII as fallback).
        //THis should be also the case for updated documents
        mappings.add(Properties.gn_name.toString()+" > "+Properties.rdfs_label.toString());
        mappings.add(Properties.gn_alternateName.toString());
        mappings.add(Properties.gn_countryCode.toString());
        mappings.add(Properties.gn_featureClass.toString());
        mappings.add(Properties.gn_featureCode.toString());
        mappings.add(Properties.gn_officialName.toString());
        //This cache copies the values of the sub-properties of parentFeature
        //to the super property. So we need to write the according mappings
        mappings.add(Properties.gn_parentADM1.toString());
        mappings.add(Properties.gn_parentADM1.toString()+" > "+Properties.gn_parentFeature.toString());
        mappings.add(Properties.gn_parentADM2.toString());
        mappings.add(Properties.gn_parentADM2.toString()+" > "+Properties.gn_parentFeature.toString());
        mappings.add(Properties.gn_parentADM3.toString());
        mappings.add(Properties.gn_parentADM3.toString()+" > "+Properties.gn_parentFeature.toString());
        mappings.add(Properties.gn_parentADM4.toString());
        mappings.add(Properties.gn_parentADM4.toString()+" > "+Properties.gn_parentFeature.toString());
        mappings.add(Properties.gn_parentCountry.toString());
        mappings.add(Properties.gn_parentCountry.toString()+" > "+Properties.gn_parentFeature.toString());
        mappings.add(Properties.gn_parentFeature.toString());
        //population is converted to long (NOTE: population of Asia > Integer.MAX_VALUE)
        mappings.add(Properties.gn_population.toString()+" | d=xsd:long");
        mappings.add(Properties.gn_postalCode.toString());
        mappings.add(Properties.gn_shortName.toString());
        mappings.add(Properties.gn_wikipediaArticle.toString());
        // Altitude is integer meters
        mappings.add(Properties.geo_alt.toString()+" | d=xsd:int");
        // Latitude and Longitude as BigDecimals (xsd:decimal)
        mappings.add(Properties.geo_lat.toString()+" | d=xsd:decimal");
        mappings.add(Properties.geo_long.toString()+" | d=xsd:decimal");
        mappings.add(Properties.rdf_type.toString());
        fieldMappings = mappings.toArray(new String[mappings.size()]);
    }
    private static final Logger log = LoggerFactory.getLogger(GeoNamesIndexer.class);

    private Yard yard;
    private ValueFactory vf;
    private boolean indexOntology = false;
    private long startPosition;
    private int indexingChunkSize = 1000;

    private File dataDir;
    @SuppressWarnings("unused") //TODO implement indexing of Ontology
    private File geonamesOntFile;
    private File alternateNamesFile;
    private File hierarchyFile;
    private List<File> adminCodesFiles;
    private File countryInfoFile;
    private final int countryGeonamesIdPos = 17;
    private File geonamesArchiveFile;
    //private final String geonamesOntBase = "http://www.geonames.org/ontology/";
    private final String geonamesFeatureBase = "http://sws.geonames.org/";
    //private final String geonamesCountryBase = "http://www.geonames.org/countries/";
    //for date processing we use joda time!
    private final Map<Integer,List<FeatureName>> featureNames = new TreeMap<Integer,List<FeatureName>>();
    private final Map<String, Integer> adminCode2featureId = new TreeMap<String, Integer>();

    private final Map<Integer,Collection<Integer>> parentFeature = new TreeMap<Integer, Collection<Integer>>();
    private final Map<Integer,Collection<Integer>> adminParentFeature = new TreeMap<Integer, Collection<Integer>>();

    private final Map<String, Integer> countryCode2featureId = new TreeMap<String, Integer>();
    /**
     * Key used to parse the Yard used for indexing
     */
    public static final String KEY_YARD = "org.apache.stanbol.entityhub.indexing.yard";
    /**
     * Used to parse the ID of the Item to start/resume the indexing
     */
    public static final String KEY_START_INDEX = "org.apache.stanbol.entityhub.indexing.startIndex";
    /**
     * State used to config if the geonames.org thesaurus should be included in the index.
     */
    public static final String KEY_INDEX_ONTOLOGY_STATE = "org.apache.stanbol.entityhub.indexing.geonames.indexOntology";

    /**
     * Key used to configure the directory that contains all the data needed
     * for indexing geonames.org
     */
    public static final String KEY_DATA_DIR = "org.apache.stanbol.entityhub.indexing.geonames.dataDir";
    /**
     * key used to parse the name of the zip archive with the geonames.org dump.
     * Typically the allcountry dump.
     */
    public static final String KEY_GEONAMES_ARCHIVE = "org.apache.stanbol.entityhub.indexing.geonames.dbdumpArchive";
    /**
     * Key used to parse the name of the file with the country informations
     */
    public static final String KEY_COUNTRY_INFOS = "org.apache.stanbol.entityhub.indexing.geonames.countryInfoFile";
    /**
     * Key used to parse the name of the file with the admin level1 codes
     */
    public static final String KEY_ADMIN1_CODES = "org.apache.stanbol.entityhub.indexing.geonames.admin1CodesFile";
    /**
     * Key used to parse the name of the file with the admin level2 codes
     */
    public static final String KEY_ADMIN2_CODES = "org.apache.stanbol.entityhub.indexing.geonames.admin2CodesFile";
    /**
     * Key used to parse the name of the file with the alternate names
     */
    public static final String KEY_ALTERNATE_NAMES = "org.apache.stanbol.entityhub.indexing.geonames.alternateNamesFile";
    /**
     * Key used to parse the name of the file with the geonames ontology
     */
    public static final String KEY_GEONAMES_ONTOLOGY = "org.apache.stanbol.entityhub.indexing.geonames.geonamesOntologyFile";

    public static final String KEY_CHUNK_SIZE = "org.apache.stanbol.entityhub.indexing.geonames.chunkSize";
    /**
     * Key used to parse the hierarchy file
     */
    public static final String KEY_HIERARCHY = "org.apache.stanbol.entityhub.indexing.geonames.hierarchyFile";

    private static final Map<String,Reference> indexDocRefs = new HashMap<String, Reference>();

    private enum Properties{
        rdf_type(NamespaceEnum.rdf.getNamespace(),"type"),
        rdfs_label(NamespaceEnum.rdfs.getNamespace(),"label"),
        dc_creator(NamespaceEnum.dcTerms.getNamespace(),"creator"),
        dc_date(NamespaceEnum.dcTerms.getNamespace(),"date"),
        gn_Feature(NamespaceEnum.geonames.getNamespace(),"Feature"),
        //gn_Country(NamespaceEnum.geonames.getNamespace(),"Country"),
        gn_countryCode(NamespaceEnum.geonames.getNamespace(),"countryCode"),
        //gn_Map(NamespaceEnum.geonames.getNamespace(),"Map"),
        //gn_RDFData(NamespaceEnum.geonames.getNamespace(),"RDFData"),
        //gn_WikipediaArticle(NamespaceEnum.geonames.getNamespace(),"WikipediaArticle"),
        gn_parentFeature(NamespaceEnum.geonames.getNamespace(),"parentFeature"),
        gn_parentCountry(NamespaceEnum.geonames.getNamespace(),"parentCountry"),
        gn_parentADM1(NamespaceEnum.geonames.getNamespace(),"parentADM1"),
        gn_parentADM2(NamespaceEnum.geonames.getNamespace(),"parentADM2"),
        gn_parentADM3(NamespaceEnum.geonames.getNamespace(),"parentADM3"),
        gn_parentADM4(NamespaceEnum.geonames.getNamespace(),"parentADM4"),
        //gn_childrenFeatures(NamespaceEnum.geonames.getNamespace(),"childrenFeatures"),
        //gn_inCountry(NamespaceEnum.geonames.getNamespace(),"inCountry"),
        //gn_locatedIn(NamespaceEnum.geonames.getNamespace(),"locatedIn"),
        //gn_locationMap(NamespaceEnum.geonames.getNamespace(),"locationMap"),
        //gn_nearby(NamespaceEnum.geonames.getNamespace(),"nearby"),
        //gn_nearbyFeatures(NamespaceEnum.geonames.getNamespace(),"nearbyFeatures"),
        //gn_neighbour(NamespaceEnum.geonames.getNamespace(),"neighbour"),
        //gn_neighbouringFeatures(NamespaceEnum.geonames.getNamespace(),"neighbouringFeatures"),
        gn_wikipediaArticle(NamespaceEnum.geonames.getNamespace(),"wikipediaArticle"),
        gn_featureClass(NamespaceEnum.geonames.getNamespace(),"featureClass"),
        gn_featureCode(NamespaceEnum.geonames.getNamespace(),"featureCode"),
        //gn_tag(NamespaceEnum.geonames.getNamespace(),"tag"),
        gn_alternateName(NamespaceEnum.geonames.getNamespace(),"alternateName"),
        gn_officialName(NamespaceEnum.geonames.getNamespace(),"officialName"),
        gn_name(NamespaceEnum.geonames.getNamespace(),"name"),
        gn_population(NamespaceEnum.geonames.getNamespace(),"population"),
        gn_shortName(NamespaceEnum.geonames.getNamespace(),"shortName"),
        gn_postalCode(NamespaceEnum.geonames.getNamespace(),"postalCode"),
        geo_lat(NamespaceEnum.geo.getNamespace(),"lat"),
        geo_long(NamespaceEnum.geo.getNamespace(),"long"),
        geo_alt(NamespaceEnum.geo.getNamespace(),"alt"),
        skos_notation(NamespaceEnum.skos.getNamespace(),"notation"),
        skos_prefLabel(NamespaceEnum.skos.getNamespace(),"prefLabel"),
        skos_altLabel(NamespaceEnum.skos.getNamespace(),"altLabel"),
        skos_hiddenLabel(NamespaceEnum.skos.getNamespace(),"hiddenLabel"),
        skos_note(NamespaceEnum.skos.getNamespace(),"note"),
        skos_changeNote(NamespaceEnum.skos.getNamespace(),"changeNote"),
        skos_definition(NamespaceEnum.skos.getNamespace(),"definition"),
        skos_editorialNote(NamespaceEnum.skos.getNamespace(),"editorialNote"),
        skos_example(NamespaceEnum.skos.getNamespace(),"example"),
        skos_historyNote(NamespaceEnum.skos.getNamespace(),"historyNote"),
        skos_scopeNote(NamespaceEnum.skos.getNamespace(),"scopeNote"),
        skos_broader(NamespaceEnum.skos.getNamespace(),"broader"),
        skos_narrower(NamespaceEnum.skos.getNamespace(),"narrower"),
        skos_related(NamespaceEnum.skos.getNamespace(),"related"),
        ;
        private String uri;
        Properties(String namespace,String name){
            uri = namespace+name;
        }
        @Override
        public String toString() {
            return uri;
        }
    }
    public GeoNamesIndexer(Dictionary<String, Object> config) throws IllegalArgumentException {
        this.yard = (Yard)config.get(KEY_YARD);
        if(yard == null){
            throw new IllegalArgumentException("Parsed config MUST CONTAIN a Yard. Use the key "+KEY_YARD+" to parse the YardInstance used to store the geonames.org index!");
        } else {
            log.info(String.format("Using Yard %s (id=%s) to index geonames.org",
                    yard.getName(),yard.getId()));
        }
        this.vf = yard.getValueFactory();
        Long startIndex = (Long)config.get(KEY_START_INDEX);
        if(startIndex != null && startIndex > 0l){
            this.startPosition = startIndex;
        } else {
            this.startPosition = 0;
        }
        Integer chunkSize = (Integer)config.get(KEY_CHUNK_SIZE);
        if(chunkSize != null && chunkSize>0){
            this.indexingChunkSize = chunkSize;
        } //else use default value of 1000
        log.info(" ... start indexing at position "+startPosition);
        Boolean indexOntologyState = (Boolean)config.get(KEY_INDEX_ONTOLOGY_STATE);
        if(indexOntologyState != null){
            this.indexOntology = indexOntologyState;
        } else {
            this.indexOntology = false;
        }
        log.info(" ... indexing geonames.org thesaurus="+indexOntologyState);
        this.dataDir = checkFile(KEY_DATA_DIR, config, "/data");
        this.geonamesArchiveFile = checkFile(KEY_GEONAMES_ARCHIVE, dataDir, config,"allCountries.zip");
        this.countryInfoFile = checkFile(KEY_COUNTRY_INFOS, dataDir,config,"countryInfo.txt");
        this.adminCodesFiles = new ArrayList<File>();
        adminCodesFiles.add(checkFile(KEY_ADMIN1_CODES, dataDir, config,"admin1CodesASCII.txt"));
        adminCodesFiles.add(checkFile(KEY_ADMIN2_CODES, dataDir, config,"admin2Codes.txt"));
        if(this.indexOntology){
            this.geonamesOntFile = checkFile(KEY_GEONAMES_ONTOLOGY, dataDir, config,"ontology_v2.2.1.rdf");
        }
        this.hierarchyFile = checkFile(KEY_HIERARCHY, dataDir, config, "hierarchy.zip");
        this.alternateNamesFile = checkFile(KEY_ALTERNATE_NAMES, dataDir, config,"alternateNames.zip");
    }
    /**
     * Create the index based on the parsed configuration
     * @throws IOException On any error while reading one of the configuration files
     * @throws YardException On any error while storing index features within the Yard
     */
    public void index() throws IOException, YardException{
        readAdminCodes();
        readHierarchy();
        readAlternateNames();
        indexGeonames();
        writeCacheBaseConfiguration();
    }
    /**
     * As the last step we need to create the baseMappings configuration
     * needed to used the Index as Entityhub full cache!
     * @throws YardException would be really bad if after successfully indexing
     * about 8 millions of documents we get an error from the yard at the
     * last possible opportunity :(
     */
    private void writeCacheBaseConfiguration() throws YardException {
        FieldMapper baseMapper = new DefaultFieldMapperImpl(ValueConverterFactory.getDefaultInstance());
        log.info("Write BaseMappings for geonames.org Cache");
        log.info(" > Mappings");
        for(String mapping : GeoNamesIndexer.fieldMappings){
            log.info("    - "+mapping);
            baseMapper.addMapping(FieldMappingUtils.parseFieldMapping(mapping));
        }
        CacheUtils.storeBaseMappingsConfiguration(yard, baseMapper);
        log.info(" < completed");
    }
    /**
     * @param config
     */
    private File checkFile(String key,Dictionary<String, Object> config,Object defaultValue) {
        return checkFile(key, null,config, defaultValue);
    }
    private File checkFile(String key,File directory,Dictionary<String, Object> config,Object defaultValue) {
        File testFile;
        Object fileName = config.get(key);
        if(fileName == null){
            if(defaultValue == null){
                throw new IllegalArgumentException("Parsed Config MUST CONTAIN the a reference to the file for key "+key+"!");
            } else {
                fileName = defaultValue;
            }
        }
        if(directory == null){
            testFile = new File(fileName.toString());
        } else {
            testFile = new File(dataDir,fileName.toString());
        }
        if(!testFile.exists()){
            throw new IllegalStateException("File "+fileName+" parsed by key "+key+" does not exist!");
        }
        if(directory == null && !testFile.isDirectory()){
            throw new IllegalStateException("parsed data directory "+fileName+" exists, but is not a directory!");
        }
        if(directory != null && !testFile.isFile()){
            throw new IllegalStateException("parsed data file "+fileName+" exists, but is not a file!");
        }
        if(!testFile.canRead()){
            throw new IllegalStateException("Unable to read File "+fileName+" parsed for key "+key+"!");
        }
        return testFile;
    }

    private void indexGeonames() throws YardException, IOException {
        ZipFile geonamesZipFile;
        try {
            geonamesZipFile = new ZipFile(geonamesArchiveFile);
        } catch (IOException e) {
            //in the init we check if this is a file, exists and we can read ...
            // .. so throw a runtime exception here!
            throw new IllegalArgumentException("Unable to access geonames.org DB Dump file",e);
        }
        for(Enumeration<? extends ZipEntry> e = geonamesZipFile.entries();e.hasMoreElements();){
            ZipEntry entry = e.nextElement();
            if(!entry.isDirectory() && !entry.getName().toLowerCase().startsWith("readme")){
                log.info("add Entry "+entry.getName());
                BufferedReader reader = new BufferedReader(new InputStreamReader(geonamesZipFile.getInputStream(entry), Charset.forName("utf-8")));
                String line;
                int pos = 0;
                int blockPos =0;
                List<Representation> currentBlock = new ArrayList<Representation>(indexingChunkSize);
                long start = System.currentTimeMillis();
                long iStart = start;
                while((line = reader.readLine())!=null){
                    pos++;
                    if(pos>=startPosition){
                        try    {
                            Representation indexedFeature = importFeature(line);
                            //log.info(ModelUtils.getRepresentationInfo(indexedFeature));
                            blockPos++;
                            currentBlock.add(indexedFeature);
                            if(blockPos == indexingChunkSize){
                                yard.store(currentBlock);
                                currentBlock.clear();
                                blockPos = 0;
                            }
                        } catch (RuntimeException e1){
                            log.warn("Exception while processing line "+line,e1);
                            throw e1;
                        } catch (YardException e1){
                            log.warn("YardException while processing lines "+(pos-blockPos)+"-"+(pos),e1);
                            throw e1;
                        }
                        if(pos%10000==0){
                            long now = System.currentTimeMillis();
                            float mean = ((float)(now-start))/(pos-startPosition);
                            float iMean = ((float)(now-iStart))/10000;
                            log.info(pos+" features processed ("+mean+"ms/feature; "+iMean+"ms/feature for the last 10000 features");
                            iStart=System.currentTimeMillis();
                        }
                    } else {
                        //remove alternate labels from the inMemoryMap for the ID to save memory
                        Integer id = Integer.valueOf(line.substring(0, line.indexOf('\t')));
                        featureNames.remove(id);
                    }
                }
                //indexing the remaining documents
                yard.store(currentBlock);
                currentBlock.clear();
                blockPos = 0;
                //the final commit
                long now = System.currentTimeMillis();
                float mean = ((float)(now-start))/(pos-startPosition);
                log.info(pos+" features processed ("+mean+"ms/feature)");
            }
        }
    }
    private Reference getDocRef(String refString){
        Reference ref = indexDocRefs.get(refString);
        if(ref == null){
            ref = yard.getValueFactory().createReference(refString);
            indexDocRefs.put(refString, ref);
        }
        return ref;
    }
    private Collection<Reference> getFeatureReferences(Collection<Integer> ids){
        List<Reference> refs = new ArrayList<Reference>(ids.size());
        for(Integer id : ids){
            if(id != null){
                refs.add(vf.createReference(String.format("%s%s/", geonamesFeatureBase,id)));
            }
        }
        return refs;
    }
    private Representation importFeature(String line){
        Tokenizer t = new Tokenizer(line);
        String id = t.next();
        Integer geoNamesId = Integer.parseInt(id);
        //create a new Doc based on the first Element (geonamesID)
        Representation doc = this.yard.getValueFactory().createRepresentation(String.format("%s%s/", geonamesFeatureBase,id));
        //add the geonames:Feature type
        doc.add(Properties.rdf_type.toString(), getDocRef(Properties.gn_Feature.toString()));
        //add the UTF-8name
        String utf8Label = t.next();
        doc.addNaturalText(Properties.gn_name.toString(),utf8Label);
        //add the ASKII Name as rdfs:label
        String askiiLabel = t.next();
        if(utf8Label == null){
            utf8Label = askiiLabel; //use ASKII label as fallback for the utf8 version
        }
        doc.addNaturalText(Properties.rdfs_label.toString(),utf8Label);
        //alternate Names (alternate names also include Airport codes, postal codes and Wikipedia links!
        t.next(); //consume this Element and use the alternateNames Map instead
        List<FeatureName> alternateNames = featureNames.remove(geoNamesId); //use remove, because we need not need it a 2nd time!
        if(alternateNames != null){
            List<Text> altList = new ArrayList<Text>(alternateNames.size());
            List<Text> officialList = new ArrayList<Text>(alternateNames.size());
            List<String> postalCodes = new ArrayList<String>();
            List<URL> wikipediaLinks = new ArrayList<URL>();
            List<Text> shortNames = new ArrayList<Text>();
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
                doc.add(Properties.gn_alternateName.toString(),altList);
            }
            if(!officialList.isEmpty()){
                doc.add(Properties.gn_officialName.toString(),officialList);
            }
            if(!postalCodes.isEmpty()){
                doc.add(Properties.gn_postalCode.toString(), postalCodes);
            }
            if(!wikipediaLinks.isEmpty()){
                doc.add(Properties.gn_wikipediaArticle.toString(), wikipediaLinks);
            }
            if(!shortNames.isEmpty()){
                doc.add(Properties.gn_shortName.toString(), shortNames);
            }
        }
        //lat
        doc.add(Properties.geo_lat.toString(),new BigDecimal(t.next()));
        //lon
        doc.add(Properties.geo_long.toString(),new BigDecimal(t.next()));
        //featureClass
        String featureClass = String.format("%s%s",NamespaceEnum.geonames,t.next());
        doc.add(Properties.gn_featureClass.toString(),getDocRef(featureClass));
        //featureCode (-> need to use <featureClass>.<featureCode>!!)
        doc.add(Properties.gn_featureCode.toString(),getDocRef(String.format("%s.%s",featureClass,t.next())));
        //countryCode
        //  -> geonames uses here the link to an HTML Page showing the Country
        //     We would like to use an Link to a SKOS:Concept representing the Country
        // ... But luckily here we need only to add the URI!
        Set<String> ccs = new HashSet<String>();
        String countryCode = t.next();
        if(countryCode != null){
            countryCode = countryCode.trim(); //need to trim because some country codes use '  ' to indicate null!
            if(countryCode.length() == 2){ //Yes there are some features that are in no country!
                ccs.add(countryCode);
            }
        }
        //alternate countryCodes
        String altCc = t.next();
        if(altCc != null){
            StringTokenizer altCcT = new StringTokenizer(altCc,",");
            while(altCcT.hasMoreElements()){
                countryCode = altCcT.nextToken();
                if(countryCode.length() ==2){
                    ccs.add(countryCode);
                }
            }
        }
        if(!ccs.isEmpty()){
            doc.add(Properties.gn_countryCode.toString(),ccs);
        }
        //admin Codes 1-4
        //first read them -> we need to consume the tokens anyway
        String[] adminCodes = new String[] {
            countryCode, //country
            t.next(), //ADM1
            t.next(), //ADM2
            t.next(), //ADM3
            t.next()};//ADM4
        //Workaround for Admin1 -> add leading '0' for single Value
        if(adminCodes[1] != null && adminCodes[1].length() < 2){
            adminCodes[1] = '0'+adminCodes[1];
        }
        addParents(doc,geoNamesId,adminCodes);

        //population
        String populationString = t.next();
        if(populationString != null){
            //NOTE: we need to used Long, because of Asia (3.800.000)
            Long population = new Long(populationString);
            if(population.intValue() > 0){
                doc.add(Properties.gn_population.toString(),population);
            }
        }
        //elevation
        String altString = t.next();
        if(altString == null){
            altString = t.next(); //if no elevation than use the gtopo30
        } else {
            t.next(); //if there is already en elevation, than consume these entry
        }
        Integer alt = Integer.valueOf(altString);
        if(alt.intValue() > -9999){ //it looks like that -9999 is sometimes used as not known!
            doc.add(Properties.geo_alt.toString(),alt);
        }
        //time zone
        t.next(); //not used
        //mod-date
        String modDateString = t.next();
        if(modDateString != null){
            try {
                doc.add(Properties.dc_date.toString(),TimeUtils.toDate(DataTypeEnum.DateTime, modDateString));
            }catch (IllegalArgumentException e) {
                log.warn(String.format("Unable to parse modificationDate for geonamesID %s from value %s",doc.getId(),modDateString));
            }
        }
        //and add geonames.org as the creator!
        doc.add(Properties.dc_creator.toString(),"http://www.geonames.org/");
        return doc;
    }

    private void addParents(Representation doc,Integer id,String[] adminCodes){
        Integer[] adminIds = new Integer[5];
        //now process the admin Codes (including the country at index 0)
        for(int i=0;i<adminCodes.length;i++){
            if(adminCodes[i] != null && !adminCodes[i].equals("00")){ //00 is used to indicate not known
                StringBuilder parentCode = new StringBuilder();
                for(int j=0;j<i;j++){
                    parentCode.append(adminCodes[j]); //add all the previous
                    parentCode.append('.'); //add the seperator char
                }
                parentCode.append(adminCodes[i]);//add the current (last) Element
                adminIds[i] =adminCode2featureId.get(parentCode.toString()); //might also add null!
            }
        }
        //now get the direct parents
        Map<Integer,Collection<Integer>> parents = new HashMap<Integer, Collection<Integer>>();
        getParents(id,parents);
        //add all parents
        doc.add(Properties.gn_parentFeature.toString(), getFeatureReferences(parents.keySet()));
        //get admin hierarchy

        Set<Integer> parentLevel;
        //add country
        if(adminIds[0]!=null){
            doc.add(Properties.gn_parentCountry.toString(), vf.createReference(geonamesFeatureBase+adminIds[0]));
            parentLevel = Collections.singleton(adminIds[0]);
        }  else {
            parentLevel = Collections.emptySet();
        }
        //add the admin codes for the 4 levels
        parentLevel = addAdminLevel(doc, Properties.gn_parentADM1, parents, parentLevel, adminIds[1]);
        parentLevel = addAdminLevel(doc, Properties.gn_parentADM2, parents, parentLevel, adminIds[2]);
        parentLevel = addAdminLevel(doc, Properties.gn_parentADM3, parents, parentLevel, adminIds[3]);
        parentLevel = addAdminLevel(doc, Properties.gn_parentADM4, parents, parentLevel, adminIds[4]);
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
    private Set<Integer> addAdminLevel(Representation doc,Properties property, Map<Integer,Collection<Integer>> parents,Set<Integer> parentLevel, Integer adminId){
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
                log.warn(String.format("Multiple %s for ID %s (ids: %s)",property.name(),doc.getId(),currentLevel.toString()));
            }
        }
        return currentLevel;
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

    private int readCountryInfos() throws IOException{
        BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(countryInfoFile), Charset.forName("utf-8")));
        String line;
        int lineCount = 0;
        while((line = reader.readLine()) != null){
            if(line.indexOf('#')!=0 && line.length()>0){ //# is used as comment
                Tokenizer t = new Tokenizer(line);
                String code = null;
                Integer geonamesId = null;
                int i=1;
                for(;t.hasNext();i++){
                    String actToken = t.next();
                    if(i==1){
                        code = actToken;
                    }
                    if(i==countryGeonamesIdPos){
                        geonamesId = Integer.valueOf(actToken);
                        break;
                    }
                }
                if(i==countryGeonamesIdPos){
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
    /**
     * This Method loads the alternate labels of geonames.org. Such labels are
     * used for multiple language support but also include postal codes, links
     * to wikipedia, airport codes ... see {@link NameType} for details.
     * TODO: This loads a lot of stuff into memory. Maybe one should consider to
     * use some caching framework like OSCache. Features are anyway sorted by
     * Country so often used labels would be in memory and all the labels that
     * are only used once can be serialised to the cache if in low memory
     * environments!
     * @throws IOException
     */
    private void readAlternateNames() throws IOException{
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
            if(entry ==null){
                throw new IllegalStateException("Archive with alternate Names does not contain the \"alternateNames.txt\" file!");
            } else {
                log.info("read alternate names from Archive Entry "+entry.getName());
                reader = new BufferedReader(new InputStreamReader(alternateNamesArchive.getInputStream(entry), Charset.forName("utf-8")));
            }
        } else {
            reader = new BufferedReader(new InputStreamReader(new FileInputStream(alternateNamesFile), Charset.forName("utf-8")));
        }
        FeatureName name;
        int lineCount = 0;
        EnumMap<NameType, int[]> labelTypeCounts = new EnumMap<NameType, int[]>(NameType.class);
        for(NameType entry :NameType.values()){
            labelTypeCounts.put(entry, new int[]{0});
        }
        String line;
        long start = System.currentTimeMillis();
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
        log.info("read "+lineCount+" alternate Names for "+featureNames.size()+" Features in "+(System.currentTimeMillis()-start)+"ms");
        for(Entry<NameType, int[]> count : labelTypeCounts.entrySet()){
            log.info("   "+count.getKey().toString()+": "+count.getValue()[0]);
        }
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
            reader = new BufferedReader(new InputStreamReader(new FileInputStream(alternateNamesFile), Charset.forName("utf-8")));
        }
        String line;
        int lineCount=0;
        long start = System.currentTimeMillis();
        while((line = reader.readLine()) != null){
            lineCount++;
            Tokenizer t = new Tokenizer(line);
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

//    private static void indexGeonamesOntology() throws RepositoryException, RDFParseException, IOException, SemanticSearchProviderException{
//        Repository ontRepository = new SailRepository(new MemoryStore());
//        ontRepository.initialize();
//        RepositoryConnection con = ontRepository.getConnection();
//        File geonamesOnt = new File(GeoNamesIndexer.geonamesOntFile);
//        System.out.println("Geonames Ontology: ");
//        System.out.println("  > name   : "+geonamesOnt.getAbsolutePath());
//        System.out.println("  > exists : "+geonamesOnt.exists());
//        System.out.println("  > isFile : "+geonamesOnt.isFile());
//        //add the geonames Ont to the Repository
//        con.add(geonamesOnt, geonamesOntBase, RDFFormat.RDFXML);
//        RepositoryResult<Statement> results = con.getStatements(null, org.openrdf.model.vocabulary.RDF.TYPE, null, false);
//        Map<Resource,IndexInputDocument> geonamesOntResources = new HashMap<Resource,IndexInputDocument>();
//        log.info("Process Ontology:");
//        for(Statement stm: results.asList()){
//            log.debug(" Statement : "+stm.getSubject());
//            //check for contains to avaoid multiple processing if a resource has two types
//            if(!geonamesOntResources.containsKey(stm.getSubject())){
//                log.info(" > "+stm.getSubject());
//                geonamesOntResources.put(stm.getSubject(), getResourceValues(manager.getPathRegistry(), con, stm));
//            }
//        }
//        log.info("Index Geonames Ontology ("+geonamesOntResources.size()+" Resources)");
//        manager.getIndexProvider().indexDocuments(geonamesOntResources.values());
//        con.close();
//        con = null;
//
//    }
//    private static IndexInputDocument getResourceValues(PathRegistry pathRegistry,
//            RepositoryConnection con, Statement stm) throws RepositoryException {
//        IndexInputDocument inputDoc = new IndexInputDocument(stm.getSubject().stringValue());
//        RepositoryResult<Statement> designValues = con.getStatements(stm.getSubject(),null,null,false);
//        for(Statement value: designValues.asList()){
//            log.debug("   "+value.getPredicate()+"="+value.getObject());
//            PathElement pathElement = pathRegistry.getPathElement(value.getPredicate().stringValue());
//            //in the geonames Data the lat/lon/alt are not marked with the dataType
//            // -> therefore try to parse the dataType from the String value!
//            inputDoc.add(pathElement, value.getObject());
//        }
//        debugInputDoc(inputDoc);
//        return inputDoc;
//    }

    public static final class FeatureName{
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
        private static final String TRUE = "1";
        protected FeatureName(String line){
            Tokenizer t = new Tokenizer(line);
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
    public static class Tokenizer implements Iterator<String>{
        private static final String DELIM ="\t";
        private final StringTokenizer t;
        private boolean prevElementWasNull = true;
        public Tokenizer(String data){
            t = new StringTokenizer(data, DELIM, true);
        }
        @Override
       public boolean hasNext() {
            return t.hasMoreTokens();
        }

        @Override
        public String next() {
            if(!prevElementWasNull){
                t.nextElement();//dump the delim
            }
            if(!t.hasMoreElements()){
                //this indicated, that the current Element is
                // - the last Element
                // - and is null
                prevElementWasNull = true;
                return null;
            } else {
                String act = t.nextToken();
                if(DELIM.equals(act)){
                    prevElementWasNull = true;
                    return null;
                } else {
                    prevElementWasNull = false;
                    return act;
                }
            }
        }
        @Override
        public void remove() {
            throw new UnsupportedOperationException();
        }
    }
}
