package eu.iksproject.kres.jersey.resource;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import javax.servlet.ServletContext;
import javax.ws.rs.Consumes;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import org.apache.clerezza.rdf.core.access.TcManager;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.AddAxiom;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLNamedIndividual;
import org.semanticweb.owlapi.model.OWLObjectProperty;
import org.semanticweb.owlapi.model.OWLObjectPropertyAssertionAxiom;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import eu.iksproject.kres.api.format.KReSFormat;
import eu.iksproject.kres.api.manager.KReSONManager;
import eu.iksproject.kres.api.storage.NoSuchStoreException;
import eu.iksproject.kres.api.storage.OntologyStorage;
import eu.iksproject.kres.api.storage.OntologyStoreProvider;
import eu.iksproject.kres.shared.transformation.JenaToOwlConvert;

import org.semanticweb.owlapi.model.OWLOntology;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Bag;
import com.sun.jersey.api.view.ImplicitProduces;

@Path("/graphs")
@ImplicitProduces(MediaType.TEXT_HTML + ";qs=2")
public class GraphsResource extends NavigationMixin {

	private OntologyStoreProvider ontologyStoreProvider;
	protected TcManager tcManager;
	protected KReSONManager onManager;
	protected OntologyStorage storage;
	
	private final Logger log = LoggerFactory.getLogger(getClass());
	
	public GraphsResource(@Context ServletContext servletContext) {
		ontologyStoreProvider  = (OntologyStoreProvider) (servletContext.getAttribute(OntologyStoreProvider.class.getName()));
		tcManager = (TcManager) servletContext.getAttribute(TcManager.class.getName());
		onManager  = (KReSONManager) (servletContext.getAttribute(KReSONManager.class.getName()));
        if (onManager == null) {
            throw new IllegalStateException(
                    "OntologyStorage missing in ServletContext");
        }
        else{
        	storage = onManager.getOntologyStore();
        }
    }
	
	@GET
	@Path("/resume")
	@Produces( {KReSFormat.FUNCTIONAL_OWL, 
				KReSFormat.MANCHESTER_OWL, 
				KReSFormat.OWL_XML, 
				KReSFormat.RDF_XML,
				KReSFormat.TURTLE,
				KReSFormat.RDF_JSON})
	public Response graphs(@Context HttpHeaders headers, @Context ServletContext servletContext){
		Set<IRI> iris = storage.listGraphs();
		if(iris != null){
			
			OWLOntologyManager manager = OWLManager.createOWLOntologyManager();
			OWLDataFactory factory = OWLManager.getOWLDataFactory();
			
			OWLOntology ontology;
			try {
				ontology = manager.createOntology();
				
				String ns = onManager.getKReSNamespace();
				
				OWLNamedIndividual storage = factory.getOWLNamedIndividual(IRI.create(ns+"Storage"));
				
				OWLObjectProperty p = factory.getOWLObjectProperty(IRI.create(ns+"hasGraph"));
				
				for(IRI iri : iris){
					iri = IRI.create(iri.toString().replace("<", "").replace(">", ""));
					OWLNamedIndividual graph = factory.getOWLNamedIndividual(iri);
					OWLObjectPropertyAssertionAxiom axiom = factory.getOWLObjectPropertyAssertionAxiom(p, storage, graph);
					manager.applyChange(new AddAxiom(ontology, axiom));
				}
				
				return Response.ok(ontology).build();
			} catch (OWLOntologyCreationException e) {
				return Response.status(500).build();
			}
			
			
		}
		
		return Response.status(404).build();
	}
	
	@GET
	@Path("/{graphid:.+}")
	public Response getGraph(@PathParam("graphid") String graphid, @Context UriInfo uriInfo, @Context HttpHeaders headers){
		OntologyStorage ontologyStorage = ontologyStoreProvider.getActiveOntologyStorage();
		
		IRI ontologyID = IRI.create(graphid);
		
		//return Response.ok(tcManager.getMGraph(new UriRef(graphid))).build();
		try {
			return Response.ok(ontologyStorage.getGraph(ontologyID)).build();
		} catch (NoSuchStoreException e) {
			return Response.status(204).build();
		}
		
	}
	
	@POST
	@Consumes(MediaType.MULTIPART_FORM_DATA)
	public Response storeGraph(@FormParam("graph") InputStream graph, @FormParam("id") String id){
		try {
			OWLOntology ontology = OWLManager.createOWLOntologyManager().loadOntologyFromOntologyDocument(graph);
			ontologyStoreProvider.getActiveOntologyStorage().store(ontology, IRI.create(id));
			return Response.ok().build();
		} catch (OWLOntologyCreationException e) {
			return Response.status(500).build();
		}
	}
	
	public String getNamespace(){
		return onManager.getKReSNamespace();
	}
	
	
	public List<String> getStoredGraphs(){
		Set<IRI> iris = ontologyStoreProvider.getActiveOntologyStorage().listGraphs();
		
		ArrayList<String> graphs = new ArrayList<String>();
		for(IRI iri : iris){
			graphs.add(iri.toString());
		}
		return graphs;
	}
}
