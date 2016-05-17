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
package org.apache.stanbol.entityhub.indexing.source.vcard;

import static org.apache.stanbol.entityhub.indexing.source.vcard.OntologyMappings.ADR_COUNTRY;
import static org.apache.stanbol.entityhub.indexing.source.vcard.OntologyMappings.ADR_EXTENDED;
import static org.apache.stanbol.entityhub.indexing.source.vcard.OntologyMappings.ADR_LOCALITY;
import static org.apache.stanbol.entityhub.indexing.source.vcard.OntologyMappings.ADR_POSTAL_CODE;
import static org.apache.stanbol.entityhub.indexing.source.vcard.OntologyMappings.ADR_POST_OFFICE_ADDRESS;
import static org.apache.stanbol.entityhub.indexing.source.vcard.OntologyMappings.ADR_REGION;
import static org.apache.stanbol.entityhub.indexing.source.vcard.OntologyMappings.ADR_STREET;
import static org.apache.stanbol.entityhub.indexing.source.vcard.OntologyMappings.N_ADDITIONAL;
import static org.apache.stanbol.entityhub.indexing.source.vcard.OntologyMappings.N_FAMILY;
import static org.apache.stanbol.entityhub.indexing.source.vcard.OntologyMappings.N_GIVEN;
import static org.apache.stanbol.entityhub.indexing.source.vcard.OntologyMappings.N_PREFIX;
import static org.apache.stanbol.entityhub.indexing.source.vcard.OntologyMappings.N_SUFFIX;
import static org.apache.stanbol.entityhub.indexing.source.vcard.OntologyMappings.RDF_TYPE;
import static org.apache.stanbol.entityhub.indexing.source.vcard.OntologyMappings.VCARD_ORGANIZATION;
import static org.apache.stanbol.entityhub.indexing.source.vcard.OntologyMappings.VCARD_PERSON;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;

import net.fortuna.ical4j.data.ParserException;
import net.fortuna.ical4j.vcard.Parameter;
import net.fortuna.ical4j.vcard.Property;
import net.fortuna.ical4j.vcard.VCard;
import net.fortuna.ical4j.vcard.VCardBuilder;
import net.fortuna.ical4j.vcard.VCardFileFilter;
import net.fortuna.ical4j.vcard.property.Address;
import net.fortuna.ical4j.vcard.property.N;
import net.fortuna.ical4j.vcard.property.Org;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.stanbol.entityhub.core.model.InMemoryValueFactory;
import org.apache.stanbol.entityhub.indexing.core.EntityDataIterable;
import org.apache.stanbol.entityhub.indexing.core.EntityDataIterator;
import org.apache.stanbol.entityhub.indexing.core.config.IndexingConfig;
import org.apache.stanbol.entityhub.indexing.core.source.ResourceImporter;
import org.apache.stanbol.entityhub.indexing.core.source.ResourceLoader;
import org.apache.stanbol.entityhub.indexing.core.source.ResourceState;
import org.apache.stanbol.entityhub.indexing.source.vcard.OntologyMappings.Mapping;
import org.apache.stanbol.entityhub.servicesapi.defaults.NamespaceEnum;
import org.apache.stanbol.entityhub.servicesapi.model.Representation;
import org.apache.stanbol.entityhub.servicesapi.model.ValueFactory;
import org.apache.stanbol.entityhub.servicesapi.util.ModelUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.emory.mathcs.backport.java.util.Arrays;
import edu.emory.mathcs.backport.java.util.Collections;

public class VcardIndexingSource implements EntityDataIterable, ResourceImporter {
    
    protected static Logger log = LoggerFactory.getLogger(VcardIndexingSource.class);

    /**
     * The prefix used to create Entities
     */
    private String prefix;

    private char typeSeperatorChar = '/';
    
