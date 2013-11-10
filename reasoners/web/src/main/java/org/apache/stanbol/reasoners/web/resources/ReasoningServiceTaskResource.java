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
package org.apache.stanbol.reasoners.web.resources;

import static javax.ws.rs.core.MediaType.APPLICATION_FORM_URLENCODED;
import static javax.ws.rs.core.MediaType.MULTIPART_FORM_DATA;
import static javax.ws.rs.core.MediaType.TEXT_HTML;

import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Form;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;
import javax.ws.rs.core.UriInfo;

import org.apache.clerezza.jaxrs.utils.form.FormFile;
import org.apache.clerezza.jaxrs.utils.form.MultiPartBody;
import org.apache.clerezza.rdf.core.access.TcManager;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.stanbol.commons.jobs.api.JobManager;
import org.apache.stanbol.commons.web.viewable.Viewable;
import org.apache.stanbol.commons.web.base.format.KRFormat;
import org.apache.stanbol.commons.web.base.resource.BaseStanbolResource;
import org.apache.stanbol.ontologymanager.servicesapi.scope.ScopeManager;
import org.apache.stanbol.ontologymanager.servicesapi.session.SessionManager;
import org.apache.stanbol.reasoners.jena.JenaReasoningService;
import org.apache.stanbol.reasoners.owlapi.OWLApiReasoningService;
import org.apache.stanbol.reasoners.servicesapi.ReasoningService;
import org.apache.stanbol.reasoners.servicesapi.ReasoningServiceInputManager;
import org.apache.stanbol.reasoners.servicesapi.ReasoningServicesManager;
import org.apache.stanbol.reasoners.servicesapi.UnboundReasoningServiceException;
import org.apache.stanbol.reasoners.servicesapi.annotations.Documentation;
import org.apache.stanbol.reasoners.web.input.impl.SimpleInputManager;
import org.apache.stanbol.reasoners.web.input.provider.impl.ByteArrayInputProvider;
import org.apache.stanbol.reasoners.web.input.provider.impl.OntologyManagerInputProvider;
import org.apache.stanbol.reasoners.web.input.provider.impl.RecipeInputProvider;
import org.apache.stanbol.reasoners.web.input.provider.impl.UrlInputProvider;
import org.apache.stanbol.reasoners.web.utils.ReasoningServiceExecutor;
import org.apache.stanbol.reasoners.web.utils.ReasoningServiceResult;
import org.apache.stanbol.reasoners.web.utils.ResponseTaskBuilder;
import org.apache.stanbol.rules.base.api.RuleAdapterManager;
import org.apache.stanbol.rules.base.api.RuleStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Endpoint for reasoning services. Services can be invoked using the service name and task in the request
 * path. The related active SCR service is selected, then the concrete execution is delegated to a {@see
 * ReasoningServiceExecutor}.
 * 
 * Two different kind of implementation of {@see ReasoningService} are supported: {@see JenaReasoningService}
 * and {@see OWLApiReasonngService}.
 * 
 * This class includes methods to prepare the input and dispatch the output (back to the client in the
 * requested format or saved in the triple store).
 * 
 * Support for long term operations is provided by adding /job to the request URI.
 * 
 */
@Component
@Service(Object.class)
@Property(name="javax.ws.rs", boolValue=true)
@Path("/reasoners/{service}/{task: [^/]+}{job: (/job)?}")
public class ReasoningServiceTaskResource extends BaseStanbolResource {
    private Logger log = LoggerFactory.getLogger(getClass());
//    private ServletContext context;
    
    private ReasoningService<?,?,?> service;
    
    private Map<String,List<String>> parameters;
    private FormFile file = null;
    
    @Reference
    protected TcManager tcManager;
    
    
    @Reference
    protected ScopeManager onm;
    
    @Reference
    protected SessionManager sessionManager;
    
    @Reference
    protected RuleStore ruleStore;
    
    @Reference
    protected RuleAdapterManager adapterManager;
    private boolean job = false;
    private String jobLocation = "";
    
    @Reference
    protected ReasoningServicesManager reasoningServicesManager;
    
    @Reference
    protected JobManager jobManager;

    @Context 
    private UriInfo uriInfo;
    @Context 
    private Form form = null;
    @Context 
    private HttpHeaders headers;
    
