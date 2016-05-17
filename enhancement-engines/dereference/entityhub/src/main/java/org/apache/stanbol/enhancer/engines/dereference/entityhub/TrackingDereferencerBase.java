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
package org.apache.stanbol.enhancer.engines.dereference.entityhub;

import java.io.StringReader;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.locks.Lock;

import org.apache.clerezza.commons.rdf.Language;
import org.apache.clerezza.commons.rdf.Graph;
import org.apache.clerezza.commons.rdf.IRI;
import org.apache.clerezza.commons.rdf.impl.utils.simple.SimpleGraph;
import org.apache.commons.lang.StringUtils;
import org.apache.marmotta.ldpath.api.backend.RDFBackend;
import org.apache.marmotta.ldpath.exception.LDPathParseException;
import org.apache.marmotta.ldpath.model.programs.Program;
import org.apache.stanbol.commons.namespaceprefix.NamespacePrefixService;
import org.apache.stanbol.enhancer.engines.dereference.DereferenceConstants;
import org.apache.stanbol.enhancer.engines.dereference.DereferenceContext;
import org.apache.stanbol.enhancer.engines.dereference.DereferenceException;
import org.apache.stanbol.enhancer.engines.dereference.EntityDereferencer;
import org.apache.stanbol.entityhub.core.mapping.DefaultFieldMapperImpl;
import org.apache.stanbol.entityhub.core.mapping.FieldMappingUtils;
import org.apache.stanbol.entityhub.core.mapping.ValueConverterFactory;
import org.apache.stanbol.entityhub.ldpath.EntityhubLDPath;
import org.apache.stanbol.entityhub.model.clerezza.RdfReference;
import org.apache.stanbol.entityhub.model.clerezza.RdfRepresentation;
import org.apache.stanbol.entityhub.model.clerezza.RdfValueFactory;
import org.apache.stanbol.entityhub.servicesapi.EntityhubException;
import org.apache.stanbol.entityhub.servicesapi.mapping.FieldMapper;
import org.apache.stanbol.entityhub.servicesapi.mapping.FieldMapping;
import org.apache.stanbol.entityhub.servicesapi.model.Representation;
import org.apache.stanbol.entityhub.servicesapi.model.Text;
import org.apache.stanbol.entityhub.servicesapi.model.ValueFactory;
import org.apache.stanbol.entityhub.servicesapi.query.TextConstraint;
import org.apache.stanbol.entityhub.servicesapi.util.ModelUtils;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Filter;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.util.tracker.ServiceTracker;
import org.osgi.util.tracker.ServiceTrackerCustomizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Abstract super class for EntityDereferencer that need to track the OSGI service
 * used to lookup Entities. Used by the {@link EntityhubDereferencer} and the 
 * {@link SiteDereferencer} implementation
 * @author Rupert Westenthaler
 *
 */
public abstract class TrackingDereferencerBase<T> implements EntityDereferencer {
    
    private final Logger log = LoggerFactory.getLogger(getClass());
    
    private ServiceTracker searchServiceTracker;
    protected BundleContext bundleContext; 

    protected final RdfValueFactory valueFactory = RdfValueFactory.getInstance();

    protected Set<String> dereferencedFields;
    private FieldMapper fieldMapper;

    private NamespacePrefixService nsPrefixService;

    private Program<Object> ldpathProgram;
    /**
     * Caches the {@link RDFBackend} for the last instance returned by
     * {@link #getService()}.
     */
    private Map<T,RDFBackend<Object>> rdfBackendCache = new IdentityHashMap<T,RDFBackend<Object>>();

    private final Class<T> serviceClass;