    private ResourceLoader loader;
    /**
     * The charset used to read the vcard file(s) in the source folder
     */
    private Charset charset = null;
    /**
     * The default Charset ("utf-8"). This is also used to write the vcard files
     * within the destination directory.
     */
    public static final Charset DEFAULT_CHARSET = Charset.forName("UTF8");
    /**
     * Parameter that allows users to define the encoding of the vcard files
     * to import (the {@link #DEFAULT_CHARSET default encoding} is set to
     * "utf-8"
     */
    public static final String PARAM_CHARSET = "encoding";
    /**
     * The Parameter used to configure the source folder(s) relative to the
     * {@link IndexingConfig#getSourceFolder()}. The ',' (comma) is used as
     * separator to parsed multiple sources.
     */
    public static final String PARAM_SOURCE_FILE_OR_FOLDER = "source";
    /**
     * The default directory name used to search for vcard files to be imported
     */
    public static final String DEFAULT_SOURCE_FOLDER_NAME = "vcard";
    /**
     * The prefix used vCard entities
     */
    public static final String PARAM_PREFIX = "prefix";

    /**
     * Used to import vcard files from the 
     * {@link IndexingConfig#getSourceFolder() source}/
     * {@link #PARAM_SOURCE_FILE_OR_FOLDER vcard} folder.
     */
    protected ResourceImporter importer;
    /**
     * Folder within the destination directory to temporary copy all the
     * vCard files to import.
     */
    private File vcardFileImportFolder;
    /**
     * List of the files that need to be imported. Initialised in {@link #initialise()}
     */
    @SuppressWarnings("unchecked")
    private List<File> vcardFiles = Collections.emptyList();
    /**
     * Used to create {@link Representation} instances
     */
    private ValueFactory vf = InMemoryValueFactory.getInstance();
    /**
     * The vcard -&gt; ontology mappings
     * TODO make configurable as soon as there are multiple mappings available
     */
    private Map<String,Mapping> mappings = OntologyMappings.schemaOrgMappings;
    
    public VcardIndexingSource() {
        //set relaxed parsing to TRUE
        System.setProperty("ical4j.parsing.relaxed", Boolean.TRUE.toString());
    }
    @Override
    public EntityDataIterator entityDataIterator() {
        return new VCardIterator();
    }

    @Override
    public void close() {
        this.importer = null;
    }

    @Override
    public boolean needsInitialisation() {
        //if there are resources with the state REGISTERED we need an initialisation
        return !loader.getResources(ResourceState.REGISTERED).isEmpty();
    }
    @Override
    public void initialise(){
        //this will call #importResource(..) for all files in the directories
        //configured by the #PARAM_SOURCE_FILE_OR_FOLDER
        loader.loadResources();
        //create the lists 
        vcardFiles = Arrays.asList(vcardFileImportFolder.listFiles(
            (FilenameFilter)VCardFileFilter.INSTANCE));
    }