    private String taskID;
    
    /**
     * Constructor
     * 
     */
    public ReasoningServiceTaskResource() {
        super();
    }

    public void prepare(String serviceID, String taskIDstr, String jobFlag) {
        this.taskID = taskIDstr;
        
        log.debug("Called service {} to perform task {}", serviceID, taskID);

        // Parameters for customized reasoning services
        this.parameters = prepareParameters();

        // Check if method is allowed
        // FIXME Supported methods are only GET and POST, but also PUT comes here, why?
//        String[] supported = {"GET", "POST"};
//        if (!Arrays.asList(supported).contains(this.httpContext.getRequest().getMethod())) {
//            throw new WebApplicationException(405);
//        }

        // Retrieve the service
        try {
            service = getService(serviceID);
        } catch (UnboundReasoningServiceException e) {
            log.error("Service not found: {}", serviceID);
            throw new WebApplicationException(e, Response.Status.NOT_FOUND);
        }
        log.debug("Service retrieved");
        // Check if the task is allowed
        if (this.service.supportsTask(taskID) || taskID.equals(ReasoningServiceExecutor.TASK_CHECK)) {
            // Ok
        } else {
            log.error("Unsupported task (not found): {}", taskID);
            throw new WebApplicationException(new Exception("Unsupported task (not found): " + taskID),
                    Response.Status.NOT_FOUND);
        }
        // Check for the job parameter
        if (!jobFlag.equals("")) {
            log.debug("Job param is {}", job);
            if (jobFlag.equals("/job")) {
                log.debug("Ask for background job");
                this.job = true;
            } else {
                log.error("Malformed request");
                throw new WebApplicationException(Response.Status.BAD_REQUEST);
            }
        }
        // Now we check if the service implementation is supported
        if (getCurrentService() instanceof JenaReasoningService) {} else if (getCurrentService() instanceof OWLApiReasoningService) {} else {
            log.error("This implementation of ReasoningService is not supported: {}", getCurrentService()
                    .getClass());
            throw new WebApplicationException(new Exception(
                    "This implementation of ReasoningService is not supported: "
                            + getCurrentService().getClass()), Response.Status.INTERNAL_SERVER_ERROR);
        }
        log.debug("Implementation is supported");
    }

    /**
     * 
     * @return
     */
    private Map<String,List<String>> prepareParameters() {
        Map<String,List<String>> parameters = new HashMap<String,List<String>>();

        log.debug("Preparing parameters...");
        
        // Parameters for a GET request and POST
        Map<?,?> queryParameters = uriInfo.getQueryParameters();
        log.debug("... {} query parameters found", queryParameters.size());
        for (Entry<?,?> e : queryParameters.entrySet()) {
            String k = (String) e.getKey();
            log.debug(" param: {} ", k);
            List<String> v = new ArrayList<String>();
            /*
             * XXX 
             * It looks like that param values here are not urldecoded
             * This is odd because the not on the method says exactly the opposite.
             * @see getQueryParameters()
             */
            for(String s : (List<String>) e.getValue()){
                try {
                    s = URLDecoder.decode(s, "UTF-8");
                } catch (UnsupportedEncodingException e1) {
                    e1.printStackTrace();
                }
                log.debug("   value {}", v);
                v.add(s);
            }
            parameters.put(k, v);
        }
        // Parameters for a POST request with content-type
        // application/x-www-form-urlencoded
        if(form!=null){
            MultivaluedMap<String,String> formParameters = form.asMap();
            log.debug("... {} form urlencoded parameters found", formParameters.size());
            for (Entry<String,List<String>> e : formParameters.entrySet()) {
                parameters.put(e.getKey(), e.getValue());
            }
        }
        log.debug("Parameters prepared");
        return parameters;
    }

    /**
     * This is an alias of the get method.
     * 
     * @param url
     * @param targetGraphID
     * @return
     */
    @POST
    @Consumes({APPLICATION_FORM_URLENCODED})
    @Produces({TEXT_HTML, "text/plain", KRFormat.RDF_XML, KRFormat.TURTLE, "text/turtle", "text/n3"})
    public Response post(@PathParam(value = "service")  String serviceID, 
                         @PathParam(value = "task") String taskID,
                         @PathParam(value = "job")  String jobFlg) {
        prepare(serviceID, taskID, jobFlg);
        return processRequest();
    }