	private ExecutorServiceProvider executorServiceProvider;
    /**
     * Creates a new instance for the parsed parameter
     * @param context the BundleContexed used to create the {@link ServiceTracker}
     * listening for the SearchService
     * @param serviceClass
     * @param filterEntries
     */
    protected TrackingDereferencerBase(BundleContext context, Class<T> serviceClass,
            Map<String,String> filterEntries, ServiceTrackerCustomizer customizer,
            ExecutorServiceProvider executorServiceProvider){
        this.bundleContext = context;
        this.serviceClass = serviceClass;
        this.executorServiceProvider = executorServiceProvider;
        //the fieldMapper allows to configure users fields that should be dereferenced
        if(filterEntries == null || filterEntries.isEmpty()){
            searchServiceTracker = new ServiceTracker(context, serviceClass.getName(), customizer);
        } else {
            StringBuffer filterString = new StringBuffer();
            filterString.append(String.format("(&(objectclass=%s)",serviceClass.getName()));
            for(Entry<String,String> filterEntry : filterEntries.entrySet()){
                if(filterEntry.getKey() != null && !filterEntry.getKey().isEmpty() &&
                    filterEntry.getValue() != null && !filterEntry.getValue().isEmpty()){
                    filterString.append(String.format("(%s=%s)",
                        filterEntry.getKey(),filterEntry.getValue()));
                } else {
                    throw new IllegalArgumentException("Illegal filterEntry "+filterEntry+". Both key and value MUST NOT be NULL nor emtpty!");
                }
            }
            filterString.append(')');
            Filter filter;
            try {
                filter = context.createFilter(filterString.toString());
            } catch (InvalidSyntaxException e) {
                throw new IllegalArgumentException(String.format(
                    "Unable to build Filter for '%s' (class=%s,filter=%s)", 
                    filterString,serviceClass,filterEntries),e);
            }
            searchServiceTracker = new ServiceTracker(context, filter, customizer);
        }
    }
    /**
     * Setter for the {@link NamespacePrefixService}
     * @param nsPrefixService
     */
    public void setNsPrefixService(NamespacePrefixService nsPrefixService) {
        this.nsPrefixService = nsPrefixService;
    }
    /**
     * Getter for the {@link NamespacePrefixService}
     * @return
     */
    public NamespacePrefixService getNsPrefixService() {
        return nsPrefixService;
    }
    /**
     * Setter for the dereferenced fields
     * @param dereferencedFields the set containing the fields that need to be
     * dereferenced. If <code>null</code> or an empty set all fields will be
     * dereferenced.
     */
    public void setDereferencedFields(List<String> dereferencedFields) {
        if(dereferencedFields != null && !dereferencedFields.isEmpty()){
            List<FieldMapping> mappings = new ArrayList<FieldMapping>(dereferencedFields.size());
            log.debug(" > parse configured field mappings");
            for(String configuredMapping : dereferencedFields){
            	log.trace(" - parse configure mapping '{}'",configuredMapping);
                FieldMapping mapping = FieldMappingUtils.parseFieldMapping(configuredMapping,nsPrefixService);
                if(mapping != null){
                    log.debug("   - add FieldMapping {}",mapping);
                    mappings.add(mapping);
                } else if(configuredMapping != null && !configuredMapping.isEmpty()){
                    log.warn("   - unable to parse FieldMapping '{}'", configuredMapping);
                }
            }
            if(!mappings.isEmpty()){
                log.debug(" > apply {} valid mappings",mappings.size());
                fieldMapper = new DefaultFieldMapperImpl(ValueConverterFactory.getDefaultInstance());
                for(FieldMapping mapping : mappings){
                	fieldMapper.addMapping(mapping);
                }
            } else { //no valid mapping parsed
            	log.debug(" > no valid mapping parsed ... will dereference all fields");
            	fieldMapper = null;
            }
        } else {
            fieldMapper = null;
        }
    }
    /**
     * Setter for the LDPath program used for dereferencing Entities
     * @param ldpathProgramStr the LDPath program as String
     * @throws ConfigurationException if parsing the LDPath program fails
     */
    public void setLdPath(String ldpathProgramStr) throws ConfigurationException {
        if(ldpathProgramStr == null || StringUtils.isBlank(ldpathProgramStr)){
            ldpathProgram = null;
        } else { //validate the parsed LDPath program
            //when this method is called the real RDFBackend will not be available.
            //however we would like to parse/validate the parsed LDPath program
            //So we will create a pseudo RDFBackend sufficient to be used with the
            //parser
            final RDFBackend<Object> parseBackend = new ParseBackend<T>(valueFactory);
            //NOTE: calling execute(..) an this parseLdPath or even the 
            //ldpathProgram will result in UnsupportedOperationException
            //but parsing is OK
            EntityhubLDPath parseLdPath = new EntityhubLDPath(parseBackend, valueFactory);
            try {
                ldpathProgram = parseLdPath.parseProgram(new StringReader(ldpathProgramStr));
            } catch (LDPathParseException e) {
                log.error("Unable to parse LDPath pogram: \n {}", ldpathProgramStr);
                throw new ConfigurationException(DereferenceConstants.DEREFERENCE_ENTITIES_LDPATH, 
                    "Unable to parse configured LDPath program ", e);
            }
            //finally validate if all mappings of the programm do use a URI as key
            for(org.apache.marmotta.ldpath.model.fields.FieldMapping<?,Object> mapping : ldpathProgram.getFields()) {
                try {
                    new URI(mapping.getFieldName());
                } catch (URISyntaxException e){
                    throw new ConfigurationException(DereferenceConstants.DEREFERENCE_ENTITIES_LDPATH, 
                        "Parsed LDPath MUST use valid URIs as field names (invalid field name: '"
                        + mapping.getFieldName()+"' | selector: '" 
                        + mapping.getSelector().getPathExpression(parseBackend)+"')!");
                }
            }
        }
    }
    /**
     * Getter for the set of dereferenced fields
     * @return the dereferenced fields or an empty set if all fields are
     * dereferenced.
     */
    public Set<String> getDereferencedFields() {
        return dereferencedFields;
    }
        
