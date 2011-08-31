package org.apache.stanbol.reasoners.web.resources;

import static javax.ws.rs.core.MediaType.APPLICATION_FORM_URLENCODED;
import static javax.ws.rs.core.MediaType.MULTIPART_FORM_DATA;
import static javax.ws.rs.core.MediaType.TEXT_HTML;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.servlet.ServletContext;
import javax.ws.rs.Consumes;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.apache.clerezza.rdf.core.Graph;
import org.apache.clerezza.rdf.core.MGraph;
import org.apache.clerezza.rdf.core.Triple;
import org.apache.clerezza.rdf.core.UriRef;
import org.apache.clerezza.rdf.core.access.TcManager;
import org.apache.clerezza.rdf.core.impl.TripleImpl;
import org.apache.clerezza.rdf.core.serializedform.Parser;
import org.apache.clerezza.rdf.core.serializedform.Serializer;
import org.apache.clerezza.rdf.core.serializedform.SupportedFormat;
import org.apache.stanbol.commons.web.base.ContextHelper;
import org.apache.stanbol.commons.web.base.format.KRFormat;
import org.apache.stanbol.commons.web.base.resource.BaseStanbolResource;
import org.apache.stanbol.ontologymanager.ontonet.api.ONManager;
import org.apache.stanbol.ontologymanager.ontonet.api.ontology.OntologyScope;
import org.apache.stanbol.ontologymanager.ontonet.api.ontology.OntologySpace;
import org.apache.stanbol.owl.transformation.JenaToOwlConvert;
import org.apache.stanbol.reasoners.jena.JenaReasoningService;
import org.apache.stanbol.reasoners.owlapi.OWLApiReasoningService;
import org.apache.stanbol.reasoners.servicesapi.ReasoningService;
import org.apache.stanbol.reasoners.servicesapi.ReasoningServicesManager;
import org.apache.stanbol.reasoners.servicesapi.UnboundReasoningServiceException;
import org.apache.stanbol.reasoners.web.utils.ReasoningServiceExecutor;
import org.apache.stanbol.rules.base.api.NoSuchRecipeException;
import org.apache.stanbol.rules.base.api.Recipe;
import org.apache.stanbol.rules.base.api.RuleStore;
import org.apache.stanbol.rules.base.api.util.RuleList;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.io.OWLOntologyCreationIOException;
import org.semanticweb.owlapi.io.RDFXMLOntologyFormat;
import org.semanticweb.owlapi.model.AddImport;
import org.semanticweb.owlapi.model.AxiomType;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.MissingImportEvent;
import org.semanticweb.owlapi.model.MissingImportListener;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyChange;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyLoaderListener;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.model.OWLOntologyManagerProperties;
import org.semanticweb.owlapi.model.OWLOntologySetProvider;
import org.semanticweb.owlapi.model.OWLOntologyStorageException;
import org.semanticweb.owlapi.model.SWRLRule;
import org.semanticweb.owlapi.util.OWLOntologyMerger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.reasoner.rulesys.Rule;
import com.hp.hpl.jena.shared.DoesNotExistException;
import com.hp.hpl.jena.vocabulary.OWL;
import com.sun.jersey.api.core.HttpContext;
import com.sun.jersey.api.core.HttpRequestContext;
import com.sun.jersey.api.view.Viewable;
import com.sun.jersey.multipart.BodyPart;
import com.sun.jersey.multipart.FormDataBodyPart;
import com.sun.jersey.multipart.FormDataMultiPart;

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
 */
@Path("/reasoners/{service}/{task}")
public class ReasoningServiceTaskResource extends BaseStanbolResource {
    private Logger log = LoggerFactory.getLogger(getClass());
    private ServletContext context;
    private ReasoningService<?,?,?> service;
    private String taskID;
    private HttpContext httpContext;
    private Map<String,List<String>> parameters;
    private TcManager tcManager;
    private HttpHeaders headers;
    private ONManager onm;
    private Serializer serializer;
    private RuleStore ruleStore;

