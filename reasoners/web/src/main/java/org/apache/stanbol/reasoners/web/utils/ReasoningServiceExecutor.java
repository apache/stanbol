package org.apache.stanbol.reasoners.web.utils;

import static javax.ws.rs.core.MediaType.TEXT_HTML;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.locks.Lock;

import javax.servlet.ServletContext;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.UriInfo;

import org.apache.clerezza.rdf.core.MGraph;
import org.apache.clerezza.rdf.core.UriRef;
import org.apache.clerezza.rdf.core.access.LockableMGraph;
import org.apache.clerezza.rdf.core.access.NoSuchEntityException;
import org.apache.clerezza.rdf.core.access.TcManager;
import org.apache.stanbol.commons.web.base.format.KRFormat;
import org.apache.stanbol.owl.transformation.JenaToClerezzaConverter;
import org.apache.stanbol.owl.transformation.OWLAPIToClerezzaConverter;
import org.apache.stanbol.reasoners.jena.JenaReasoningService;
import org.apache.stanbol.reasoners.owlapi.OWLApiReasoningService;
import org.apache.stanbol.reasoners.servicesapi.InconsistentInputException;
import org.apache.stanbol.reasoners.servicesapi.ReasoningServiceException;
import org.apache.stanbol.reasoners.servicesapi.UnsupportedTaskException;
import org.coode.owlapi.manchesterowlsyntax.ManchesterOWLSyntaxOntologyFormat;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.model.OWLOntologyStorageException;
import org.semanticweb.owlapi.model.SWRLRule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Statement;
import com.hp.hpl.jena.reasoner.rulesys.Rule;
import com.sun.jersey.api.view.Viewable;

/**
 * TODO Add comment
 */
public class ReasoningServiceExecutor {
	private Logger log = LoggerFactory.getLogger(getClass());
	private HttpHeaders headers;
	private ServletContext servletContext;
	private UriInfo uriInfo;
	private TcManager tcManager;

	// This task is not dinamically provided by the service, since it work on a
	// specific method
	// (isConsistent())
	public static String TASK_CHECK = "check";

	public ReasoningServiceExecutor(TcManager tcManager, HttpHeaders headers,
			ServletContext context, UriInfo uriInfo) {
		this.headers = headers;
		this.servletContext = context;
		this.uriInfo = uriInfo;
		this.tcManager = tcManager;
	}

	/**
	 * Execute a JenaReasoningService
	 * 
	 * TODO: Add parameter to decide if the output graph must be deleted if
	 * exists
	 * 
	 * @param s
	 * @param input
	 * @param rules
	 * @return
	 */
	public Response executeJenaReasoningService(String task,
			JenaReasoningService s, Model input, List<Rule> rules,
			String targetGraphID, boolean filtered,
			Map<String, List<String>> parameters) {
	    long start = System.currentTimeMillis();
        log.info("[start] Execution: {}",s);
        // Check task: this is managed directly by the endpoint
		if (task.equals(ReasoningServiceExecutor.TASK_CHECK)) {
			log.debug("Task is '{}'", ReasoningServiceExecutor.TASK_CHECK);
			try {
			    boolean is = s.isConsistent(input);
                long end = System.currentTimeMillis();
                log.info("[end] In time: {}", (end - start));
				return buildCheckResponse(is);
			} catch (ReasoningServiceException e) {
				log.error("Error thrown: {}", e);
				throw new WebApplicationException(e,
						Response.Status.INTERNAL_SERVER_ERROR);
			}
		}
		try {
			Set<Statement> result = s.runTask(task, input, rules, filtered,
					parameters);
			if (result == null) {
				log.error("Result is null");
				throw new WebApplicationException();
			}
			Model outputModel = ModelFactory.createDefaultModel();
			outputModel.add(result.toArray(new Statement[result.size()]));
			// If target is null, then get back results, elsewhere put it in
			// target graph
			long end = System.currentTimeMillis();
            log.info("[end] In time: {}", (end - start));
            log.info("Prepare output");
			if (targetGraphID == null) {
				log.info("Returning {} statements", result.size());
				if (isHTML()) {
					ByteArrayOutputStream out = new ByteArrayOutputStream();
					outputModel.write(out, "TURTLE");
					return Response.ok(
							new Viewable("result",
									new ReasoningPrettyResultResource(
											servletContext, uriInfo, out)),
							TEXT_HTML).build();
				} else {
					return Response.ok(outputModel).build();
				}
			} else {
				save(outputModel, targetGraphID);
				return Response.ok().build();
			}
		} catch (ReasoningServiceException e) {
			log.error("Error thrown: {}", e);
			throw new WebApplicationException(e,
					Response.Status.INTERNAL_SERVER_ERROR);
		} catch (InconsistentInputException e) {
			log.debug("The input is not consistent");
			return Response.status(Status.NO_CONTENT).build();
		} catch (UnsupportedTaskException e) {
			log.error("Error thrown: {}", e);
			throw new WebApplicationException(e,
					Response.Status.INTERNAL_SERVER_ERROR);
		}
	}