    /**
     * Getter for the FieldMapper used for the {@link #getDereferencedFields()}
     * @return the fieldMapper or <code>null</code> of the dereferenced fields
     * are set
     */
    public FieldMapper getFieldMapper(){
        return fieldMapper;
    }
    
    /**
     * Getter for the LDPath {@link Program} parsed form the
     * {@link #getLdPath}
     * @return
     */
    public Program<Object> getLdPathProgram(){
        return ldpathProgram;
    }
    
    public ValueFactory getValueFactory(){
        return valueFactory;
    }
    
    /**
     * Starts the tracking by calling {@link ServiceTracker#open()}
     */
    public void open(){
        searchServiceTracker.open();
    }
    /**
     * Getter for the Service used to search for Entities. If the service is
     * currently not available, than this method will return <code>null</code>
     * @return The service of <code>null</code> if not available
     */
    @SuppressWarnings("unchecked") //type is ensured by OSGI
    protected T getService(){
        if(searchServiceTracker == null){
            throw new IllegalStateException("This TrackingEntitySearcher is already closed!");
        } else {
            return (T) searchServiceTracker.getService();
        }
    }
    
    @Override
    public final ExecutorService getExecutor() {
    	return executorServiceProvider == null ? null : executorServiceProvider.getExecutorService();
    }
    
    @Override
    public final boolean dereference(IRI uri, Graph graph, Lock writeLock, DereferenceContext dc) throws DereferenceException {
        T service = getService();
        if(service == null){
            throw new DereferenceException(uri, serviceClass.getClass().getSimpleName() 
                + "service is currently not available");
        }
        EntityhubDereferenceContext derefContext = (EntityhubDereferenceContext)dc;
        Representation rep;
        try {
            rep = getRepresentation(service, uri.getUnicodeString(), derefContext.isOfflineMode());
        } catch(EntityhubException e){
            throw new DereferenceException(uri,e);
        }
        //we need the languages as strings
        final Set<String> langs = derefContext.getLanguages();
        final FieldMapper fieldMapper = derefContext.getFieldMapper();
        final Program<Object>ldpathProgram = derefContext.getProgram();
        if(rep != null){
            if(fieldMapper == null && ldpathProgram == null && 
                    (langs == null || langs.isEmpty())){
                copyAll(uri, rep, graph, writeLock);
            } else { //we need to apply some filters while dereferencing
                if(fieldMapper != null || (langs != null && !langs.isEmpty())){
                    //this considers speficied fields and included languages
                    copyMapped(uri, rep, fieldMapper, langs, graph, writeLock);
                }
                if(ldpathProgram != null){
                    //this executes LDPath statements
                    copyLdPath(uri, getRdfBackend(service), ldpathProgram, langs, graph, writeLock);
                }
            }
            return true;
        } else {
            return false;
        }
    }
    /**
     * Executes the {@link #ldpathProgram} using the parsed URI as context and
     * writes the the results to the parsed ImmutableGraph
     * @param uri the context
     * @param rdfBackend the RdfBackend the LDPath program is executed on
     * @param ldpathProgram The {@link Program} parsed via the dereference context
     * @param langs the set of languages to dereference
     * @param graph the graph to store the results
     * @param writeLock the write lock for the graph
     * @throws DereferenceException on any {@link EntityhubException} while
     * executing the LDPath program
     */
    private void copyLdPath(IRI uri, RDFBackend<Object> rdfBackend, Program<Object> ldpathProgram,
            Set<String> langs, Graph graph, Lock writeLock) throws DereferenceException {
        //A RdfReference needs to be used as context
        RdfReference context = valueFactory.createReference(uri);
        //create the representation that stores results in an intermediate
        //graph (we do not want partial results on an error
        Graph ldPathResults = new SimpleGraph();
        RdfRepresentation result = valueFactory.createRdfRepresentation(uri, ldPathResults);
        //execute the LDPath Program and write results to the RDF ImmutableGraph
        try {
	        for(org.apache.marmotta.ldpath.model.fields.FieldMapping<?,Object> mapping : ldpathProgram.getFields()) {
	        	Collection<?> values;
	        	try {
	        		values = mapping.getValues(rdfBackend, context);
	            } catch (RuntimeException e){
	                throw new DereferenceException(uri, e);
	            }
	            if(values != null && !values.isEmpty()){
	                String fieldName = mapping.getFieldName();
	                if(langs == null || langs.isEmpty()){
	                    result.add(fieldName,values);
	                } else { //filter for languages
	                    for(Object value : values){
	                        if((!(value instanceof Text)) || 
	                                langs.contains(((Text)value).getLanguage())){
	                            result.add(fieldName, value);
	                        } //else text with filtered language ... do not add
	                    }
	                }
	            }
	        }
        } catch (EntityhubException e){
            throw new DereferenceException(uri, e);
        }
       	if(log.isTraceEnabled()){
    		log.trace("dereferenced via LDPath {}", ModelUtils.getRepresentationInfo(result));
    	}

        if(!ldPathResults.isEmpty()){ //copy the results
            writeLock.lock();
            try {
                graph.addAll(ldPathResults);
            } finally {
                writeLock.unlock();
            }
        }
    }
    /**
     * Getter for the {@link RDFBackend} for the parsed service. This tries to
     * get the backend from {@link #rdfBackendCache}. If it is not yet created
     * {@link #createRdfBackend(Object)} is called.
     * @param service The Service to get the {@link RDFBackend} for.
     * @return the {@link RDFBackend}.
     */
    protected final RDFBackend<Object> getRdfBackend(T service) {
        RDFBackend<Object> rdfBackend = rdfBackendCache.get(service);
        if(rdfBackend == null){
            rdfBackend = createRdfBackend(service);
            rdfBackendCache.clear(); //cache only a single service
            rdfBackendCache.put(service, rdfBackend);
        }
        return rdfBackend;
    }
    
