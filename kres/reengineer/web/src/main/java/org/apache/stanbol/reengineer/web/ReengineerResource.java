package org.apache.stanbol.reengineer.web;

import java.io.InputStream;
import java.util.Collection;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Consumes;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.apache.clerezza.rdf.core.Literal;
import org.apache.clerezza.rdf.core.LiteralFactory;
import org.apache.clerezza.rdf.core.MGraph;
import org.apache.clerezza.rdf.core.UriRef;
import org.apache.clerezza.rdf.core.access.TcManager;
import org.apache.clerezza.rdf.core.impl.SimpleMGraph;
import org.apache.clerezza.rdf.core.impl.TripleImpl;
import org.apache.stanbol.ontologymanager.ontonet.impl.io.ClerezzaOntologyStorage;
import org.apache.stanbol.reengineer.base.api.DataSource;
import org.apache.stanbol.reengineer.base.api.ReengineeringException;
import org.apache.stanbol.reengineer.base.api.ReengineerManager;
import org.apache.stanbol.reengineer.base.api.Reengineer;
import org.apache.stanbol.reengineer.base.api.datasources.DataSourceFactory;
import org.apache.stanbol.reengineer.base.api.datasources.InvalidDataSourceForTypeSelectedException;
import org.apache.stanbol.reengineer.base.api.datasources.NoSuchDataSourceExpection;
import org.apache.stanbol.reengineer.base.api.datasources.RDB;
import org.apache.stanbol.reengineer.base.api.settings.ConnectionSettings;
import org.apache.stanbol.reengineer.base.api.settings.DBConnectionSettings;
import org.apache.stanbol.reengineer.base.api.util.ReengineerType;
import org.apache.stanbol.reengineer.base.api.util.UnsupportedReengineerException;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLOntology;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.jersey.api.view.ImplicitProduces;
import org.apache.stanbol.kres.jersey.resource.NavigationMixin;


@Path("/reengineer")
@ImplicitProduces("text/html")
public class ReengineerResource extends NavigationMixin {
	


	protected ReengineerManager reengineeringManager;
	protected TcManager tcManager;
	protected ClerezzaOntologyStorage storage;
	
	private final Logger log = LoggerFactory.getLogger(getClass());
	
	
	public ReengineerResource(@Context ServletContext servletContext) {
		tcManager = (TcManager) servletContext.getAttribute(TcManager.class.getName());
		storage = (ClerezzaOntologyStorage) servletContext.getAttribute(ClerezzaOntologyStorage.class.getName());
		reengineeringManager  = (ReengineerManager) (servletContext.getAttribute(ReengineerManager.class.getName()));
		if (reengineeringManager == null) {
            throw new IllegalStateException(
                    "ReengineeringManager missing in ServletContext");
        }
    }
	
	
	@POST
	@Consumes(MediaType.MULTIPART_FORM_DATA)
	public Response reengineering(
			@FormParam("output-graph") String outputGraph,
			@FormParam("input-type") String inputType, 
			@FormParam("input") InputStream input,
			@Context HttpHeaders headers, @Context HttpServletRequest httpServletRequest){
		
		
		System.out.println("Reengineering: "+inputType);
		int reengineerType = -1;
		try {
			reengineerType = ReengineerType.getType(inputType);
		} catch (UnsupportedReengineerException e) {
			Response.status(404).build();
		}
		
		try {
			DataSource dataSource = DataSourceFactory.createDataSource(reengineerType, input);
			
			try {
				OWLOntology ontology;
				System.out.println("STORE PROVIDER : "+storage);
				System.out.println("OUTGRAPH: "+outputGraph);
				String servletPath = httpServletRequest.getLocalAddr();
				System.out.println("SERVER PATH : "+servletPath);
				servletPath = "http://"+servletPath+"/kres/graphs/"+outputGraph+":"+httpServletRequest.getLocalPort();
				if(outputGraph == null || outputGraph.equals("")){
					ontology = reengineeringManager.performReengineering(servletPath, null, dataSource);
					return Response.ok().build();
				}
				else{
					ontology = reengineeringManager.performReengineering(servletPath, IRI.create(outputGraph), dataSource);
					
					storage.store(ontology);
					return Response.ok(ontology).build();
				}
			} catch (ReengineeringException e) {
				e.printStackTrace();
				return Response.status(500).build();
			}
			
		} catch (NoSuchDataSourceExpection e) {
			return Response.status(415).build();
		} catch (InvalidDataSourceForTypeSelectedException e) {
			return Response.status(204).build();
		}
		
	}
	
	
	@POST
	@Consumes(MediaType.MULTIPART_FORM_DATA)
	@Path("/schema")
	public Response schemaReengineering(
			@FormParam("output-graph") String outputGraph,
			@FormParam("input-type") String inputType, 
			@FormParam("input") InputStream input, 
			@Context HttpHeaders headers, 
			@Context HttpServletRequest httpServletRequest){
		
		int reengineerType = -1;
		try {
			reengineerType = ReengineerType.getType(inputType);
		} catch (UnsupportedReengineerException e) {
			Response.status(404).build();
		}
		
		try {
			DataSource dataSource = DataSourceFactory.createDataSource(reengineerType, input);
			
			try {
				OWLOntology ontology;
				
				String servletPath = httpServletRequest.getLocalAddr();
				servletPath = "http://"+servletPath+"/kres/graphs/"+outputGraph+":"+httpServletRequest.getLocalPort();
				if(outputGraph == null){
					ontology = reengineeringManager.performSchemaReengineering(servletPath, null, dataSource);
					return Response.ok().build();
				}
				else{
					ontology = reengineeringManager.performSchemaReengineering(servletPath, IRI.create(outputGraph), dataSource);
					return Response.ok(ontology).build();
				}
			} catch (ReengineeringException e) {
				return Response.status(500).build();
			}
			
		} catch (NoSuchDataSourceExpection e) {
			return Response.status(415).build();
		} catch (InvalidDataSourceForTypeSelectedException e) {
			return Response.status(204).build();
		}
		
	}
	
	
	