	/**
	 * Executes the OWLApiReasoingService
	 * 
	 * @param task
	 * @param s
	 * @param input
	 * @param rules
	 * @param targetGraphID
	 * @param parameters
	 * @return
	 */
	public Response executeOWLApiReasoningService(String task,
			OWLApiReasoningService s, OWLOntology input, List<SWRLRule> rules,
			String targetGraphID,boolean filtered, Map<String, List<String>> parameters) {
	    long start = System.currentTimeMillis();
        log.info("[start] Execution: {}",s);
		// Check task: this is managed directly by the endpoint
		if (task.equals(ReasoningServiceExecutor.TASK_CHECK)) {
			log.debug("Task is '{}'", ReasoningServiceExecutor.TASK_CHECK);
			try {
			    boolean is = s.isConsistent(input);
			    long end = System.currentTimeMillis();
	            log.info("[end] In time: {}", (end - start));
				return buildCheckResponse(is);
			} catch (ReasoningServiceException e) {
				throw new WebApplicationException(e,
						Response.Status.INTERNAL_SERVER_ERROR);
			}
		}
		// We get the manager from the input ontology
		// XXX We must be aware of this.
		OWLOntologyManager manager = input.getOWLOntologyManager();
		try {
			OWLOntology output = manager.createOntology();
			Set<OWLAxiom> axioms = s.runTask(task, input, rules, filtered,
                parameters);
			long end = System.currentTimeMillis();
			log.info("[end] In time: {} ms", (end - start));
            log.info("Prepare output: {} axioms",axioms.size());
			manager.addAxioms(output,axioms);
            if (targetGraphID == null) {
				if (isHTML()) {
					ByteArrayOutputStream out = new ByteArrayOutputStream();
					manager.saveOntology(output,
							new ManchesterOWLSyntaxOntologyFormat(), out);
					return Response.ok(
							new Viewable("result",
									new ReasoningPrettyResultResource(
											servletContext, uriInfo, out)),
							TEXT_HTML).build();
				} else {
					return Response.ok(output).build();
				}
			} else {
				save(output, targetGraphID);
				return Response.ok().build();
			}
		} catch (InconsistentInputException e) {
            log.warn("The input is not consistent");
            return buildCheckResponse(false);
        } catch (ReasoningServiceException e) {
            log.error("Error! \n",e);
			throw new WebApplicationException(e,
					Response.Status.INTERNAL_SERVER_ERROR);
		} catch (OWLOntologyCreationException e) {
		    log.error("Error! \n",e);
			throw new WebApplicationException(e,
					Response.Status.INTERNAL_SERVER_ERROR);
		} catch (OWLOntologyStorageException e) {
		    log.error("Error! \n",e);
			throw new WebApplicationException(e,
					Response.Status.INTERNAL_SERVER_ERROR);
		} catch (UnsupportedTaskException e) {
		    log.error("Error! \n",e);
			throw new WebApplicationException(e,
					Response.Status.INTERNAL_SERVER_ERROR);
		}catch(Throwable t){
		    log.error("Error! \n",t);
		    throw new WebApplicationException(t,
                Response.Status.INTERNAL_SERVER_ERROR);
		}
	}