    /**
     * Applies the field mappings to the representation and stores the results
     * in the graph
     * @param uri the uri of the entity to dereference
     * @param rep the data for the entity as in the entityhub
     * @param fieldMapper the {@link FieldMapper} parsed from the dereference context
     * @param langs the set of languages to dereference
     * @param graph the graph to store the mapping results
     * @param writeLock the write lock for the graph
     */
    private void copyMapped(IRI uri, Representation rep, FieldMapper fieldMapper, Set<String> langs, 
            Graph graph, Lock writeLock) {
        //NOTE: The fieldMapper parsed via the context does already have a
        //      filter for the parsed languages. Because of that the old code
        //      adding such a language filter is no longer needed
//        FieldMapper fieldMapper;
//        if(!langs.isEmpty()){ //if we need to filter for specific languages
//            fieldMapper = this.fieldMapper != null ? this.fieldMapper.clone() :
//                new DefaultFieldMapperImpl(ValueConverterFactory.getDefaultInstance());
//            fieldMapper.addMapping(new FieldMapping(new TextConstraint(
//                (String)null, langs.toArray(new String[langs.size()]))));
//        } else { //just use the fieldMapper as parsed in the config
//            fieldMapper = this.fieldMapper;
//        }
        //execute the field mappings
        writeLock.lock();
        try {
            RdfRepresentation clerezzaRep = valueFactory.createRdfRepresentation(uri, graph);
            fieldMapper.applyMappings(rep, clerezzaRep, valueFactory);
           	if(log.isTraceEnabled()){
        		log.trace("dereferenced via Mappings {}", ModelUtils.getRepresentationInfo(clerezzaRep));
        	}
        } finally {
            writeLock.unlock();
        }
    }
    /**
     * Copies all data form the representation to the graph. This is used
     * if no dereference rules are defined
     * @param uri the uri of the entity to copy
     * @param rep the {@link Representation} with the data of the entity
     * @param graph the graph to copy the data
     * @param writeLock the write lock for the graph
     */
    private void copyAll(IRI uri, Representation rep, Graph graph, Lock writeLock) {
        writeLock.lock();
        try {
        	if(log.isTraceEnabled()){
        		log.trace("dereferenced all of {}", ModelUtils.getRepresentationInfo(rep));
        	}
            if(rep instanceof RdfRepresentation){
                graph.addAll(((RdfRepresentation)rep).getRdfGraph());
            } else {
                RdfRepresentation clerezzaRep = valueFactory.createRdfRepresentation(uri,graph);
                //convert all values for all fields
                for (Iterator<String> fields = rep.getFieldNames(); fields.hasNext();) {
                    String field = fields.next();
                    for (Iterator<Object> fieldValues = rep.get(field); fieldValues.hasNext();) {
                        clerezzaRep.add(field, fieldValues.next());
                    }
                }
            }
        } finally {
            writeLock.unlock();
        }
    }
    /**
     * provides the Representation for the parsed id
     * @param id the id
     * @param offlineMode off line mode state
     * @return the Representation or <code>null</code> if not found
     * @throws DereferenceException 
     */
    protected abstract Representation getRepresentation(T service, String id, boolean offlineMode) throws EntityhubException;
    /**
     * Creates an RDFBackend for the parsed service
     * @param service
     * @return
     */
    protected abstract RDFBackend<Object> createRdfBackend(T service);
    /**
     * Closes the {@link ServiceTracker} used to track the service.
     */
    public void close(){
        searchServiceTracker.close();
        searchServiceTracker = null;
        bundleContext = null;
    }
    @Override
    protected void finalize() throws Throwable {
        close();
        super.finalize();
    }
    
}
 