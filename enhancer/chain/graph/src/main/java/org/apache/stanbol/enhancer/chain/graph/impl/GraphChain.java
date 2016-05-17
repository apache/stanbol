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
package org.apache.stanbol.enhancer.chain.graph.impl;

import static org.apache.commons.io.FilenameUtils.getExtension;
import static org.apache.stanbol.enhancer.servicesapi.helper.ConfigUtils.getEnhancementProperties;
import static org.apache.stanbol.enhancer.servicesapi.helper.ConfigUtils.getParameters;
import static org.apache.stanbol.enhancer.servicesapi.helper.ConfigUtils.getState;
import static org.apache.stanbol.enhancer.servicesapi.helper.ConfigUtils.getValue;
import static org.apache.stanbol.enhancer.servicesapi.helper.ConfigUtils.guessRdfFormat;
import static org.apache.stanbol.enhancer.servicesapi.helper.ConfigUtils.parseConfig;
import static org.apache.stanbol.enhancer.servicesapi.helper.ExecutionPlanHelper.createExecutionPlan;
import static org.apache.stanbol.enhancer.servicesapi.helper.ExecutionPlanHelper.validateExecutionPlan;
import static org.apache.stanbol.enhancer.servicesapi.helper.ExecutionPlanHelper.writeExecutionNode;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.clerezza.commons.rdf.ImmutableGraph;
import org.apache.clerezza.commons.rdf.Graph;
import org.apache.clerezza.commons.rdf.BlankNodeOrIRI;
import org.apache.clerezza.commons.rdf.impl.utils.simple.SimpleGraph;
import org.apache.clerezza.commons.rdf.impl.utils.TripleImpl;
import org.apache.clerezza.rdf.core.serializedform.Parser;
import org.apache.commons.io.IOUtils;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.ConfigurationPolicy;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.Service;
import org.apache.stanbol.commons.stanboltools.datafileprovider.DataFileListener;
import org.apache.stanbol.commons.stanboltools.datafileprovider.DataFileProvider;
import org.apache.stanbol.commons.stanboltools.datafileprovider.DataFileTracker;
import org.apache.stanbol.enhancer.servicesapi.Chain;
import org.apache.stanbol.enhancer.servicesapi.ChainException;
import org.apache.stanbol.enhancer.servicesapi.impl.AbstractChain;
import org.apache.stanbol.enhancer.servicesapi.rdf.ExecutionPlan;
import org.osgi.framework.Constants;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Chain implementation that allows to configure the execution graph. Two ways
 * to configure the graph are supported:<ol>
 * <li> Configuration by pointing to an RDF file containing the execution graph.
 * The name of the file needs to be provided by {@link #PROPERTY_GRAPH_RESOURCE}.
 * The file is tracked by using the {@link DataFileListener} service. Typically
 * users will need to copy this file to the /datafiles directory of the
 * Stanbol Environment. However datafiles may be also provided by other means
 * such as bundles or even the default configuration of the Stanbol Environment.
 * <li> Configuration directly provided by the properties of a component instance.
 * The {@link #PROPERTY_CHAIN_LIST} is used for this option. See the java doc for
 * this property for details on the syntax. Users that want to use this option
 * MUST make sure that {@link #PROPERTY_GRAPH_RESOURCE} is not present or
 * empty. Otherwise the {@link #PROPERTY_CHAIN_LIST} will be ignored regardless
 * if the graph resource is available or not.
 * </ol>
 * <i>NOTE:</i> Since <code>0.12.1</code> this supports EnhancementProperties
 * as described by <a href="https://issues.apache.org/jira/browse/STANBOL-488"></a>
 * 
 * @author Rupert Westenthaler
 */
@Component(configurationFactory=true,metatype=true,
    policy=ConfigurationPolicy.REQUIRE)
@Service
@Properties(value={
    @Property(name=Chain.PROPERTY_NAME),
    @Property(name=GraphChain.PROPERTY_GRAPH_RESOURCE),
    @Property(name=GraphChain.PROPERTY_CHAIN_LIST, cardinality=1000),
    @Property(name=AbstractChain.PROPERTY_CHAIN_PROPERTIES,cardinality=1000),
    @Property(name=Constants.SERVICE_RANKING, intValue=0)
})
public class GraphChain extends AbstractChain implements Chain {

    protected final Logger log = LoggerFactory.getLogger(GraphChain.class); 
    
    /**
     * Property used to configure the ImmutableGraph by using the line based 
     * representation with the following Syntax:
     * <code><pre>
     *   &lt;engineName&gt;;&lt;parm1&gt;=&lt;value1&gt;,&lt;value2&gt;;&lt;parm2&gt;=&lt;value1&gt;...
     * </pre></code>
     * Note that this property is ignored in case {@link #PROPERTY_GRAPH_RESOURCE}
     * is defined.
     * <p>
     * Example:
     * <code><pre>
     *   metaxa
     *   langId;dependsOn=metaxa
     *   ner;dependsOn=langId
     *   zemanta;optional
     *   dbpedia-linking;dependsOn=ner
     *   geonames;optional;dependsOn=ner
     *   refactor;dependsOn=geonames,dbpedia-linking,zemanta
     * </pre></code> 
     */
    public static final String PROPERTY_CHAIN_LIST = "stanbol.enhancer.chain.graph.chainlist";
    /**
     * Property used to link to a resource available via the {@link DataFileProvider}
     * utility. The RDF format of the file can be configured by the format parameter.
     * However for well known file extensions (e.g. 'rdf','xml','json','nt')
     * this is not required.<p>
     * Both example will result in "application/rdf+xml" to be used as format to
     * parse the configured resource: <code><pre>
     *     myExecutionPlan.rdf
     *     myExecutionPlan;format=application/rdf+xml
     * </pre></code>
     * Compressed file are not supported because execution plans should be
     * reasonable small.<p>
     * NOTE that if this property is present the {@link #PROPERTY_CHAIN_LIST} is 
     * ignored regardless if the referenced resource is available or not.
     */
    public static final String PROPERTY_GRAPH_RESOURCE = "stanbol.enhancer.chain.graph.graphresource";
    /**
     * Enum with the different configuration modes
     */
    private enum MODE {
        /**
         * Configuration provided by {@link GraphChain#PROPERTY_GRAPH_RESOURCE}
         */
        RESOURCE,
        /**
         * Configuration provided by {@link GraphChain#PROPERTY_CHAIN_LIST}
         */
        LIST
    };
    /**
     * The configuration mode.
     */
    private MODE mode;
    /**
     * The internal chain. Different implementation depending on the {@link #mode}
     */
    private Chain internalChain = null;
    /**
     * The {@link DataFileTracker}. Optional because only required in 
     * {@link MODE#RESOURCE}.
     */
    @Reference(cardinality=ReferenceCardinality.OPTIONAL_UNARY)
    protected DataFileTracker tracker;
    
    /**
     * The {@link Parser}. Optional because only required by
     * {@link ExecutionPlanListerner}.
     */
    @Reference(cardinality=ReferenceCardinality.OPTIONAL_UNARY)
    protected Parser parser;

    @Activate
    @Override
    protected void activate(ComponentContext ctx) throws ConfigurationException {
        super.activate(ctx);
        Object resource = ctx.getProperties().get(PROPERTY_GRAPH_RESOURCE);
        Object list = ctx.getProperties().get(PROPERTY_CHAIN_LIST);
        if(resource != null && !resource.toString().isEmpty()){
            String[] config = resource.toString().split(";");
            String resourceName = config[0];
            String format = getValue(getParameters(config, 1),"format");
            if(format == null){
                format = guessRdfFormat(getExtension(resourceName));
            } else if(format.isEmpty()){
                throw new ConfigurationException(PROPERTY_GRAPH_RESOURCE, 
                    "The configured value for the 'format' parameter MUST NOT be" +
                    "empty (configured: '"+resource+"')!");
            }
            if(format == null){
                throw new ConfigurationException(PROPERTY_GRAPH_RESOURCE, 
                    "RDF formant for extension '"+getExtension(resourceName)+
                    "' is not known. Please use the 'format' parameter to specify" +
                    "it manually (configured: '"+resource+"')!");
            }
            ExecutionPlanListerner epl = new ExecutionPlanListerner(resourceName,format);
            if(tracker != null){
                tracker.add(epl, resourceName, null);
            }
            internalChain = epl;
            mode = MODE.RESOURCE;
        } else if (list != null){
            Set<String> configuredChain = new HashSet<String>();
            if(list instanceof String[]){
                configuredChain.addAll(Arrays.asList((String[])list));
            } else if (list instanceof Collection<?>){
                for(Object o : (Collection<?>) list){
                    if(o instanceof String){
                        configuredChain.add((String)o);
                    }
                }
            } else {
                throw new ConfigurationException(PROPERTY_CHAIN_LIST, 
                    "The list based configuration of a ImmutableGraph Chain MUST BE " +
                    "configured as a Array or Collection of Strings (parsed: "+
                    (list != null?list.getClass():"null")+"). NOTE you can also " +
                    "configure the ImmutableGraph by pointing to a resource with the graph as" +
                    "value of the property '"+PROPERTY_GRAPH_RESOURCE+"'.");
            }
            Map<String,Map<String,List<String>>> config;
            try {
                config = parseConfig(configuredChain);
            } catch (IllegalArgumentException e) {
                throw new ConfigurationException(PROPERTY_CHAIN_LIST, 
                    "Unable to parse the execution plan configuraiton (message: '"+
                    e.getMessage()+"')!",e);
            }
            if(config.isEmpty()){
                throw new ConfigurationException(PROPERTY_CHAIN_LIST, 
                    "The configured execution plan MUST at least contain a single " +
                    "valid execution node!");
            }
            internalChain = new ListConfigExecutionPlan(config,getChainProperties());
            mode = MODE.LIST;
        } else { //both PROPERTY_CHAIN_LIST and PROPERTY_GRAPH_RESOURCE are null
            throw new ConfigurationException(PROPERTY_GRAPH_RESOURCE, 
                "The Execution Plan is a required property. It MUST BE configured" +
                "by one of the properties :"+
                 Arrays.asList(PROPERTY_GRAPH_RESOURCE,PROPERTY_GRAPH_RESOURCE));
        }
    }
    @Deactivate
    @Override
    protected void deactivate(ComponentContext ctx){
        if(mode == MODE.RESOURCE && tracker != null){
            //we need to remove the ExecutionPlanListerner
            tracker.removeAll((DataFileListener)internalChain);
        }
        internalChain = null;
        mode = null;
        tracker = null;
        super.deactivate(ctx);
    }
    @Override
    public ImmutableGraph getExecutionPlan() throws ChainException {
        return internalChain.getExecutionPlan();
    }

    @Override
    public Set<String> getEngines() throws ChainException {
        return internalChain.getEngines();
    }

    /**
     * {@link GraphChain#internalChain} implementation used if the execution
     * plan is provided form a RDF file provided by the {@link DataFileProvider}
     * infrastructure.<p>
     * This implementation requires the optional {@link GraphChain#tracker} and
     * {@link GraphChain#parser} services. If they are not available the
     * {@link #getExecutionPlan()} and {@link #getEngines()} methods will throw
     * {@link ChainException}s.<p>
     * This class implements {@link DataFileListener} to track the configured
     * {@link #resourceName}.
     * 
     * @author Rupert Westenthaler
     *
     */
    private class ExecutionPlanListerner implements DataFileListener, Chain {
        /**
         * The data for the resource as retrieved from 
         * {@link #available(String, InputStream)}. This variable is only
         * set after a call to {@link #available(String, InputStream)} up to the
         * next call to {@link #updateExecutionPlan()}.
         */
        byte[] resourceData;
        /**
         * The execution Plan. Use the {@link #resourceName} to sync access.<p>
         * The executionPlan is parsed and validated within
         * {@link #updateExecutionPlan()}
         */
        private ImmutableGraph executionPlan;
        /**
         * The referenced engine names. Use the {@link #resourceName} to sync 
         * access.<p>
         * This variable is initialised in {@link #updateExecutionPlan()}
         */
        private Set<String> engineNames;
        /**
         * Parser used to parse the RDF {@link ImmutableGraph} from the {@link InputStream}
         * provided to the {@link #available(String, InputStream)} method by the
         * {@link DataFileTracker}.
         */
        private final String format;
        /**
         * The name of the resource. Used to ignore events for other resources
         * within the {@link #available(String, InputStream)} and
         * {@link #unavailable(String)} methods. <p>
         * This final variable is also used as lock to sync access to
         * {@link #resourceData},{@link #executionPlan} and {@link #engineNames}
         */
        private final String resourceName;
        /**
         * Constructs an instance
         * @param rdfParser the parser
         * @param resourceName the name of the resource
         * @throws IllegalArgumentException if any of the two parameter is 
         * <code>null</code> or the resourceName is empty.
         */
        public ExecutionPlanListerner(String resourceName, String format) {
            if(format == null){
                throw new IllegalArgumentException("The parsed rdf format MUST NOT be NULL!");
            }
            if(resourceName == null || resourceName.isEmpty()){
                throw new IllegalArgumentException("The parsed name of the resource MUST NOT be NULL!");
            }
            this.resourceName = resourceName;
            this.format = format;
        }
        @Override
        public boolean available(String resourceName, InputStream is) {
            if(this.resourceName.equals(resourceName)){
                //update ExectionPlan
                synchronized (resourceName) {
                    try{
                        resourceData = IOUtils.toByteArray(is);
                    } catch (IOException e) {
                        log.error("Unable to read data from InputStream provided" +
                        		"by the DataFileTracker for resource "+resourceName,e);
                    }
                    IOUtils.closeQuietly(is);
                    executionPlan = null;
                    engineNames = null;
                }
                return false; //keep tracking
            } else {
                log.warn("received Event for unexpected resource '"+
                    resourceName+"' (tracked: '"+this.resourceName+"') -> " +
                    "ignored and disabled listening for this resource!");
                return true;
            }
        }
        @Override
        public boolean unavailable(String resource) {
            synchronized (resourceName) {
                executionPlan = null;
                engineNames = null;
                resourceData = null;
            }
            return false; //keep tracking
        }
        @Override
        public ImmutableGraph getExecutionPlan() throws ChainException {
            synchronized (resourceName) {
                if(executionPlan == null){
                    updateExecutionPlan();
                }
                return executionPlan;
            }
        }
        @Override
        public Set<String> getEngines() throws ChainException {
            synchronized (resourceName) {
                if(engineNames == null){
                    updateExecutionPlan();
                }
                return engineNames;
            }
        }
        /**
         * Updates the {@link #executionPlan} and {@link #engineNames}
         * based on {@link #resourceData}.<p>
         * This method assumes to be called within a sync on {@link #resourceName}
         * @throws ChainException if {@link #resourceData} is <code>null</code>,
         * {@link GraphChain#parser} is not available, any exception during
         * parsing or if the parsed RDF data are not a valid execution plan
         */
        private void updateExecutionPlan() throws ChainException {
            if(resourceData == null){
                throw new ChainException("The configured resource '"+resourceName+
                    "' for the execution plan is not available via the" +
                    "DataFileProvider infrastructure");
            }
            if(parser == null){
                throw new ChainException("Unable to parse RDF data from resource '"+
                    resourceName+"' because the RDF parser service is not available!");
            }
            try {
                executionPlan = parser.parse(new ByteArrayInputStream(resourceData), format);
            }catch(Exception e){
                throw new ChainException("Unable to parse RDF from resource '"+resourceName+
                    "' using format '"+format+"'!",e);
            }
            resourceData = null; //we have the graph no need to keep the raw data
            //we need still to parse the engines and to validate the plan
            engineNames = validateExecutionPlan(executionPlan);
        }

        @Override
        public String getName() {
            return GraphChain.this.getName();
        }
    }
    /**
     * Implementation the parsed the execution plan form a config as described
     * in the java doc of {@link GraphChain#PROPERTY_CHAIN_LIST}.<p>
     * An instance of this class will be set to {@link GraphChain#internalChain}
     * during activation.
     * @author Rupert Westenthaler
     *
     */
    private final class ListConfigExecutionPlan implements Chain {

        private final ImmutableGraph executionPlan;
        private final Set<String> engines;
        
        /**
         * Parses the execution plan form the configuration.
         * @param config
         */
        private ListConfigExecutionPlan(Map<String,Map<String,List<String>>> config,
                Map<String,Object> chainProperties){
            if(config == null || config.isEmpty()){
                throw new IllegalArgumentException("The parsed execution plan " +
                		"confiuguration MUST NOT be NULL nor empty");
            }
            if(config.remove(null) != null || config.remove("") != null){
                log.warn("ExecutionNode configurations with NULL or an empty " +
                		"engine name where removed form the configuration of the" +
                		"GraphChain '{}'",getName());
            }
            engines = Collections.unmodifiableSet(new HashSet<String>(config.keySet()));
            Graph graph = new SimpleGraph();
            BlankNodeOrIRI epNode = createExecutionPlan(graph, getName(), chainProperties);
            //caches the String name -> {BlankNodeOrIRI node, List<Stirng> dependsOn} mappings
            Map<String,Object[]> name2nodes = new HashMap<String,Object[]>();
            //1. write the nodes (without dependencies)
            for(Entry<String,Map<String,List<String>>> node : config.entrySet()){
                name2nodes.put(
                    node.getKey(),
                    new Object[]{
                        writeExecutionNode(graph, epNode, 
                            node.getKey(), 
                            getState(node.getValue(), "optional"),
                            null,
                            getEnhancementProperties(node.getValue())),
                        node.getValue().get("dependsOn")}); //dependsOn
            }
            //2. write the dependencies
            for(Entry<String,Object[]> info : name2nodes.entrySet()){
                @SuppressWarnings("unchecked")
                List<String> dependsOn = (List<String>)info.getValue()[1];
                if(dependsOn != null){
                    for(String target : dependsOn){
                        Object[] targetInfo = name2nodes.get(target);
                        if(targetInfo != null){
                            graph.add(new TripleImpl(
                                (BlankNodeOrIRI)info.getValue()[0], 
                                ExecutionPlan.DEPENDS_ON, 
                                (BlankNodeOrIRI)targetInfo[0]));
                            
                        } else { //reference to a undefined engine :(
                            throw new IllegalArgumentException("The Engine '"+
                                info.getKey()+"' defines a ex:dependOn to Engine '"+
                                target+"' that is not define in the configuration" +
                                "(defined Engines: "+engines+")!");
                        }
                    }
                } //this node has no dependencies
            }
            this.executionPlan = graph.getImmutableGraph();
        }
        
        @Override
        public ImmutableGraph getExecutionPlan() throws ChainException {
            return executionPlan;
        }

        @Override
        public Set<String> getEngines() throws ChainException {
            return engines;
        }

        @Override
        public String getName() {
            return GraphChain.this.getName();
        }

    }
}