    public ReasoningServiceTaskResource(@PathParam(value = "service") String serviceID,
                                        @PathParam(value = "task") String taskID,
                                        @Context ServletContext servletContext,
                                        @Context HttpHeaders headers,
                                        @Context HttpContext httpContext) {
        super();
        log.info("Called service {} to perform task {}", serviceID, taskID);

        // ServletContext
        this.context = servletContext;

        // HttpContext
        this.httpContext = httpContext;

        // HttpHeaders
        this.headers = headers;

        // Parameters for customized reasoning services
        this.parameters = prepareParameters();

        // Clerezza storage
        this.tcManager = (TcManager) ContextHelper.getServiceFromContext(TcManager.class, servletContext);

        // Retrieve the ontology network manager
        this.onm = (ONManager) ContextHelper.getServiceFromContext(ONManager.class, servletContext);

        // Retrieve the ontology network manager
        this.ruleStore = (RuleStore) ContextHelper.getServiceFromContext(RuleStore.class, servletContext);

        // Retrieve the clerezza serializer
        this.serializer = (Serializer) ContextHelper.getServiceFromContext(Serializer.class, servletContext);

        // Retrieve the service
        try {
            service = getService(serviceID);
        } catch (UnboundReasoningServiceException e) {
            log.error("Service not found: {}", serviceID);
            throw new WebApplicationException(e, Response.Status.NOT_FOUND);
        }
        log.info("Service retrieved");
        // Check if the task is allowed
        if (this.service.supportsTask(taskID) || taskID.equals(ReasoningServiceExecutor.TASK_CHECK)) {
            this.taskID = taskID;
        } else {
            log.error("Unsupported task (not found): {}", taskID);
            throw new WebApplicationException(new Exception("Unsupported task (not found): " + taskID),
                    Response.Status.BAD_REQUEST);
        }

        // Now we check if the service implementation is supported
        if (getCurrentService() instanceof JenaReasoningService) {} else if (getCurrentService() instanceof OWLApiReasoningService) {} else {
            log.error("This implementation of ReasoningService is not supported: {}", getCurrentService()
                    .getClass());
            throw new WebApplicationException(new Exception(
                    "This implementation of ReasoningService is not supported: "
                            + getCurrentService().getClass()), Response.Status.INTERNAL_SERVER_ERROR);
        }
        log.info("Implementation is supported");

    }