    private Response processRequest() {
        if (job) {
            log.trace("Processing in background");
            return processBackgroundRequest();
        } else {
            log.trace("Processing in foreground");
            return processRealTimeRequest();
        }
    }

    /**
     * Get the inferences from input URL. If url param is null, get the HTML description of this service/task
     * 
     * @param url
     * @return
     */
    @GET
    @Produces({TEXT_HTML, "text/plain", KRFormat.RDF_XML, KRFormat.TURTLE, "text/turtle", "text/n3"})
    public Response get(@QueryParam("target") String targetGraphID,
                        @PathParam(value = "service")  String serviceID, 
                        @PathParam(value = "task") String taskID,
                        @PathParam(value = "job")  String jobFlg) {
        log.debug("Called GET serviceID {} taskID {}",serviceID, taskID);
        prepare(serviceID, taskID, jobFlg);
        log.debug("Called GET with parameters: {} ", parameters.keySet()
                .toArray(new String[parameters.keySet().size()]));
        
        return processRequest();
    }

    /**
     * Process a background request. This service use the Stanbol Commons Jobs API to start a background job.
     * Returns 201 on success, with HTTP header Location pointing to the Job resource.
     * 
     * @return
     */
    private Response processBackgroundRequest() {
        // If parameters is empty it's a bad request...
        if (this.parameters.isEmpty()) {
            log.error("Cannot start job without input parameters... sending BAD REQUEST");
            throw new WebApplicationException(Response.Status.BAD_REQUEST);
        }
        String target = getTarget();
        // Setup the input
        ReasoningServiceInputManager imngr = prepareInput();
        // The service executor
        ReasoningServiceExecutor executor = new ReasoningServiceExecutor(tcManager, imngr,
                getCurrentService(), getCurrentTask(), target, parameters);
        String jid = getJobManager().execute(executor);
        URI location = URI.create(getPublicBaseUri() + "jobs/" + jid);
        this.jobLocation = location.toString();
        /**
         * If everything went well, we return 201 Created We include the header Location: with the Job URL
         */
        Viewable view = new Viewable("created", this);
        return Response.created(location).entity(view).build();
    }

    /**
     * Process a real-time operation. Returns 200 when the process is ready, 500 if some error occurs
     * 
     * @return
     */
    private Response processRealTimeRequest() {
        // If all parameters are missing we produce the service/task welcome
        // page
        if (this.parameters.isEmpty() && file == null) {
            log.debug("no parameters no input file, show default index page");
            // return Response.ok(new Viewable("index", this)).build();
            ResponseBuilder rb = Response.ok(new Viewable("index", this));
            rb.header(HttpHeaders.CONTENT_TYPE, TEXT_HTML + "; charset=utf-8");
            return rb.build();
        }
        try {
            String target = getTarget();
            // Setup the input
            ReasoningServiceInputManager imngr = prepareInput();
            // The service executor
            ReasoningServiceExecutor executor = new ReasoningServiceExecutor(tcManager, imngr,
                    getCurrentService(), getCurrentTask(), target, parameters);
            ReasoningServiceResult<?> result = executor.call();
            return new ResponseTaskBuilder(new ReasoningTaskResult(uriInfo, headers)).build(result);
        } catch (Exception e) {
            if (e instanceof RuntimeException) {
                throw (RuntimeException)e;
            }
            throw new WebApplicationException(e, Response.Status.INTERNAL_SERVER_ERROR);
        } 
    }

    /**
     * Get the target graph, or null if no target graph have been given
     * 
     * @return
     */
    private String getTarget() {
        String target = null;
        if (parameters.get("target") != null) {
            if (!parameters.get("target").isEmpty()) {
                target = parameters.get("target").iterator().next();
                if (target.equals("")) {
                    // Parameter exists with empty string value
                    log.error("Parameter 'target' must have a value!");
                    throw new WebApplicationException(Response.Status.BAD_REQUEST);
                }
            } else {
                // Parameter exists with empty value
                log.error("Parameter 'target' must have a value!");
                throw new WebApplicationException(Response.Status.BAD_REQUEST);
            }
        }
        return target;
    }