    @Override
    public void setConfiguration(Map<String,Object> config) {
        //init fields
        IndexingConfig indexingConfig = (IndexingConfig)config.get(IndexingConfig.KEY_INDEXING_CONFIG);
        loader = new ResourceLoader(this, true, false);
        //vcard files are imported from a special folder in the destination dir.
        //this folder needs to be deleted/(re-)created first.
        vcardFileImportFolder = new File(indexingConfig.getDestinationFolder(),"vcard");
        if(vcardFileImportFolder.exists()){
            if(vcardFileImportFolder.isDirectory()){
                try {
                    FileUtils.deleteDirectory(vcardFileImportFolder);
                }catch (IOException e){
                    throw new IllegalStateException("Unable to delete Folder "+
                        vcardFileImportFolder.getAbsolutePath()+" containing the vCard files from a" +
                        		"previouse indexing! Please remove this folder manually.",e);
                }
            } else if(!vcardFileImportFolder.delete()){
                throw new IllegalStateException("Unable to delete File "+
                    vcardFileImportFolder.getAbsolutePath()+" containing the vCard data from a" +
                            "previouse indexing! Please remove this File manually.");
            }
        }
        if(!vcardFileImportFolder.mkdirs()){
            throw new IllegalStateException("Unable to delete Folder "+
                vcardFileImportFolder.getAbsolutePath()+" containing the vCard files from a" +
                        "previouse indexing! Please remove this folder manually.");
        }
        //load config
        Object value;
        log.debug("load vcard resources from :");
        value = config.get(PARAM_SOURCE_FILE_OR_FOLDER);
        if(value == null){ //if not set use the default
            value = DEFAULT_SOURCE_FOLDER_NAME;
        }
        for(String source : value.toString().split(",")){
            File sourceFileOrDirectory = indexingConfig.getSourceFile(source);
            if(sourceFileOrDirectory.exists()){
                //register the configured source with the ResourceLoader
                this.loader.addResource(sourceFileOrDirectory);
            } else {
                if(FilenameUtils.getExtension(source).isEmpty()){
                    //non existent directory -> create
                    //This is typically the case if this method is called to
                    //initialise the default configuration. So we will try
                    //to create the directory users need to copy the source
                    //RDF files.
                    if(!sourceFileOrDirectory.mkdirs()){
                        log.warn("Unable to create directory {} configured to improt source data from. " +
                                "You will need to create this directory manually before copying the" +
                                "Source files into it.",sourceFileOrDirectory);
                        //this would not be necessary because the directory will
                        //be empty - however I like to be consistent and have
                        //all configured and existent files & dirs added the the
                        //resource loader
                        this.loader.addResource(sourceFileOrDirectory);
                    }
                } else {
                    log.warn("Unable to find vcard source {} within the indexing Source folder ",source,indexingConfig.getSourceFolder());
                }
            }
        }
        if(log.isDebugEnabled()){
            for(String registeredSource : loader.getResources(ResourceState.REGISTERED)){
                log.debug(" > "+registeredSource);
            }
        }
        //parse the encoding
        value = config.get(PARAM_CHARSET);
        if(value != null){
            String encoding = value.toString();
            if(encoding.isEmpty()){ //use plattform encoding if empty
                charset = Charset.defaultCharset();
            } else {
                try {
                    charset = Charset.forName(encoding);
                } catch (RuntimeException e) {
                    throw new IllegalStateException("The configured encoding '"+
                        encoding+"' is not supported by this Plattform", e);
                }
            }
        } else { //use plattorm encoding if missing
            charset = Charset.defaultCharset();
        }
        //parse the prefix
        value = config.get(PARAM_PREFIX);
        if(value == null || value.toString().isEmpty()){
            throw new IllegalStateException("Teh configuration is missing the required parameter 'prefix'!");
        } else {
            prefix = value.toString();
            //set the typeSeperatorChar based on the kind of parsed prefix
            if(prefix.endsWith("#")){
                typeSeperatorChar = '.';
            } else if (prefix.endsWith("/")){
                typeSeperatorChar = '/';
            } else if (prefix.endsWith(":")){
                typeSeperatorChar = ':';
            } else if (prefix.startsWith("urn:")){ //maybe an urn without an tailing ':'
                prefix = prefix+':';
                typeSeperatorChar = ':';
            } else if (prefix.indexOf("://")>0){ //maybe an url without an tailing '/' or '#'
                prefix = prefix+'/';
            } //else ... no idea what kind of prefix ... use the default '/'
        }
    }