	/**
	 * To build the Response for any CHECK task execution
	 * 
	 * @param isConsistent
	 * @return
	 */
	public Response buildCheckResponse(boolean isConsistent) {
		if (isHTML()) {
			if (isConsistent) {
				log.debug("The input is consistent");
				return Response.ok(
						new Viewable("result",
								new ReasoningPrettyResultResource(
										servletContext, uriInfo,
										"The input is consistent :)")),
						TEXT_HTML).build();
			} else {
				log.debug("The input is not consistent");
				return Response
						.status(Status.NO_CONTENT)
						.entity(new Viewable("result",
								new ReasoningPrettyResultResource(
										servletContext, uriInfo,
										"The input is NOT consistent :(")))
						.type(TEXT_HTML).build();
			}
		} else {
			if (isConsistent) {
				log.debug("The input is consistent");
				return Response.ok("The input is consistent :)").build();
			} else {
				log.debug("The input is not consistent");
				return Response.status(Status.NO_CONTENT).build();
			}
		}
	}

	/**
	 * Check if the client needs a serialization of the output or a human
	 * readable form (HTML)
	 * 
	 * @param headers
	 * @return
	 */
	public boolean isHTML() {
		// We only want to state if HTML format is the preferred format
		// requested
		Set<String> htmlformats = new HashSet<String>();
		htmlformats.add(TEXT_HTML);
		Set<String> rdfformats = new HashSet<String>();
		String[] formats = { TEXT_HTML, "text/plain", KRFormat.RDF_XML,
				KRFormat.TURTLE, "text/turtle", "text/n3" };
		rdfformats.addAll(Arrays.asList(formats));
		List<MediaType> mediaTypes = headers.getAcceptableMediaTypes();
		for (MediaType t : mediaTypes) {
			String strty = t.toString();
			log.info("Acceptable is {}", t);
			if (htmlformats.contains(strty)) {
				log.debug("Requested format is HTML {}", t);
				return true;
			} else if (rdfformats.contains(strty)) {
				log.debug("Requested format is RDF {}", t);
				return false;
			}
		}
		// Default behavior? Should never happen!
		return true;
	}

	/**
	 * To save data in the triple store.
	 * 
	 * @param data
	 * @param targetGraphID
	 */
	protected void save(Object data, String targetGraphID) {
		log.info("Attempt saving in target graph {}", targetGraphID);
		final long startSave = System.currentTimeMillis();
		LockableMGraph mGraph;
		UriRef graphUriRef = new UriRef(targetGraphID);
		try {
			// Check whether the graph already exists
			mGraph = tcManager.getMGraph(graphUriRef);
		} catch (NoSuchEntityException e) {
			mGraph = tcManager.createMGraph(graphUriRef);
		}
		// We lock the graph before proceed
		Lock writeLock = mGraph.getLock().writeLock();
		boolean saved = false;
		if (data instanceof Model) {
			MGraph m = JenaToClerezzaConverter
					.jenaModelToClerezzaMGraph((Model) data);
			writeLock.lock();
			saved = mGraph.addAll(m);
			writeLock.unlock();
		} else if (data instanceof OWLOntology) {
			MGraph m = OWLAPIToClerezzaConverter
					.owlOntologyToClerezzaMGraph((OWLOntology) data);
			writeLock.lock();
			saved = mGraph.addAll(m);
			writeLock.unlock();
		}
		if (!saved)
			throw new WebApplicationException(new IOException(
					"Cannot save model!"),
					Response.Status.INTERNAL_SERVER_ERROR);
		final long endSave = System.currentTimeMillis();
		log.info("Save time: {}", (endSave - startSave));
	}

}