    /**
     * To catch additional parameters in case of a POST with content-type multipart/form-data, we need to
     * access the {@link FormDataMultiPart} representation of the input.
     * 
     * @param data
     * @return
     */
    @POST
    @Consumes({MULTIPART_FORM_DATA})
    @Produces({TEXT_HTML, "text/plain", KRFormat.RDF_XML, KRFormat.TURTLE, "text/turtle", "text/n3"})
    public Response post(MultiPartBody data,
                         @PathParam(value = "service")  String serviceID, 
                         @PathParam(value = "task") String taskID,
                         @PathParam(value = "job")  String jobFlg) {
        prepare(serviceID, taskID, jobFlg);
        log.debug("Called POST post(MultiPartBody data, ...)");
        // In this case we setup the parameter from a multipart request
        
        if(data.getFormFileParameterValues("file").length > 0){
            file = data.getFormFileParameterValues("file")[0]; 
        }else{
            log.debug("No files in multipart body");
        }
        
        for(String p : data.getTextParameterNames()){
            this.parameters.put(p, Arrays.asList(data.getTextParameterValues(p)));
        } 

        log.debug(" parameters: {}", parameters);
        log.debug(" file: {}", file);        
//        // Then add the file
//        if (file != null) {
//            List<String> values = new ArrayList<String>();
//            try {
//                if (file.canRead() && file.exists()) {
//                    values.add(file.toURI().toURL().toString());
//                } else {
//                    log.error("Bad request");
//                    log.error(" file is: {}", file);
//                    throw new WebApplicationException(Response.Status.BAD_REQUEST);
//                }
//            } catch (MalformedURLException e) {
//                // This should never happen
//                throw new WebApplicationException();
//            }
//            this.parameters.put("file", values);
//        }
        return processRequest();
    }

    /**
     * Binds the request parameters to a list of {@see ReasoningServiceInputProvider}s, and fed a {@see
     * SimpleInputManager}. TODO In the future we may want to decouple this process from this
     * resource/submodule.
     * 
     * @return
     */
    private ReasoningServiceInputManager prepareInput() {
        ReasoningServiceInputManager inmgr = new SimpleInputManager();
        String scope = null;
        String session = null;
        
        if(file != null){
            inmgr.addInputProvider(new ByteArrayInputProvider(file.getContent()));
        }
        
        for (Entry<String,List<String>> entry : this.parameters.entrySet()) {
            if (entry.getKey().equals("url")) {
                if (!entry.getValue().isEmpty()) {
                    // We keep only the first value
                    // XXX (make sense support multiple values?)
                    inmgr.addInputProvider(new UrlInputProvider(entry.getValue().iterator().next()));
                    // We remove it form the additional parameter list
                    this.parameters.remove("url");
                } else {
                    // Parameter exists with no value
                    log.error("Parameter 'url' must have a value!");
                    throw new WebApplicationException(Response.Status.BAD_REQUEST);
                }
            }
//            else if (entry.getKey().equals("file")) {
//                if (!entry.getValue().isEmpty()) {
//                    // We keep only the first value
//                    // FIXME We create the file once again...
//                    String fv = entry.getValue().iterator().next();
//                    log.debug("File value is: {}", fv);
//                    inmgr.addInputProvider(new FileInputProvider(new File(URI.create(fv))));
//                    // We remove it form the additional parameter list
//                    this.parameters.remove("url");
//                } else {
//                    // Parameter exists with no value
//                    log.error("Parameter 'url' must have a value!");
//                    throw new WebApplicationException(Response.Status.BAD_REQUEST);
//                }
//            } 
            else if (entry.getKey().equals("scope")) {
                if (!entry.getValue().isEmpty()) {
                    scope = entry.getValue().iterator().next();
                } else {
                    // Parameter exists with no value
                    log.error("Parameter 'scope' must have a value!");
                    throw new WebApplicationException(Response.Status.BAD_REQUEST);
                }

            } else if (entry.getKey().equals("session")) {
                if (!entry.getValue().isEmpty()) {
                    session = entry.getValue().iterator().next();
                } else {
                    // Parameter exists with no value
                    log.error("Parameter 'session' must have a value!");
                    throw new WebApplicationException(Response.Status.BAD_REQUEST);
                }

            } else if (entry.getKey().equals("recipe")) {
                if (!entry.getValue().isEmpty()) {
                    inmgr.addInputProvider(new RecipeInputProvider(ruleStore, adapterManager, entry
                            .getValue().iterator().next()));
                    // We remove it form the additional parameter list
                    this.parameters.remove("url");
                } else {
                    // Parameter exists with no value
                    log.error("Parameter 'recipe' must have a value!");
                    throw new WebApplicationException(Response.Status.BAD_REQUEST);
                }

            }
        }
        if (scope != null) {
            inmgr.addInputProvider(new OntologyManagerInputProvider(onm, sessionManager, scope, session));
            this.parameters.remove("scope");
            this.parameters.remove("session");
        }
        return inmgr;
    }