    /**
     * This only copies vCard files to the {@link #vcardFileImportFolder} within the
     * {@link IndexingConfig#getDestinationFolder()}.<p>
     * In addition if a specific {@link #charset} is configured for the
     * vcard files to import this also changes the encoding to the
     * {@link #DEFAULT_CHARSET} (utf-8). This can help users to investigate and
     * correct file encoding related issues.
     * @see org.apache.stanbol.entityhub.indexing.core.source.ResourceImporter#importResource(java.io.InputStream, java.lang.String)
     */
    @Override
    public ResourceState importResource(InputStream is, String resourceName) throws IOException {
        //only copies the file to tmp files in the
        if(resourceName.charAt(0) != '.' && VCardFileFilter.INSTANCE.accept(new File(resourceName))){
            //copy the file to the destination directory
            //1. get the file name used in the destination
            String name = FilenameUtils.getName(resourceName);
            String baseName = FilenameUtils.getBaseName(name);
            String extension = FilenameUtils.getExtension(name);
            File outFile = new File(vcardFileImportFolder,name);
            for(int i = 0;outFile.exists();i++){
                outFile = new File(vcardFileImportFolder,
                    String.format("%s_%s.%s",baseName,i,extension));
            }
            //check the encoding to ensure that in the destination all files use
            // DEFAULT_CHARSET (utf-8)
            if(charset == null || charset.equals(DEFAULT_CHARSET)){
                // no recoding -> copy bytes
                OutputStream os = new FileOutputStream(outFile);
                IOUtils.copy(is, os);
                IOUtils.closeQuietly(os);
                IOUtils.closeQuietly(is);
            } else { //recode
                Reader r = new InputStreamReader(is, charset);
                Writer w = new OutputStreamWriter(new FileOutputStream(outFile), DEFAULT_CHARSET);
                IOUtils.copy(r, w);
                IOUtils.closeQuietly(r);
                IOUtils.closeQuietly(w);
            }
            return ResourceState.LOADED;
        } else {
            log.debug("RDFTerm {} ignored: Not an Vcard file.",resourceName);
            return ResourceState.IGNORED;
        }
    }
    
    private final class VCardIterator implements EntityDataIterator {
        Map<EntityType,Map<String,Set<String>>> entityMap;
        Iterator<File> files = vcardFiles.iterator(); 
        @SuppressWarnings("unchecked")
        Iterator<VCard> vcards = Collections.emptyList().iterator();
        @SuppressWarnings("unchecked")
        Iterator<Representation> representations = Collections.emptyList().iterator();
        Representation nextRepresentation = null;
        Representation currentRepresentation = null;

        private VCardIterator(){
            entityMap = new EnumMap<EntityType,Map<String,Set<String>>>(EntityType.class);
            entityMap.put(EntityType.organization, new HashMap<String,Set<String>>());
            entityMap.put(EntityType.person,  new HashMap<String,Set<String>>());
        }
        /**
         * Parses all {@link VCard} object of the next {@link #files file};
         */
        private Iterator<VCard> parseNext(File file){
            Reader r;
            try {
                r = new InputStreamReader(new FileInputStream(file), DEFAULT_CHARSET);
            } catch (FileNotFoundException e) {
                throw new IllegalStateException("vcard import file "+file+
                    "not found - maybe deleted during import?",e);
            }
            VCardBuilder parser = new VCardBuilder(r);
            try {
                return parser.buildAll().iterator();
            } catch (IOException e) {
                throw new IllegalStateException("Unable to read vcard file "+file,e);
            } catch (ParserException e) {
                throw new IllegalStateException("Unable to parse vcard file "+file,e);
            }
        }

        @Override
        public Representation getRepresentation() {
            return currentRepresentation;
        }

        @Override
        public boolean hasNext() {
            //Iterate while there are still representations, vCards or files
            while(nextRepresentation == null && (representations.hasNext() || 
                    vcards.hasNext() || files.hasNext())) {
                if(representations.hasNext()){ //if more representations
                    nextRepresentation = representations.next(); //set next
                } else { //else process the next vCard object
                    VCard nextVcard = null;
                    //Iterate while there are still more vCards or files
                    while(nextVcard == null && (vcards.hasNext() || files.hasNext())){
                        if(vcards.hasNext()){ //if there are more vCards                
                            nextVcard = vcards.next(); //get next
                        } else { //parse the next file
                            //NOTE: we do not need to check for file.hasNext,
                            //because this was already implicitly checked by the
                            //outer most while loop
                            vcards = parseNext(files.next());
                        }
                    }
                    if(nextVcard != null){
                        representations = processVcard(nextVcard,mappings,entityMap);
                    }
                }
            }
            return nextRepresentation != null;
        }