    /**
     * 
     * @return
     */
    private Map<String,List<String>> prepareParameters() {
        Map<String,List<String>> parameters = new HashMap<String,List<String>>();

        log.info("Preparing parameters...");
        HttpRequestContext request = this.httpContext.getRequest();
        // Parameters for a GET request
        MultivaluedMap<String,String> queryParameters = request.getQueryParameters();
        log.info("... {} query parameters found", queryParameters.size());
        for (Entry<String,List<String>> e : queryParameters.entrySet()) {
            parameters.put(e.getKey(), e.getValue());
        }
        // Parameters for a POST request with content-type
        // application/x-www-form-urlencoded
        MultivaluedMap<String,String> formParameters = request.getFormParameters();
        log.info("... {} form urlencoded parameters found", formParameters.size());
        for (Entry<String,List<String>> e : formParameters.entrySet()) {
            parameters.put(e.getKey(), e.getValue());
        }
        log.info("Parameters prepared");
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
    public Response post(@FormParam("url") String url,
                         @FormParam("scope") String scope,
                         @FormParam("session") String session,
                         @FormParam("recipe") String recipe,
                         @FormParam("target") String targetGraphID) {
        return get(url, scope, session, recipe, targetGraphID);
    }

    /**
     * Get the inferences from input URL. If url param is null, get the HTML description of this service/task
     * 
     * @param url
     * @return
     */
    @GET
    @Produces({TEXT_HTML, "text/plain", KRFormat.RDF_XML, KRFormat.TURTLE, "text/turtle", "text/n3"})
    public Response get(@QueryParam("url") String url,
                        @QueryParam("scope") String scope,
                        @QueryParam("session") String session,
                        @QueryParam("recipe") String recipe,
                        @QueryParam("target") String targetGraphID) {
        log.info("Called {} with parameters: {} ",httpContext.getRequest().getMethod(), parameters.keySet().toArray(new String[parameters.keySet().size()]));
        // If all parameters are missing we produce the service/task welcome
        // page
        if (this.parameters.isEmpty()) {
            return Response.ok(new Viewable("index", this)).build();
        }
        if (url != null) {
            // We remove it form the additional parameter list
            this.parameters.remove("url");
        }
        // We remove also target
        this.parameters.remove("target");

        // The service executor
        ReasoningServiceExecutor executor = new ReasoningServiceExecutor(tcManager, headers, servletContext,
                uriInfo);

        /**
         * Select the service implementation TODO Question: how this part could be decoupled?
         */
        if (getCurrentService() instanceof JenaReasoningService) {
            // Prepare input data
            Model input;
            try {
                input = prepareJenaInputFromGET(url, scope, session);
            } catch (DoesNotExistException e) {
                throw new WebApplicationException(e, Response.Status.NOT_FOUND);
            }
            // Prepare rules
            // TODO (this is not implemented yet!)
            List<Rule> rules = prepareJenaRules(recipe);
            return executor.executeJenaReasoningService(getCurrentTask(),
                (JenaReasoningService) getCurrentService(), input, rules, targetGraphID, false,
                this.parameters);
        } else if (getCurrentService() instanceof OWLApiReasoningService) {
            OWLOntology input = null;
            try {
                input = prepareOWLApiInputFromGET(url, scope, session);
            } catch (OWLOntologyCreationIOException e) {
                throw new WebApplicationException(e, Response.Status.NOT_FOUND);
            } catch (OWLOntologyCreationException e) {
                throw new WebApplicationException(e, Response.Status.INTERNAL_SERVER_ERROR);
            }
            // Prepare rules
            List<SWRLRule> rules = prepareOWLApiRules(recipe);
            return executor.executeOWLApiReasoningService(getCurrentTask(),
                (OWLApiReasoningService) getCurrentService(), input, rules, targetGraphID, false, this.parameters);
        }
        throw new WebApplicationException(new Exception("Unsupported implementation"),
                Response.Status.INTERNAL_SERVER_ERROR);
    }

    /**
     * To catch additional parameters in case of a POST with content-type multipart/form-data, we need to
     * acces the {@link FormDataMultiPart} representation of the input.
     * 
     * @param data
     * @return
     */
    @POST
    @Consumes({MULTIPART_FORM_DATA})
    @Produces({TEXT_HTML, "text/plain", KRFormat.RDF_XML, KRFormat.TURTLE, "text/turtle", "text/n3"})
    public Response post(FormDataMultiPart data) {
        File file = null;
        String scope = null;
        String session = null;
        String recipe = null;
        String targetGraphID = null;
        for (BodyPart bpart : data.getBodyParts()) {
            log.info("is a {}", bpart.getClass());
            if (bpart instanceof FormDataBodyPart) {
                FormDataBodyPart dbp = (FormDataBodyPart) bpart;
                if (dbp.getName().equals("target")) {
                    targetGraphID = dbp.getValue();
                } else if (dbp.getName().equals("file")) {
                    file = bpart.getEntityAs(File.class);
                } else if (dbp.getName().equals("scope")) {
                    scope = ((FormDataBodyPart) bpart).getValue();
                } else if (dbp.getName().equals("session")) {
                    session = ((FormDataBodyPart) bpart).getValue();
                } else if (dbp.getName().equals("recipe")) {
                    recipe = ((FormDataBodyPart) bpart).getValue();
                } else {
                    // We put all the rest in the parameters field
                    // XXX We supports here only simple fields
                    // We do NOT support the sent of additional files, for
                    // example
                    if (dbp.isSimple()) {
                        if (this.parameters.containsKey(dbp.getName())) {
                            this.parameters.get(dbp.getName()).add(dbp.getValue());
                        } else {
                            List<String> values = new ArrayList<String>();
                            values.add(dbp.getValue());
                            this.parameters.put(dbp.getName(), values);
                        }
                    }
                }
            }
        }
        return postData(file, scope, session, recipe, targetGraphID);
    }

    /**
     * Generate inferences from the input file. Output comes back to the client.
     * 
     * @param file
     * @return
     */
    private Response postData(File file, String scope, String session, String recipe, String targetGraphID) {
        log.info("Called {} with parameters: {} ",httpContext.getRequest().getMethod(), parameters.keySet().toArray(new String[parameters.keySet().size()]));
        if (file.exists() && file.canRead()) {
            // The service executor
            ReasoningServiceExecutor executor = new ReasoningServiceExecutor(tcManager, headers,
                    servletContext, uriInfo);

            // Select the service implementation
            if (getCurrentService() instanceof JenaReasoningService) {
                // Prepare input data
                Model input;
                try {
                    input = prepareJenaInputFromPOST(file, scope, session);
                } catch (MalformedURLException e) {
                    throw new WebApplicationException(new IllegalArgumentException("Cannot read file"),
                            Response.Status.INTERNAL_SERVER_ERROR);
                }
                // Prepare rules
                List<Rule> rules = prepareJenaRules(recipe);
                return executor.executeJenaReasoningService(getCurrentTask(),
                    (JenaReasoningService) getCurrentService(), input, rules, targetGraphID, false,
                    this.parameters);
            } else if (getCurrentService() instanceof OWLApiReasoningService) {
                OWLOntology input = null;
                try {
                    input = prepareOWLApiInputFromPOST(file, scope, session);
                } catch (OWLOntologyCreationIOException e) {
                    throw new WebApplicationException(e, Response.Status.NOT_FOUND);
                } catch (OWLOntologyCreationException e) {
                    throw new WebApplicationException(e, Response.Status.INTERNAL_SERVER_ERROR);
                }
                // Prepare rules
                List<SWRLRule> rules = prepareOWLApiRules(recipe);
                return executor.executeOWLApiReasoningService(getCurrentTask(),
                    (OWLApiReasoningService) getCurrentService(), input, rules, targetGraphID, false,
                    this.parameters);
            }
            throw new WebApplicationException(new Exception("Unsupported implementation"),
                    Response.Status.INTERNAL_SERVER_ERROR);
        } else {
            log.error("Cannot read file: {}", file);
            throw new WebApplicationException(new IllegalArgumentException("Cannot read file"),
                    Response.Status.INTERNAL_SERVER_ERROR);
        }
    }

    private OWLOntologyManager createOWLOntologyManager() {
        // We isolate here the creation of the temporary manager
        // TODO How to behave when resolving owl:imports?
        // We should set the manager to use a service to lookup for ontologies,
        // instead of trying on the web
        // directly
        OWLOntologyManager manager = OWLManager.createOWLOntologyManager();
       
        // FIXME Which is the other way of doing this?
        // Maybe -> OWLOntologyManagerProperties();
        manager.setSilentMissingImportsHandling(true);
        // Listening for missing imports
        manager.addMissingImportListener(new MissingImportListener() {
            @Override
            public void importMissing(MissingImportEvent arg0) {
                log.warn("Missing import {} ", arg0.getImportedOntologyURI());
            }
        });
        manager.addOntologyLoaderListener(new OWLOntologyLoaderListener(){

            @Override
            public void finishedLoadingOntology(LoadingFinishedEvent arg0) {
                log.info("Finished loading {} (imported: {})",arg0.getOntologyID(),arg0.isImported());
            }

            @Override
            public void startedLoadingOntology(LoadingStartedEvent arg0) {
                log.info("Started loading {} (imported: {}) ...",arg0.getOntologyID(),arg0.isImported());
                log.info(" ... from {}",arg0.getDocumentIRI().toString());
            }});
        return manager;
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
        return (ReasoningServicesManager) ContextHelper.getServiceFromContext(ReasoningServicesManager.class,
            this.context);
    }

    private Object getFromOntonet(String scopeID, String sessionID, Class<?> type) {

        /**
         * FIXME The code below does not work, even if it should, or the API is not clear at all...
         * 
         * 
         * Set<SessionOntologySpace> spaces = onm.getSessionManager() .getSessionSpaces(sessionIRI);
         * 
         * There MUST be 1 single session which such ID. Why this method? What happens if there are more then
         * 1 space? Probably it is not possible, but why this method returns a set? In addition, the method
         * returns **all** sessions (?!).
         * 
         * The code below seems do not work properly (maybe I misunderstood something) also:
         * 
         * try {
         * 
         * 
         * if (!spaces.isEmpty()) {
         * 
         * log.info("found {} session spaces", spaces.size());
         * 
         * for (SessionOntologySpace s : spaces)
         * 
         * log.info(" - {}", s.asOWLOntology());
         * 
         * All empty ontologies! (and not only the one identified by the session ID, there are other sessions
         * within, this is puzzling...)
         * 
         * Then, even if we get the first possible, at the moment ...
         * 
         * SessionOntologySpace session = spaces.iterator().next(); log.info("Found session: {}", session);
         * 
         * OWLOntology sessionOntology = session.asOWLOntology();
         * 
         * 
         * // WHAT I EXPECTED? I expect here to have the ontology network of the session, in this fashion:
         * 
         * - The SESSION space ontology, which includes (owl:imports):
         * 
         * 1) 0...n ontologies, loaded after session creation; they owl:imports the CUSTOM ontology
         * 
         * 2) The CUSTOM space ontology, which includes (owl:imports):
         * 
         * - 0...n ontologies, loaded after scope creation, they owl:imports the CORE
         * 
         * - The CORE space ontology, which includes (owl:imports):
         * 
         * - 0...n ontologies loaded on scope creation
         * 
         * 
         * Instead, I have an empty ontology :(
         * 
         * int importsSize = sessionOntology.getImports().size();
         * 
         * log.info("Session ontology: {}", sessionOntology); // No axioms log.info("Imports {} ontologies",
         * importsSize); // No imports
         * 
         * for (OWLOntology i : sessionOntology.getImports()) { log.info(" - {}", i);
         * 
         * }
         * 
         * return sessionOntology;
         * 
         * } else {
         * 
         * log.error( "The session {} does not exists or have been deleted", sessionID);
         * 
         * throw new IllegalArgumentException("Session does not exists!");
         * 
         * }
         * 
         * } catch (NonReferenceableSessionException e) {
         * log.error("The session {} does not exists or have been deleted", sessionID);
         * 
         * throw new IllegalArgumentException("Session does not exists!", e);
         * 
         * }
         */

        /**
         * FIXME! Another problem with the OntoNet API: Sessions are retrieved from ANY scope, in other words
         * if we have a session we cannot know which is the scope bound. Maybe because you can use data in a
         * session with any other scope? If yes, why this method? Why I can access sessions from a scope?
         * 
         * IRI sessionIRI = IRI.create(sessionID);
         * 
         * ScopeRegistry registry = onm.getScopeRegistry();
         * 
         * Set<OntologyScope> scopes = registry.getActiveScopes();
         * 
         * 
         * OntologyScope scope = null;
         * 
         * SessionOntologySpace sessionSpace = null;
         * 
         * for (OntologyScope s : scopes) {
         * 
         * sessionSpace = s.getSessionSpace(sessionIRI);
         * 
         * if (sessionSpace != null) {
         * 
         * log.info("Found session on scope {}", s.getID());
         * 
         * scope = s;
         * 
         * }
         * 
         * }
         */

        /**
         * FIXME! THIS SHOULD BE DONE BY ONTONET! We pack the ontology network on our own...
         * 
         */
        try {
            // We must know both scope and session
            IRI scopeIRI = IRI.create(scopeID);

            OntologyScope scope = onm.getScopeRegistry().getScope(scopeIRI);
            if (scope == null) {
                log.error("Scope {} cannot be retrieved", sessionID);
                throw new WebApplicationException(Response.Status.NOT_FOUND);
            }
            OntologySpace sessionSpace = null;
            if (sessionID != null) {
                IRI sessionIRI = IRI.create(sessionID);
                sessionSpace = scope.getSessionSpace(sessionIRI);
            }
            OntologySpace coreSpace = scope.getCoreSpace();
            Set<OWLOntology> coreOntologies = coreSpace.getOntologies(true);
            log.info("Found {} ontologies in core space",coreOntologies.size());
            OntologySpace customSpace = scope.getCustomSpace();
            Set<OWLOntology> customOntologies = customSpace.getOntologies(true);
            log.info("Found {} ontologies in custom space",coreOntologies.size());

            Set<OWLOntology> sessionOntologies = new HashSet<OWLOntology>();
            log.info("Found {} ontologies in session space",coreOntologies.size());

            if (sessionSpace != null) {
                // We collect all the ontologies in session (here we use
                // 'false')
                // The reason is that the set contains also an ontology which is
                // the
                // root of the session space, with buggy owl:import
                sessionOntologies.addAll(sessionSpace.getOntologies(false));
            }

            /**
             * FIXME Remove this code. It is not runtime code!!!
             * 
             * We have tried here to create a real network, anyway this is very inefficient, so we use the
             * latter way...
             * 
             * We keep it here for the moment as study...
             * 
             */
            if (false) {
                // Prepare the CORE space
                OWLOntologyManager m = createOWLOntologyManager();
                OWLDataFactory f = m.getOWLDataFactory();
                OWLOntology on_CORE = m.createOntology(IRI.create("ontonet:__CORE__"));
                List<OWLOntologyChange> addImports = new ArrayList<OWLOntologyChange>();
                // We want to load ALL ontologies (to support transitive
                // imports)
                log.info("CORE space: {} [{}]", coreSpace.getID(), coreOntologies.size());
                for (OWLOntology o : coreOntologies) {
                    log.info(" loading {}", o);
                    OWLOntology oLoaded = decantOntology(o, m);
                    log.info("Loaded {}", oLoaded);
                    // We add the import if it is a direct import
                    if (coreSpace.getOntologies(false).contains(o)) {
                        // The we prepare the import statement for the CORE
                        // ontology
                        addImports.add(new AddImport(on_CORE, f.getOWLImportsDeclaration(oLoaded
                                .getOntologyID().getOntologyIRI())));
                        log.info(" preparing change CORE owl:imports {}", oLoaded);
                    }
                }
                m.applyChanges(addImports);
                log.info("Change applied");

                log.info("NETWORK::CORE :: {}", on_CORE);
                log.info(buildImportsTree(on_CORE));

                // Prepare the CUSTOM space
                OWLOntology on_CUSTOM = m.createOntology(IRI.create("ontonet:__CUSTOM__"));
                // Reset changes
                addImports = new ArrayList<OWLOntologyChange>();
                log.info("CUSTOM space: {} [{}]", customSpace.getID(), customOntologies.size());
                for (OWLOntology o : customOntologies) {
                    log.info(" loading {}", o);

                    // We add the import if it is a direct import
                    if (customSpace.getOntologies(false).contains(o)) {
                        /**
                         * We need this conversion every time we want to change the imports declaration of an
                         * ontology which contains axioms. This because an import statement change the way an
                         * axiom is interpreted. For example, if a property is not defined as
                         * OWLObjectProperty or rdfs:Property, it will be loaded as OWLAnnotationProperty !!!
                         */
                        MGraph g = toGraph(o);
                        log.info(" adding owl:imports CORE");
                        // In the network, this ontology will import the CORE
                        g.add(new TripleImpl(new UriRef(o.getOntologyID().getOntologyIRI().toString()),
                                new UriRef(OWL.imports.getURI().toString()), new UriRef(on_CORE
                                        .getOntologyID().getOntologyIRI().toString())));
                        // Load in manager
                        OWLOntology oImported = loadGraph(g, m);
                        g.clear();

                        // Custom imports this ontology (custom is empty, so we
                        // don't need a graph)
                        log.info(" preparing change CUSTOM owl:imports {}", oImported);
                        // The we prepare the import statement for the CUSTOM
                        // ontology
                        addImports.add(new AddImport(on_CUSTOM, f.getOWLImportsDeclaration(o.getOntologyID()
                                .getOntologyIRI())));
                        log.info("Loaded {}", oImported);
                    } else {
                        // We directly load it in the manager
                        OWLOntology oLoaded = decantOntology(o, m);
                        log.info("Loaded {}", oLoaded);
                    }
                }
                // CUSTOM imports CORE
                addImports.add(new AddImport(on_CUSTOM, f.getOWLImportsDeclaration(on_CORE.getOntologyID()
                        .getOntologyIRI())));
                log.info(" preparing change CUSTOM owl:imports CORE");
                m.applyChanges(addImports);

                log.info("NETWORK::CUSTOM :: {}", on_CUSTOM);
                log.info(buildImportsTree(on_CUSTOM));

                // /////////////////////////////////////////////////////////////////////////////////////////////////
                // Prepare the SESSION
                OWLOntology on_SESSION = m.createOntology(IRI.create("ontonet:__SESSION__"));
                // Reset changes
                addImports = new ArrayList<OWLOntologyChange>();

                // Prepare SESSION
                log.info("SESSION space: {} [{}]", sessionSpace.getID(), sessionOntologies.size());

                for (OWLOntology o : sessionOntologies) {
                    log.info(" loading {}", o);

                    // We add the import if it is a direct import
                    if (sessionSpace.getOntologies(false).contains(o)) {
                        log.info(" adding owl:imports CUSTOM");
                        /**
                         * We need this conversion every time we want to change the imports declaration of an
                         * ontology which contains axioms. This because an import statement change the way an
                         * axiom is interpreted. For example, if a property is not defined as
                         * OWLObjectProperty or rdfs:Property, it will be loaded as OWLAnnotationProperty !!!
                         */
                        MGraph g = toGraph(o);
                        // In the network, this ontology will import the CUSTOM
                        Triple triple = new TripleImpl(new UriRef(o.getOntologyID().getOntologyIRI()
                                .toString()), new UriRef(OWL.imports.getURI().toString()), new UriRef(
                                on_CUSTOM.getOntologyID().getOntologyIRI().toString()));
                        log.info(" prepared import triple: {}", triple);
                        g.add(triple);
                        // Load in manager
                        OWLOntology oLoaded = loadGraph(g, m);
                        // remove the temporary mgraph
                        g.clear();

                        log.info(" preparing change SESSION owl:imports {}", oLoaded);
                        // The we prepare the import statement for the CUSTOM
                        // ontology
                        addImports.add(new AddImport(on_SESSION, f.getOWLImportsDeclaration(o.getOntologyID()
                                .getOntologyIRI())));
                        log.info("Loaded {}", oLoaded);
                    } else {
                        // Or we directly go in the manager
                        OWLOntology oLoaded = decantOntology(o, m);
                        log.info("Loaded {}", oLoaded);
                    }
                    log.info(" loaded.");

                }
                // SESSION imports CUSTOM
                log.info(" preparing change SESSION owl:imports CUSTOM");
                addImports.add(new AddImport(on_SESSION, f.getOWLImportsDeclaration(on_CUSTOM.getOntologyID()
                        .getOntologyIRI())));
                for (OWLOntologyChange change : addImports)
                    log.info("CHANGE : {}", change);
                m.applyChanges(addImports);

                log.info("NETWORK::SESSION :: {}", on_SESSION);
                log.info(buildImportsTree(on_SESSION));
                // log.info("Check");
                // for (OWLOntology o : on_SESSION.getImports()) {
                // log.info("checking {}", o);
                // for (OWLAxiom a : o.getAxioms())
                // log.info("- {} [{}]", a, a.getAxiomType());
                // }
            }

            /**
             * This way is more efficient
             */
            final Set<OWLOntology> set = new HashSet<OWLOntology>();
            set.addAll(coreOntologies);
            set.addAll(customOntologies);
            set.addAll(sessionOntologies);
            /**
             * Now we merge the ontologies
             */
            OWLOntologyMerger merger = new OWLOntologyMerger(new OWLOntologySetProvider() {
                @Override
                public Set<OWLOntology> getOntologies() {
                    return set;
                }
            });
            OWLOntology merged = merger.createMergedOntology(createOWLOntologyManager(),
                IRI.create("reasoners:input-" + System.currentTimeMillis()));
            Object output;
            if (type.isAssignableFrom(Model.class)) {
                output = new JenaToOwlConvert().ModelOwlToJenaConvert(merged, "RDF/XML");
            } else if (type.isAssignableFrom(OWLOntology.class)) {
                OWLOntology ready = decantOntology(merged, createOWLOntologyManager());
                output = ready;
                //output = merged;
            } else throw new IllegalArgumentException(new Exception(
                    "Only Model.class and OWLOntology.class are allowed"));
            return output;
        } catch (OWLOntologyCreationException e) {
            log.error("The network for scope/session cannot be retrieved:",e);
            throw new IllegalArgumentException();
        } catch (OWLOntologyStorageException e) {
            log.error("The network for scope/session cannot be retrieved:",e);
            throw new IllegalArgumentException("The network for scope/session cannot be retrieved");
        }
    }

    private OWLOntology decantOntology(OWLOntology o, OWLOntologyManager into) throws OWLOntologyStorageException,
                                                                              OWLOntologyCreationException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        o.getOWLOntologyManager().saveOntology(o, baos);
        ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
        return into.loadOntologyFromOntologyDocument(bais);
    }

