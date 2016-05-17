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

import static org.apache.stanbol.entityhub.indexing.geonames.GeonamesConstants.GEONAMES_ONTOLOGY_NS;
import static org.apache.stanbol.entityhub.indexing.geonames.GeonamesConstants.GEONAMES_RESOURCE_NS;
import static org.apache.stanbol.entityhub.indexing.geonames.GeonamesConstants.getReference;
import static org.apache.stanbol.entityhub.indexing.geonames.GeonamesConstants.valueFactory;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.StringTokenizer;

import org.apache.commons.compress.archivers.zip.ZipArchiveInputStream;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.LineIterator;
import org.apache.stanbol.commons.namespaceprefix.NamespacePrefixService;
import org.apache.stanbol.entityhub.core.mapping.DefaultFieldMapperImpl;
import org.apache.stanbol.entityhub.core.mapping.FieldMappingUtils;
import org.apache.stanbol.entityhub.core.mapping.ValueConverterFactory;
import org.apache.stanbol.entityhub.core.site.CacheUtils;
import org.apache.stanbol.entityhub.core.utils.TimeUtils;
import org.apache.stanbol.entityhub.indexing.core.EntityDataIterable;
import org.apache.stanbol.entityhub.indexing.core.EntityDataIterator;
import org.apache.stanbol.entityhub.indexing.core.config.IndexingConfig;
import org.apache.stanbol.entityhub.indexing.core.source.ResourceImporter;
import org.apache.stanbol.entityhub.indexing.core.source.ResourceLoader;
import org.apache.stanbol.entityhub.indexing.core.source.ResourceState;
import org.apache.stanbol.entityhub.servicesapi.defaults.DataTypeEnum;
import org.apache.stanbol.entityhub.servicesapi.mapping.FieldMapper;
import org.apache.stanbol.entityhub.servicesapi.model.Representation;
import org.apache.stanbol.entityhub.servicesapi.yard.YardException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GeonamesIndexingSource implements EntityDataIterable, ResourceImporter {
    
    private static final Logger log = LoggerFactory.getLogger(GeonamesIndexingSource.class);
    /**
     * The Parameter used to configure the source folder(s) relative to the
     * {@link IndexingConfig#getSourceFolder()}. The ',' (comma) is used as
     * separator to parsed multiple sources.
     */
    public static final String PARAM_SOURCE_FILE_OR_FOLDER = "source";
    /**
     * The zip archive with the geonames entities
     */
    public static final String GEONAMES_DUMP = "allCountries.zip";

    private IndexingConfig indexingConfig;
    private NamespacePrefixService nsPrefixService;
    private File dataDir;
    
    private ResourceLoader loader = new ResourceLoader(this, false, false);

    protected static class RDFTerm {
        protected final String name;
        protected final InputStream is;
        protected RDFTerm(String name, InputStream is) {
            this.name = name;
            this.is = is;
        }
        
        public String getName() {
            return name;
        }
        
        public LineIterator getEntries() throws IOException {
            if(name.endsWith(".zip")){
                ZipArchiveInputStream zipIn = new ZipArchiveInputStream(is);
                zipIn.getNextEntry();
                return IOUtils.lineIterator(zipIn, "UTF-8");
            } else {
                return IOUtils.lineIterator(is, "UTF-8");
            }
        }
        
    }
    private List<RDFTerm> resourceList = new ArrayList<GeonamesIndexingSource.RDFTerm>();
    private boolean consumed;
    
    @Override
    public void setConfiguration(Map<String,Object> config) {
        indexingConfig = (IndexingConfig)config.get(IndexingConfig.KEY_INDEXING_CONFIG);
        nsPrefixService = indexingConfig.getNamespacePrefixService();
        log.info("reading Geonames data from:");
        Object value = config.get(PARAM_SOURCE_FILE_OR_FOLDER);
        if(value == null){ //if not set use the default
            value = GeonamesConstants.DEFAULT_SOURCE_FOLDER_NAME + GEONAMES_DUMP;
            log.info("No Geonames.org dump source set use the default: {}",value);
        }
        log.info("Source File(s): ");
        for(String source : value.toString().split(",")){
            File sourceFileOrDirectory = indexingConfig.getSourceFile(source);
            if(sourceFileOrDirectory.exists()){
                //register the configured source with the ResourceLoader
                log.info(" > {}",sourceFileOrDirectory.getName());
                this.loader.addResource(sourceFileOrDirectory);
            } else {
                if(FilenameUtils.getExtension(source).isEmpty()){
                    //non existent directory -> create
                    //This is typically the case if this method is called to
                    //initialise the default configuration. So we will try
                    //to create the directory users need to copy the source
                    //RDF files.
                    if(!sourceFileOrDirectory.mkdirs()){
                        log.warn("Unable to create directory {} configured to improt geonames.org data from. " +
                                "You will need to create this directory manually before copying the" +
                                "Geonames files into it.",sourceFileOrDirectory);
                        //this would not be necessary because the directory will
                        //be empty - however I like to be consistent and have
                        //all configured and existent files & dirs added the the
                        //resource loader
                        this.loader.addResource(sourceFileOrDirectory);
                    }
                } else {
                    log.warn("Unable to find RDF source {} within the indexing Source folder ",source,indexingConfig.getSourceFolder());
                }
            }
        }
     }

    @Override
    public boolean needsInitialisation() {
        //if there are resources with the state REGISTERED we need an initialisation
        return !loader.getResources(ResourceState.REGISTERED).isEmpty();
    }

    @Override
    public void initialise() {
        loader.loadResources();
    }

    @Override
    public void close() {
        loader = null;
        for(RDFTerm resource : resourceList){
            IOUtils.closeQuietly(resource.is);
        }
    }

    @Override
    public ResourceState importResource(InputStream is, String resourceName) throws IOException {
        resourceList.add(new RDFTerm(resourceName, is));
        return ResourceState.LOADED;
    }

    
    @Override
    public EntityDataIterator entityDataIterator() {
        if(!consumed){
            consumed = true;
        } else {
            throw new IllegalStateException("This implementation supports only a"
                    + "single Iteration of the data.");
        }
        return new EntityDataIterator() {
            
            Iterator<RDFTerm> resources = resourceList.iterator();
            RDFTerm r;
            LineIterator it = null;
            private String next;
            private Representation rep;
            
            
            private String getNext(){
                while((it == null || !it.hasNext()) && resources != null && resources.hasNext()){
                    if(r != null){
                        IOUtils.closeQuietly(r.is);
                    }
                    r = resources.next();
                    try {
                        it = r.getEntries();
                    } catch (IOException e) {
                        log.error("Unable to read RDFTerm '"+r.getName()+"' because of "+e.getMessage(),e);
                        e.printStackTrace();
                        IOUtils.closeQuietly(r.is);
                        it = null;
                    }
                    resources.remove();
                }
                if(it != null && it.hasNext()){
                    return it.nextLine();
                } else {
                    return null;
                }
            }
            
            @Override
            public void remove() {
                throw new UnsupportedOperationException();
            }
            
            @Override
            public String next() {
                if(next == null){
                    next = getNext();
                }
                if(next == null){
                    throw new NoSuchElementException();
                } else {
                    rep = processGeonameEntry(next);
                    next = null;
                    return rep.getId();
                }
            }
            
            @Override
            public boolean hasNext() {
                if(next == null){
                    next = getNext();
                }
                return next != null;
            }
            
            @Override
            public Representation getRepresentation() {
                return rep;
            }
            
            @Override
            public void close() {
                if(r != null){
                    IOUtils.closeQuietly(r.is);
                }
                next = null;
                it = null;
                resources = null;
            }
            /**
             * Parses the Representation from the current line.<p>
             * NOTE: this does not process alternate labels and also does not
             * lookup entities for parent codes. Those things are done now by
             * own EntityProcessors
             * @param line the line to process
             * @return the representation
             */
            private Representation processGeonameEntry(String line){
                LineTokenizer t = new LineTokenizer(line);
                //[0] geonames id
                String id = t.next();
                Integer geoNamesId = Integer.parseInt(id);
                //create a new Doc based on the first Element (geonamesID)
                Representation doc = valueFactory.createRepresentation(
                    new StringBuilder(GEONAMES_RESOURCE_NS).append(id).append('/').toString());
                //add the Integer id so that we do not need to parse it from the subject URI
                doc.add(GeonamesPropertyEnum.idx_id.toString(), geoNamesId);
                //add the geonames:Feature type
                doc.add(GeonamesPropertyEnum.rdf_type.toString(), getReference(GeonamesPropertyEnum.gn_Feature.toString()));
                //[1] UTF-8 name
                String utf8Label = t.next();
                //[2] ASKII Name as rdfs:label
                String askiiLabel = t.next();
                if(utf8Label == null){
                    utf8Label = askiiLabel; //use ASKII label as fallback for the utf8 version
                }
                doc.addNaturalText(GeonamesPropertyEnum.gn_name.toString(),utf8Label);
                //[3] Alternate Names
                t.next(); //alternate names are added later during processing
                //addAlternateNames(geoNamesId, doc);
                //[4] lat
                doc.add(GeonamesPropertyEnum.geo_lat.toString(),new BigDecimal(t.next()));
                //[5] lon
                doc.add(GeonamesPropertyEnum.geo_long.toString(),new BigDecimal(t.next()));
                //[6] featureClass
                String featureClass = new StringBuilder(GEONAMES_ONTOLOGY_NS).append(t.next()).toString();
                doc.add(GeonamesPropertyEnum.gn_featureClass.toString(),getReference(featureClass));
                //[7] featureCode (-> need to use <featureClass>.<featureCode>!!)
                doc.add(GeonamesPropertyEnum.gn_featureCode.toString(),getReference(
                    new StringBuilder(featureClass).append('.').append(t.next()).toString()));
                //countryCode
                //  -> geonames uses here the link to an HTML Page showing the Country
                //     We would like to use an Link to a SKOS:Concept representing the Country
                // ... But luckily here we need only to add the URI!
                Set<String> ccs = new HashSet<String>();
                //[8] countryCode
                String countryCode = t.next();
                if(countryCode != null){
                    countryCode = countryCode.trim(); //need to trim because some country codes use '  ' to indicate null!
                    if(countryCode.length() == 2){ //Yes there are some features that are in no country!
                        ccs.add(countryCode);
                    }
                }
                //[9] alternate countryCodes
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
                    doc.add(GeonamesPropertyEnum.gn_countryCode.toString(),ccs);
                }
                //[10 TO 13] Admin codes
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
                //now process the admin Codes (including the country at index 0)
                StringBuilder parentCode = new StringBuilder();
                //iterate over parent codes until the first NULL (or '00' unknown) element
                for(int i=0;i<adminCodes.length && adminCodes[i] != null && !adminCodes[i].equals("00") ;i++){
                    if(i>0){
                        parentCode.append('.');
                    }
                    parentCode.append(adminCodes[i]);//add the current (last) Element
                    String property = i==0 ? GeonamesPropertyEnum.idx_CC.toString() :
                        new StringBuilder(GeonamesPropertyEnum.idx_ADM.toString()).append(i).toString();
                    doc.add(property, parentCode.toString()); // add each level
                }

                //[14] population
                String populationString = t.next();
                if(populationString != null){
                    //NOTE: we need to used Long, because of Asia (3.800.000)
                    Long population = new Long(populationString);
                    if(population.intValue() > 0){
                        doc.add(GeonamesPropertyEnum.gn_population.toString(),population);
                    }
                }
                
                //[15 TO 16] elevation and gtopo30
                String altString = t.next();
                if(altString == null){
                    altString = t.next(); //if no elevation than use the gtopo30
                } else {
                    t.next(); //if there is already en elevation, than consume these entry
                }
                Integer alt = Integer.valueOf(altString);
                if(alt.intValue() > -9999){ //it looks like that -9999 is sometimes used as not known!
                    doc.add(GeonamesPropertyEnum.geo_alt.toString(),alt);
                }
                
                //[17] time zone
                t.next(); //not used
                //[18] mod-date
                String modDateString = t.next();
                if(modDateString != null){
                    try {
                        doc.add(GeonamesPropertyEnum.dc_date.toString(),TimeUtils.toDate(DataTypeEnum.DateTime, modDateString));
                    }catch (IllegalArgumentException e) {
                        log.warn(String.format("Unable to parse modificationDate for geonamesID %s from value %s",doc.getId(),modDateString));
                    }
                }
                //no creator as this is anyway provided by attribution
                //doc.add(GeonamesPropertyEnum.dc_creator.toString(),"http://www.geonames.org/");
                return doc;
            }
        };
    }
}