        @Override
        public String next() {
            if(nextRepresentation == null && 
                    !hasNext()){ //try to get the next
                throw new NoSuchElementException();
            }
            currentRepresentation = nextRepresentation;
            nextRepresentation = null;
            return currentRepresentation.getId();
        }

        @Override
        public void remove() {
            throw new UnsupportedOperationException("removal is not supported");
            
        }
        @SuppressWarnings("unchecked")
        @Override
        public void close() {
            //set to empty iterators instead of null. Otherwise I would need
            //to check for null in all the other methods
            files = Collections.emptyList().iterator();
            representations = Collections.emptyList().iterator();
            vcards = Collections.emptyList().iterator();
            nextRepresentation = null;
            currentRepresentation = null;
        }
        
    }
    /**
     * Vcard objects can represent persons (FN is defined) or organisations
     * (no 'FN' but an 'ORG' element)
     * @author Rupert Westenthaler
     *
     */
    private enum EntityType {person,organization}
    /**
     * Converts a vCard object to Representations.
     * @param vCard the vCard object to process
     * @param mappings the Mappings to use
     * @param entityMap the Map holding the ids of already processed vCards. This
     * is used to avoid id conflicts
     * @return Iterator over the processed Representation
     */
    protected Iterator<Representation> processVcard(VCard vCard,Map<String,Mapping> mappings,
        Map<EntityType,Map<String,Set<String>>> entityMap){
        //NOTE: this is protected to allow direct access from the VCardIterator
        String name = null;
        EntityType entityType = null;
        Property nameProperty = vCard.getProperty(Property.Id.FN);
        if(nameProperty != null && nameProperty.getValue() != null && !nameProperty.getValue().isEmpty()){
            entityType = EntityType.person;
            name = nameProperty.getValue();
        } else { //FN name -> maybe a ORG was exported
            Property orgProperty = vCard.getProperty(Property.Id.ORG);
            if(orgProperty != null && ((Org)orgProperty).getValues() != null && ((Org)orgProperty).getValues().length>0){
                entityType = EntityType.organization;
                name = ((Org)orgProperty).getValues()[0];
            }
        }
        if(entityType == null){
            log.warn("Unable to index vCard object without values for FN or ORG parameter (vCard: {})",vCard);
            return Collections.emptyList().iterator();
        }
        String id = null;
        Property uid = vCard.getProperty(Property.Id.UID);
        if(uid != null){
            id = uid.getValue();
        } else {
            id = name;
        }
        id = entityByName(entityMap, entityType, name, id,true);
        
        //we have a name and an id (local name of the URI/URN)
        // ... now parse the vCard
        Representation rep = vf .createRepresentation(
            id);
        Map<String,Representation> representations = new HashMap<String,Representation>();
        representations.put(rep.getId(), rep);
        //add the type
        Mapping typeMapping = mappings.get(
            entityType == EntityType.person ? VCARD_PERSON : VCARD_ORGANIZATION);
        if(typeMapping != null){
            rep.add(NamespaceEnum.rdf+"type", typeMapping.uri);
        }
        log.debug("vCard [type: {} | name: '{}' | id: '{}']",
            new Object[]{entityType,name,rep.getId()});
        for(Property property : vCard.getProperties()){
            Property.Id propertyId = property.getId();
            String propName = propertyId.getPropertyName();
            if(mappings.containsKey(propName)){ //there is a mapping for this property
              //the Representation to write the Information of the current Property
                Representation current;
                //the Map with the mappings to be used for processing the current
                //Property
                Map<String,Mapping> currentMappings;
                Mapping mapping = mappings.get(propName); //May be null!!
                if(mapping == null || mapping.subMappings == null){
                    current = rep; //add to the base Representation
                    currentMappings = mappings; //and use the parsed mappings
                } else {
                    current = null; //indicates we need to create a new Representation
                    currentMappings = mapping.subMappings; //and use the sub mappings
                }
                switch (propertyId) {
                    case N:
                        N n = (N)property;
                        String given = n.getGivenName();
                        String family = n.getFamilyName();
                        if((given == null || given.isEmpty()) && (family == null
                                || family.isEmpty())){
                            log.warn("'N' property '{}'does not define given nor family name -> ignored",
                                n.getValue());
                        } else {
                            if(current == null){ //create new Representation
                                current = createSubRepresentation(rep, ".name", 
                                    representations.keySet(), mapping);
                                representations.put(current.getId(), current);
                            }
                            Mapping subPropertyMapping = currentMappings.get(N_GIVEN);
                            if(subPropertyMapping != null && given != null && !given.isEmpty()){
                                current.addNaturalText(subPropertyMapping.uri, StringUtils.chomp(given).trim());
                            }
                            subPropertyMapping = currentMappings.get(N_FAMILY);
                            if(subPropertyMapping != null & family != null && !family.isEmpty()){
                                current.addNaturalText(subPropertyMapping.uri, StringUtils.chomp(family).trim());
                            }
                            String[] additional = n.getAdditionalNames();
                            subPropertyMapping = currentMappings.get(N_ADDITIONAL);
                            if(subPropertyMapping != null & additional != null && additional.length>0){
                                for(String value : additional){
                                    if(value != null && !value.isEmpty()){
                                        current.addNaturalText(subPropertyMapping.uri, StringUtils.chomp(value).trim());
                                    }
                                }
                            }
                            String[] prefixes = n.getPrefixes();
                            subPropertyMapping = currentMappings.get(N_PREFIX);
                            if(subPropertyMapping != null & prefixes != null && prefixes.length>0){
                                for(String value : prefixes){
                                    if(value != null && !value.isEmpty()){
                                        current.addNaturalText(subPropertyMapping.uri, StringUtils.chomp(value).trim());
                                    }
                                }
                            }
                            String[] suffixes = n.getSuffixes();
                            subPropertyMapping = currentMappings.get(N_SUFFIX);
                            if(subPropertyMapping != null & suffixes != null && suffixes.length>0){
                                for(String value : suffixes){
                                    if(value != null && !value.isEmpty()){
                                        current.addNaturalText(subPropertyMapping.uri, StringUtils.chomp(value).trim());
                                    }
                                }
                            }
                        }
                        break;
                    case ADR:
                        Address address = (Address)property;
                        if(address.getValue() != null &&
                                //check of the value does not only contain seperators (',')
                                !address.getValue().replace(';', ' ').trim().isEmpty()){
                            if(current == null){ //create new Representation
                                current = createSubRepresentation(rep, ".adr", 
                                    representations.keySet(), mapping);
                                representations.put(current.getId(), current);
                            }
                            Mapping subPropertyMapping = currentMappings.get(ADR_POST_OFFICE_ADDRESS);
                            String value = address.getPoBox();
                            if(subPropertyMapping != null && value != null && !value.isEmpty()){
                                //add string -> this is no natural language text
                                current.add(subPropertyMapping.uri, StringUtils.chomp(value).trim());
                            }
                            value = address.getExtended();
                            subPropertyMapping = currentMappings.get(ADR_EXTENDED);
                            if(subPropertyMapping != null && value != null && !value.isEmpty()){
                                current.addNaturalText(subPropertyMapping.uri, StringUtils.chomp(value).trim());
                            }
                            value = address.getStreet();
                            subPropertyMapping = currentMappings.get(ADR_STREET);
                            if(subPropertyMapping != null && value != null && !value.isEmpty()){
                                current.addNaturalText(subPropertyMapping.uri, StringUtils.chomp(value).trim());
                            }
                            value = address.getLocality();
                            subPropertyMapping = currentMappings.get(ADR_LOCALITY);
                            if(subPropertyMapping != null && value != null && !value.isEmpty()){
                                current.addNaturalText(subPropertyMapping.uri, StringUtils.chomp(value).trim());
                            }
                            value = address.getRegion();
                            subPropertyMapping = currentMappings.get(ADR_REGION);
                            if(subPropertyMapping != null && value != null && !value.isEmpty()){
                                current.addNaturalText(subPropertyMapping.uri, StringUtils.chomp(value).trim());
                            }
                            value = address.getPostcode();
                            subPropertyMapping = currentMappings.get(ADR_POSTAL_CODE);
                            if(subPropertyMapping != null && value != null && !value.isEmpty()){
                                // add string -> this is no natural language text
                                current.add(subPropertyMapping.uri, StringUtils.chomp(value).trim());
                            }
                            value = address.getCountry();
                            subPropertyMapping = currentMappings.get(ADR_COUNTRY);
                            if(subPropertyMapping != null && value != null && !value.isEmpty()){
                                // add string -> based on the standard this should be the two letter code
                                current.add(subPropertyMapping.uri, StringUtils.chomp(value).trim());
                            }
                            
                        } //else empty ADR field -> ignore
                        break;
                    case ORG:
                        Org org = (Org)property;
                        String[] unitHierarchy = org.getValues();
                        Mapping orgNameMapping = currentMappings.get(OntologyMappings.ORG_NAME);
                        if(unitHierarchy.length>0 && orgNameMapping != null &&
                                unitHierarchy[0] != null && unitHierarchy[0].trim().length()>0){
                            String orgName = unitHierarchy[0];
                            if(current == null){ //create new Representation for the Organisation
                                //Note: this is an Entity and no sub-RDFTerm!
                                String orgEntityId = entityByName(entityMap, EntityType.organization, 
                                    orgName, null, false);
                                if(orgEntityId == null){
                                    //create new Entity for this Organization
                                    orgEntityId = entityByName(entityMap, EntityType.organization, 
                                        orgName, null, true);
                                    current = vf.createRepresentation(orgEntityId);
                                    initSubRepresentation(current, rep, mapping);
                                    representations.put(current.getId(), current);
                                    current.addNaturalText(orgNameMapping.uri, StringUtils.chomp(orgName).trim());
                                    //TODO: inverse relation form the ORG to the
                                    // Person can not be supported without caching
                                    // organisations. Therefore delete this relation for now
                                    if(mapping.invUri != null){
                                        current.removeAll(mapping.invUri);
                                    }
                                    //TODO: Organisation units are not supported
                                } else {
                                    rep.addReference(mapping.uri, orgEntityId);
                                }
                            }
                        }
                        break;
                    default:
                        if(current != null && mapping != null){
                            String value = property.getValue();
                            if(value != null){
                                value = StringUtils.chomp(property.getValue()).trim();
                            }
                            if(value.isEmpty()){
                                log.warn("Unable to index empty value for property {} of vCard {}",
                                    property.getId().getPropertyName(),rep.getId());
                            } else {
                                current.addNaturalText(mapping.uri, value);
                            }
                        } else if(mapping != null){
                            log.warn("Sub-Resources are not supported for Property {} (mapping to {} ignored)!",
                                propName,mapping);
                        } //else no mapping defined
                        break;
                }
                String value = property.getValue();
                log.debug(" - {}: {}",propertyId.getPropertyName(),value);
                for(Parameter param : property.getParameters()){
                    Parameter.Id paramId = param.getId();
                    String paramValue = param.getValue();
                    log.debug("   {}:{}",paramId.getPname(),paramValue);
                }
            } else {
                log.debug("No mapping for Property {} with value {}",propertyId,property.getValue());
            }
        }
        log.debug(" > Mapped Data;");
        if(log.isDebugEnabled()){
            for(Representation tmp : representations.values()){
                log.info(ModelUtils.getRepresentationInfo(tmp));
            }
        }
        log.debug("--- end ---");
        return representations.values().iterator();
    }