	@GET
	@Path("/reengineers")
	public Response listReengineers(@Context HttpHeaders headers){
		Collection<Reengineer> reengineers = reengineeringManager.listReengineers();
		MGraph mGraph = new SimpleMGraph();
		UriRef semionRef = new UriRef("http://semion.kres.iksproject.eu#Semion");
		for(Reengineer semionReengineer : reengineers){
			UriRef hasReengineer = new UriRef("http://semion.kres.iksproject.eu#hasReengineer");
			Literal reenginnerLiteral = LiteralFactory.getInstance().createTypedLiteral(semionReengineer.getClass().getCanonicalName());
			mGraph.add(new TripleImpl(semionRef, hasReengineer, reenginnerLiteral));
		}
		
		return Response.ok(mGraph).build();
	}
	
	
	@GET
	@Path("/reengineers/count")
	public Response countReengineers(@Context HttpHeaders headers){
		
		return Response.ok(reengineeringManager.countReengineers()).build();
	}
	
	
	
	@POST
	@Consumes(MediaType.APPLICATION_FORM_URLENCODED)
	@Path("/db/schema")
	public Response reengineeringDBSchema(  
			@FormParam("output-graph") String outputGraph,
			@FormParam("db") String physicalDBName, @FormParam("jdbc") String jdbcDriver,
			@FormParam("protocol") String protocol, @FormParam("host") String host,
			@FormParam("port") String port, @FormParam("username") String username, @FormParam("password") String password,
			@Context HttpHeaders headers, 
			@Context HttpServletRequest httpServletRequest){
		
		
		log.info("There are " + tcManager.listMGraphs().size() + " mGraphs");
		System.out.println("There are " + tcManager.listMGraphs().size() + " mGraphs");
		
		//UriRef uri = ContentItemHelper.makeDefaultUri(databaseURI, databaseURI.getBytes());
		ConnectionSettings connectionSettings = new DBConnectionSettings(protocol, host, port, physicalDBName, username, password, null, jdbcDriver);
		DataSource dataSource = new RDB(connectionSettings);
		
		String servletPath = httpServletRequest.getLocalAddr();
		servletPath = "http://"+servletPath+"/kres/graphs/"+outputGraph+":"+httpServletRequest.getLocalPort();
		
		if(outputGraph != null && !outputGraph.equals("")){
			OWLOntology ontology;
			try {
				ontology = reengineeringManager.performSchemaReengineering(servletPath, IRI.create(outputGraph), dataSource);
				/*MediaType mediaType = headers.getMediaType();
				String res = OntologyRenderUtils.renderOntology(ontology, mediaType.getType());*/
				return Response.ok(ontology).build();
			} catch (ReengineeringException e) {
				return Response.status(500).build();
			}
		}
		else{
			try {
				reengineeringManager.performSchemaReengineering(servletPath, null, dataSource);
				return Response.ok().build();
			} catch (ReengineeringException e) {
				return Response.status(500).build();
			}
		}
		
		
	}
	
	
	@POST
	@Consumes(MediaType.APPLICATION_FORM_URLENCODED)
	@Path("/db")
	public Response reengineeringDB(  
			@QueryParam("db") String physicalDBName, @QueryParam("jdbc") String jdbcDriver,
			@QueryParam("protocol") String protocol, @QueryParam("host") String host,
			@QueryParam("port") String port, @QueryParam("username") String username, @QueryParam("password") String password,
			@QueryParam("output-graph") String outputGraph, 
			@Context HttpHeaders headers,
			@Context HttpServletRequest httpServletRequest){
		
		
		log.info("There are " + tcManager.listMGraphs().size() + " mGraphs");
		System.out.println("There are " + tcManager.listMGraphs().size() + " mGraphs");
		
		//UriRef uri = ContentItemHelper.makeDefaultUri(databaseURI, databaseURI.getBytes());
		ConnectionSettings connectionSettings = new DBConnectionSettings(protocol, host, port, physicalDBName, username, password, null, jdbcDriver);
		DataSource dataSource = new RDB(connectionSettings);
		
		String servletPath = httpServletRequest.getLocalAddr();
		servletPath = "http://"+servletPath+"/kres/graphs/"+outputGraph+":"+httpServletRequest.getLocalPort();
		
		if(outputGraph != null && !outputGraph.equals("")){
			OWLOntology ontology;
			try {
				ontology = reengineeringManager.performReengineering(servletPath, IRI.create(outputGraph), dataSource);
				return Response.ok(ontology).build();
			} catch (ReengineeringException e) {
				return Response.status(500).build();
			}
			
		}
		else{
			try {
				reengineeringManager.performReengineering(servletPath, null, dataSource);
				return Response.ok().build();
			} catch (ReengineeringException e) {
				return Response.status(500).build();
			}
		}
		
		
	}
	
	
	

}