    /**
     * The returned graph must be removed as soon as possible!
     * 
     * @param o
     * @return
     * @throws OWLOntologyStorageException
     */
    private MGraph toGraph(OWLOntology o) throws OWLOntologyStorageException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        o.getOWLOntologyManager().saveOntology(o, new RDFXMLOntologyFormat(), baos);
        ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
        final Parser parser = Parser.getInstance();
        Graph deserializedGraph = parser.parse(bais, "application/rdf+xml");
        // FIXME Find a better way to generate the temporary ID
        String temporaryID = "reasoners-network-temporary-" + System.currentTimeMillis();
        MGraph temporaryGraph = tcManager.createMGraph(new UriRef(temporaryID));
        temporaryGraph.addAll(deserializedGraph);
        return temporaryGraph;
    }

    private OWLOntology loadGraph(MGraph g, OWLOntologyManager m) throws OWLOntologyCreationException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        serializer.serialize(baos, g, SupportedFormat.RDF_XML);
        return m.loadOntologyFromOntologyDocument(new ByteArrayInputStream(baos.toByteArray()));
    }

    private StringBuilder recursiveImportsTree(OWLOntology on, StringBuilder builder, int level) {
        String ls = System.getProperty("line.separator");
        String lvl = "--";
        for (OWLOntology ch : on.getDirectImports()) {
            builder.append(ls);
            for (int x = 0; x < level; x++) {
                builder.append(lvl);
            }
            builder.append("> ");
            builder.append(ch.getOntologyID());
            builder.append(" [imports: ");
            builder.append(ch.getDirectImports().size());
            builder.append("] [axioms: ");
            builder.append(ch.getAxiomCount());
            builder.append("] [logical axioms: ");
            builder.append(ch.getLogicalAxiomCount());
            builder.append("] [annotations: ");
            builder.append(ch.getAxiomCount(AxiomType.ANNOTATION_ASSERTION));
            builder.append("]");
            if (ch.getDirectImports().size() > 0) {
                builder = recursiveImportsTree(ch, builder, level + 1);
            }
        }
        return builder;
    }

    private String buildImportsTree(OWLOntology o) {
        StringBuilder b = new StringBuilder();
        b.append(System.getProperty("line.separator"));
        b.append(o);
        return recursiveImportsTree(o, b, 1).toString();
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

    private Model prepareJenaInputFromGET(String url, String scope, String session) {
        long start = System.currentTimeMillis();
        log.info("[start] Prepare input for Jena ");
        OntModel input = ModelFactory.createOntologyModel();
        // Get the network as Jena model
        if (scope != null) {
            input.add((Model) getFromOntonet(scope, session, Model.class));
        }
        // If url exists, merge the location within the model
        if (url != null) {
            input.read(url);
        }
        long end = System.currentTimeMillis();
        log.info("[end] Prepared input for Jena in {} ms. Size is: {}", (end - start), input.getGraph()
                .size());
        return input;
    }

    private Model prepareJenaInputFromPOST(File file, String scope, String session) throws MalformedURLException {
        long start = System.currentTimeMillis();
        log.info("[start] Prepare input for Jena ");
        OntModel input = ModelFactory.createOntologyModel();
        // Get the network as Jena model
        if (scope != null) {
            input.add((Model) getFromOntonet(scope, session, Model.class));
        }
        // If file exists, merge the location within the model
        if (file != null) {
            input.read(file.toURI().toURL().toString());
        }
        long end = System.currentTimeMillis();
        log.info("[end] Prepared input for Jena in {} ms. Size is: {}", (end - start), input.getGraph()
                .size());
        return input;
    }

    private OWLOntology prepareOWLApiInputFromGET(String url, String scope, String session) throws OWLOntologyCreationException {
        long start = System.currentTimeMillis();
        log.info("[start] Prepare input for OWLApi ");
        OWLOntology input;
        if (scope != null) {
            input = (OWLOntology) getFromOntonet(scope, session, OWLOntology.class);
        } else {
            input = createOWLOntologyManager().createOntology();
        }
        if (url != null) {
            // We add additional axioms
            OWLOntology fromUrl = input.getOWLOntologyManager().loadOntologyFromOntologyDocument(
                IRI.create(url));
            Set<OWLOntology> all = fromUrl.getImportsClosure();
            for(OWLOntology o : all){
                for (OWLAxiom a : o.getAxioms()) {
                    input.getOWLOntologyManager().addAxiom(input, a);
                }
            }
        }
        try {
            synchronized (input) {
                input = decantOntology(input, OWLManager.createOWLOntologyManager());
            }
        } catch (OWLOntologyStorageException e) {
            log.error("Cannot prepare the input");
            throw new OWLOntologyCreationException();
        }
        long end = System.currentTimeMillis();
        log.info("[end] Prepared input for OWLApi in {} ms. Size is: {}", (end - start), input.getAxiomCount());
        return input;
    }

    private OWLOntology prepareOWLApiInputFromPOST(File file, String scope, String session) throws OWLOntologyCreationException {
        long start = System.currentTimeMillis();
        log.info("[start] Prepare input for OWLApi ");
        OWLOntology input;
        if (scope != null) {
            input = (OWLOntology) getFromOntonet(scope, session, OWLOntology.class);
        } else {
            input = createOWLOntologyManager().createOntology();
        }
        if (file != null) {
            // We add additional axioms
            OWLOntology fromUrl = input.getOWLOntologyManager().loadOntologyFromOntologyDocument(file);
            Set<OWLOntology> all = fromUrl.getImportsClosure();
            for(OWLOntology o : all){
                for (OWLAxiom a : o.getAxioms()) {
                    input.getOWLOntologyManager().addAxiom(input, a);
                }
            }
        }
        try {
            synchronized (input) {
                input = decantOntology(input, OWLManager.createOWLOntologyManager());
            }
        } catch (OWLOntologyStorageException e) {
            log.error("Cannot prepare the input");
            throw new OWLOntologyCreationException();
        }
        long end = System.currentTimeMillis();
        log.info("[end] Prepared input for OWLApi in {} ms. Size is: {}", (end - start), input.getAxiomCount());
        return input;
    }

    private List<Rule> prepareJenaRules(String recipe) {
        if (recipe != null) {
            // If recipe exists, parse it as a list of Jena rules
            // TODO This cannot be implemented since Jena rules format is not
            // yet supported by the Rules
            // module!!! (See STANBOL-186)
            log.error("prepareJenaRules(String recipe) Not implemented yet!");
            throw new WebApplicationException(501);
        }
        return null;
    }

    private List<SWRLRule> prepareOWLApiRules(String recipe) {
        List<SWRLRule> rules = null;
        if (recipe != null) {
            long start = System.currentTimeMillis();
            log.info("[start] Prepare rules for OWLApi ");

         // If recipe exists, return it as a list of SWRL rules
            rules = new ArrayList<SWRLRule>();
            try {
                Recipe rec = ruleStore.getRecipe(IRI.create(recipe));
                log.debug("Recipe is: {}",rec);
                RuleList ruleList = rec.getkReSRuleList();
                log.debug("RuleList is: {}",ruleList);
                for(org.apache.stanbol.rules.base.api.Rule r : ruleList ){
                    SWRLRule swrl = r.toSWRL(OWLManager.getOWLDataFactory());
                    log.debug("Prepared rule: {}",swrl);
                    rules.add(swrl);
                }
            } catch (NoSuchRecipeException e) {
                log.error("Recipe {} does not exists",recipe);
                throw new WebApplicationException(e,Status.NOT_FOUND);
            }

            long end = System.currentTimeMillis();
            log.info("[end] Prepared {} rules for OWLApi in {} ms.", rules.size(), (end - start));
            
        }
        return rules;
    }
}