    /**
     * @param entityMap the map with all the Entity name -&gt; id mappings
     * @param entityType the type of the entity to search
     * @param name the name of the Entity
     * @param id optionally an id other than the name otherwise the name is used
     * @param create if <code>true</code> is parsed a new Entity is created even
     * if a entity with the same name already exists
     * @return the id of the created or found Entity
     */
    private String entityByName(Map<EntityType,Map<String,Set<String>>> entityMap,
                                EntityType entityType,
                                String name,
                                String id,
                                boolean create) {
        if(id == null) {
            id = name;
        }
        //lookup the existing entities of that type and name
        Set<String> entities = entityMap.get(entityType).get(name);
        if(entities == null){ //if none -> we will create one in this method
            entities = new HashSet<String>(2); //use lower size to save memory
            entityMap.get(entityType).put(name, entities);
        }   
        //make ids only to use ASKII chars and no white spaces
        id = id.replace(' ', '-');
        try { // encode special chars
            //TODO: replace that by ASKII folding
            id = URLEncoder.encode(id, "utf8");
        } catch (UnsupportedEncodingException e) {
            throw new IllegalStateException("This Plattform does not support 'utf8' encoding :(");
        }
        //add prefixes and so on
        id = prefix+entityType+typeSeperatorChar+id;
        //now we have the id
        if(!create){
            //NOTE: this would always return the first Entity if multiple Entities
            // would have been created by using the ceckId method. 
            return entities.contains(id) ? id : null;
        } else { //we need to create a new entity
            id = checkId(id, entities);
            entities.add(id);
            return id;
        }
    }