    /**
     * The actual path, to be used in the template.
     * 
     * @return
     */
    public String getCurrentPath() {
        return uriInfo.getPath().replaceAll("[\\/]*$", "");
    }

    /**
     * The selected service
     * 
     * @return
     */
    public ReasoningService<?,?,?> getCurrentService() {
        return this.service;
    }

    /**
     * The selected task
     * 
     * @return
     */
    public String getCurrentTask() {
        return this.taskID;
    }

    /**
     * If this resource created a job, this field contains the location to be rendered in the viewable.
     * 
     * @return
     */
    public String getJobLocation() {
        return this.jobLocation;
    }

    /**
     * The list of supported tasks. We include CHECK, which is managed directly by the endpoint.
     */
    public List<String> getSupportedTasks() {
        List<String> supported = new ArrayList<String>();
        supported.add(ReasoningServiceExecutor.TASK_CHECK);
        supported.addAll(getCurrentService().getSupportedTasks());
        return supported;
    }

    /**
     * To retrieve the service using the service manager
     * 
     * @param servicePath
     * @return
     * @throws UnboundReasoningServiceException
     */
    private ReasoningService<?,?,?> getService(String servicePath) throws UnboundReasoningServiceException {
        return getServicesManager().get(servicePath);
    }

    /**
     * Get the service manager from the context
     * 
     * @return
     */
    private ReasoningServicesManager getServicesManager() {
        log.debug("(getServicesManager()) ");
        return reasoningServicesManager;
    }

    /**
     * Gets the list of active services
     * 
     * @return
     */
    public Set<ReasoningService<?,?,?>> getActiveServices() {
        log.debug("(getActiveServices()) There are {} reasoning services", getServicesManager().size());
        return getServicesManager().asUnmodifiableSet();
    }

    /**
     * Gets the job manager
     * 
     * @return
     */
    private JobManager getJobManager() {
        log.debug("(getJobManager()) ");
        return jobManager;
    }

    public Map<String,String> getServiceDescription() {
        return getServiceDescription(service);
    }

    public Map<String,String> getServiceDescription(ReasoningService<?,?,?> service) {
        Class<?> serviceC = service.getClass();
        String name;
        try {
            name = serviceC.getAnnotation(Documentation.class).name();
        } catch (NullPointerException e) {
            log.warn("The service {} is not documented: missing name", serviceC);
            name = "";
        }
        String description;
        try {
            description = serviceC.getAnnotation(Documentation.class).description();
        } catch (NullPointerException e) {
            log.warn("The service {} is not documented: missing description", serviceC);
            description = "";
        }
        // String file = serviceC.getAnnotation(Documentation.class).file();
        Map<String,String> serviceProperties = new HashMap<String,String>();
        serviceProperties.put("name", name);
        serviceProperties.put("description", description);
        // serviceProperties.put("file", file);
        serviceProperties.put("path", service.getPath());
        return serviceProperties;
    }
    
    public class ReasoningTaskResult extends ResultData implements ReasoningResult {
        private Object result;
        private UriInfo uriInfo;
        private HttpHeaders headers;
        public ReasoningTaskResult(UriInfo uriInfo, HttpHeaders headers) {
            this.headers = headers;
            this.uriInfo = uriInfo;
        }

        public void setResult(Object result){
            this.result = result;
        }
        public Object getResult() {
            return this.result;
        }

        public HttpHeaders getHeaders() {
            return headers;
        }
        
        public UriInfo getUriInfo(){
            return uriInfo;
        }
    }
}
