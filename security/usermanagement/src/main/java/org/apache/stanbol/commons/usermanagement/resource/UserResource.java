package org.apache.stanbol.commons.usermanagement.resource;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.locks.Lock;

import javax.ws.rs.DefaultValue;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.CacheControl;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;
import javax.ws.rs.core.UriInfo;

import org.apache.clerezza.platform.config.SystemConfig;
import org.apache.clerezza.rdf.core.Graph;
import org.apache.clerezza.rdf.core.Literal;
import org.apache.clerezza.rdf.core.LiteralFactory;
import org.apache.clerezza.rdf.core.Triple;
import org.apache.clerezza.rdf.core.UriRef;
import org.apache.clerezza.rdf.core.access.LockableMGraph;
import org.apache.clerezza.rdf.core.impl.PlainLiteralImpl;
import org.apache.clerezza.rdf.core.serializedform.Parser;
import org.apache.clerezza.rdf.core.serializedform.Serializer;
import org.apache.clerezza.rdf.core.serializedform.SupportedFormat;
import org.apache.clerezza.rdf.ontologies.FOAF;
import org.apache.clerezza.rdf.ontologies.PLATFORM;
import org.apache.clerezza.rdf.ontologies.RDF;
import org.apache.clerezza.rdf.utils.GraphNode;
import org.apache.clerezza.rdf.utils.MGraphUtils;
import org.apache.clerezza.rdf.utils.MGraphUtils.NoSuchSubGraphException;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.clerezza.rdf.ontologies.PERMISSION;
import org.apache.stanbol.commons.security.PasswordUtil;
import org.apache.stanbol.commons.viewable.RdfViewable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component
@Service(UserResource.class)
@Path("user-management")
public class UserResource {
	
	private static Logger log = LoggerFactory.getLogger(UserResource.class);
	
	@Reference(target = SystemConfig.SYSTEM_GRAPH_FILTER)
	private LockableMGraph systemGraph;
	
	@Reference
	private Parser parser;
	
	@Reference
	private Serializer serializer;
	
	@GET
	public String index() throws UnsupportedEncodingException {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		serializer.serialize(baos, systemGraph, SupportedFormat.TURTLE);
		String serialized = new String(baos.toByteArray(),"utf-8");
		return serialized;
	}
        
        @GET
        @Path("users")
        public RdfViewable listUsers(){
            return new RdfViewable("listUser.ftl", getUserType(), this.getClass());
        }
        
	@GET
	@Path("user/{username}")
	public RdfViewable editUser(@PathParam("username") String userName) {
            return new RdfViewable("editUser.ftl", getUser(userName), this.getClass());
	}
	
	@GET
	@Path("view-user") 
	public RdfViewable viewUser(@QueryParam("userName") String userName) {
		return new RdfViewable("edit.ftl", getUser(userName), this.getClass());
	}
	
	/**
	 * takes edit form data and pushes into store
	 * so far only password change implemented
	 * (others should be straightforward delete/inserts)
	 */
	@POST
	@Path("store-user")
	public Response storeUser(@Context UriInfo uriInfo, @FormParam("userName") String userName, @FormParam("email") String email,
			@FormParam("password") String password, @FormParam("permission[]") List<String> permission) {

        GraphNode userNode = getUser(userName);
        
		String passwordSha1 = PasswordUtil.convertPassword(password);

        // System.out.println("new password = "+password);
        // System.out.println("new passwordSha1 = "+passwordSha1);
        
        System.out.println("BEFORE ========================================================");
        serializer.serialize(System.out, userNode.getGraph(), SupportedFormat.TURTLE);
            
        Iterator<Literal> oldPasswordsSha1 = userNode.getLiterals(PERMISSION.passwordSha1);
        Literal oldPasswordSha1 = oldPasswordsSha1.next(); 
        // no exception, if there is no value, let it break totally, if more than one - it is broken elsewhere
        
        userNode.addPropertyValue(PERMISSION.passwordSha1, passwordSha1);
        // workaround for possible issue in verification re. PlainLiteral vs. xsd:string 
        // userNode.addProperty(PERMISSION.passwordSha1, new PlainLiteralImpl(passwordSha1));
        // most likely not a problem, and the above will work
        
        userNode.deleteProperty(PERMISSION.passwordSha1, oldPasswordSha1);

        System.out.println("AFTER ========================================================");
        serializer.serialize(System.out, userNode.getGraph(), SupportedFormat.TURTLE);
        
        URI pageUri = uriInfo.getBaseUriBuilder().path("/system/console").build();
        
        // header Cache-control: no-cache, just in case intermediaries are holding onto old stuff
        CacheControl cc = new CacheControl();
        cc.setNoCache(true); 
        
        // see other my not be the best response, but does seem the best given the jax-rs things available
        return Response.seeOther(pageUri).cacheControl(cc).build();
	}
	
	
	
	/**
	 * replaces the subgraph serialized with RDF/XML in <code>revokedString
	 * </code> with the one from <code>assertedString</code>.
	 *
	 * @param graphUri the graph within which the replacement has to take place or null
	 * for the content graph
	 * @param assertedString the asserted Graph
	 * @param revokedString the revoked Graph
	 * @param format the media-type of the rdf format in which the asserted and revoked graph are serialized, default: text/turtle
	 */
	@POST
	@Path("replace-subgraph")
	public void replaceSubGraph(@QueryParam("graph") UriRef graphUri,
			@FormParam("assert") String assertedString,
			@FormParam("revoke") String revokedString, 
			@FormParam("format") @DefaultValue("text/turtle") String format) {
		final Graph assertedGraph;
		final Graph revokedGraph;
		try {
			assertedGraph = parser.parse(new ByteArrayInputStream(assertedString.getBytes("utf-8")), 
					format);
			revokedGraph = parser.parse(new ByteArrayInputStream(assertedString.getBytes("utf-8")), 
					format);
		} catch (IOException ex) {
			log.error("reading graph {}", ex);
			throw new WebApplicationException(ex, 500);
		}
		try {
			MGraphUtils.removeSubGraph(systemGraph, revokedGraph);
		} catch (NoSuchSubGraphException ex) {
			throw new RuntimeException(ex);
		}
		systemGraph.addAll(assertedGraph);
	}

	
	private GraphNode getUser(@QueryParam("userName") String userName) {
		Iterator<Triple> iter = systemGraph.filter(null, PLATFORM.userName, new PlainLiteralImpl(userName));
		if (!iter.hasNext()) {
			return null;
		}
		return new GraphNode(iter.next().getSubject(), systemGraph);
	}
	
	public GraphNode getUserType() {
		return new GraphNode(FOAF.Agent, systemGraph);
	}
	
	public Set<GraphNode> getUsers() {
		return getResourcesOfType(FOAF.Agent);
	}
	
	private Set<GraphNode> getResourcesOfType(UriRef type) {
		Lock readLock = systemGraph.getLock().readLock();
		readLock.lock();
		try {
			final Iterator<Triple> triples = systemGraph.filter(null, RDF.type, type);
			Set<GraphNode> userRoles = new HashSet<GraphNode>();
			while (triples.hasNext()) {
				userRoles.add(new GraphNode(triples.next().getSubject(), systemGraph));
			}
			return userRoles;
		} finally {
			readLock.unlock();
		}
	}

}