    /**
     * Create a sub-representation by considering the base {@link Representation},
     * IDs already taken by other sub representations. The Id addon the caller
     * would like to add to the id of the base representation. In addition it
     * adds the relation between the base and the sub-representation as well as
     * the type and the inverse links to the sub-representation.
     * @param base the base (parent) representation
     * @param addon the string addon to the id of the base
     * @param takenIds set of IDs that are already taken
     * @param mapping the mapping used to get the information needed to correctly
     * initialise the sub-relation
     */
    private Representation createSubRepresentation(Representation base, String addon, Set<String> takenIds, Mapping mapping) {
        Representation current =  vf.createRepresentation(
            checkId(base.getId()+addon, takenIds));
        initSubRepresentation(current, base, mapping);
        return current;

    }

    /**
     * Initialise the parsed sub-representation by adds the relation between 
     * the base and the sub-representation as well as
     * the rdf:type of the sub-relation and the inverse link if the sub- to the
     * base representation.
     * @param toInit The representation to initialise
     * @param base the parent representation
     * @param mapping the mapping 
     */
    private void initSubRepresentation(Representation toInit, Representation base, Mapping mapping) {
        Mapping typeMapping = mapping.subMappings.get(RDF_TYPE);
        if(typeMapping != null){
            toInit.addReference(NamespaceEnum.rdf+"type", typeMapping.uri);
        }
        base.addReference(mapping.uri, toInit.getId());
        if(mapping.invUri != null){
            toInit.addReference(mapping.invUri, base.getId());
        }
    }

    /**
     * Adds "-{i}" to the end of the parsed ID until it does no longer conflict
     * with already taken IDs
     * @param id the id
     * @param taken already taken IDs
     * @return a id based on the parsed one that does not conflict with already
     * taken once. 
     */
    private String checkId(String id, Set<String> taken) {
        String test = null;
        int i=0;
        while(taken.contains(i == 0 ? id : test)){
            i++;
            test = id+'-'+i;
        }
        if(test != null){
            id = test;
        }
        return id;
    }
    public static void main(String[] args) throws Exception {
     VcardIndexingSource instance = new VcardIndexingSource();
     
     instance.prefix = "http://test.org/";
     VCardBuilder parser = new VCardBuilder(new InputStreamReader(new FileInputStream(new File(args[0])), "utf8"));
     Map<EntityType,Map<String,Set<String>>> entityMap = new EnumMap<EntityType,Map<String,Set<String>>>(EntityType.class);
     entityMap.put(EntityType.organization, new HashMap<String,Set<String>>());
     entityMap.put(EntityType.person,  new HashMap<String,Set<String>>());
     for(VCard vcard : parser.buildAll()){
         instance.processVcard(vcard,OntologyMappings.schemaOrgMappings,entityMap);
     }
    }
    
}
