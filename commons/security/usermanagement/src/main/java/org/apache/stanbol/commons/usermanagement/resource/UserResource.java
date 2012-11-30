package org.apache.stanbol.commons.usermanagement.resource;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.locks.Lock;

import javax.ws.rs.Consumes;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
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
import org.apache.clerezza.rdf.core.NonLiteral;
import org.apache.clerezza.rdf.core.Triple;
import org.apache.clerezza.rdf.core.Resource;
import org.apache.clerezza.rdf.core.TripleCollection;
import org.apache.clerezza.rdf.core.UriRef;
import org.apache.clerezza.rdf.core.access.LockableMGraph;
import org.apache.clerezza.rdf.core.impl.PlainLiteralImpl;
import org.apache.clerezza.rdf.core.impl.SimpleGraph;
import org.apache.clerezza.rdf.core.impl.TripleImpl;
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
import org.apache.stanbol.commons.usermanagement.Ontology;
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
		String serialized = new String(baos.toByteArray(), "utf-8");
		return serialized;
	}

        
        @GET
        @Path("users")
        @Produces("text/html")
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
    @Produces("text/html")
	public RdfViewable viewUser(@QueryParam("userName") String userName) {
		return new RdfViewable("edit.ftl", getUser(userName), this.getClass());
	}

	/**
	 * takes edit form data and pushes into store "" values are ignored
	 */
	@POST
	@Path("store-user")
	// @Consumes("multipart/form-data")
	@Consumes("application/x-www-form-urlencoded")
	public Response storeUser(@Context UriInfo uriInfo,
			@FormParam("currentUserName") String currentUserName,
			@FormParam("newUserName") String newUserName,
			@FormParam("fullName") String fullName,
			@FormParam("email") String email,
			@FormParam("password") String password,
			@FormParam("permission[]") List<String> permission) {

		GraphNode userNode = getUser(currentUserName);

		System.out
				.println("BEFORE ========================================================");
		// serializer.serialize(System.out, userNode.getNodeContext(),
		// SupportedFormat.TURTLE);
		serializeTriplesWithSubject(System.out, userNode);

		if (!newUserName.equals("")) {
			changeLiteral(userNode, PLATFORM.userName, newUserName);
		}
		if (!fullName.equals("")) {
			changeLiteral(userNode, FOAF.name, fullName);
		}
		if (!password.equals("")) {
			String passwordSha1 = PasswordUtil.convertPassword(password);
			changeLiteral(userNode, PERMISSION.passwordSha1, passwordSha1);
		}

		if (!email.equals("")) {
			changeResource(userNode, FOAF.mbox, new UriRef("mailto:" + email));
		}

		System.out
				.println("AFTER ========================================================");
		// serializer.serialize(System.out, userNode.getNodeContext(),
		// SupportedFormat.TURTLE);
		serializeTriplesWithSubject(System.out, userNode);

		System.out
				.println("^^^^ ========================================================");

		URI pageUri = uriInfo.getBaseUriBuilder()
				.path("system/console/usermanagement").build();

		// header Cache-control: no-cache, just in case intermediaries are
		// holding onto old stuff
		CacheControl cc = new CacheControl();
		cc.setNoCache(true);

		// see other my not be the best response, but does seem the best given
		// the jax-rs things available
		return Response.seeOther(pageUri).cacheControl(cc).build();
	}

	/**
	 * replaces the subgraph serialized with RDF/XML in <code>revokedString
	 * </code> with the one from <code>assertedString</code>.
	 * 
	 * @param graphUri
	 *            the graph within which the replacement has to take place or
	 *            null for the content graph
	 * @param assertedString
	 *            the asserted Graph
	 * @param revokedString
	 *            the revoked Graph
	 * @param format
	 *            the media-type of the rdf format in which the asserted and
	 *            revoked graph are serialized, default: text/turtle
	 */
	@POST
	@Path("replace-subgraph")
	@Consumes("multipart/form-data")
	public void replaceSubGraph(@QueryParam("graph") UriRef graphUri,
			@FormParam("assert") String assertedString,
			@FormParam("revoke") String revokedString,
			@FormParam("format") @DefaultValue("text/turtle") String format) {
		final Graph assertedGraph;
		final Graph revokedGraph;
		try {
			assertedGraph = parser.parse(new ByteArrayInputStream(
					assertedString.getBytes("utf-8")), format);
			revokedGraph = parser.parse(
					new ByteArrayInputStream(assertedString.getBytes("utf-8")),
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

	/**
	 * Endpoint-style user creation takes a little bunch of Turtle e.g. [] a
	 * foaf:Agent ; cz:userName "Hugo Ball" .
	 * 
	 * @param userData
	 * @return HTTP/1.1 204 No Content
	 */
	@POST
	@Consumes("text/turtle")
	@Path("add-user")
	public Response addUser(String userData) {

		log.debug(("addUser called with " + userData));

		Graph inputGraph = readData(userData);

		Iterator<Triple> agents = inputGraph.filter(null, null, FOAF.Agent);

		NonLiteral userNode = agents.next().getSubject();
		Iterator<Triple> userTriples = inputGraph.filter(userNode, null, null);
		while (userTriples.hasNext()) {
			Triple userTriple = userTriples.next();
			systemGraph.add(userTriple);
		}

		// it's not actually creating a resource at this URI so this
		// seems the most appropriate response
		return Response.noContent().build();
	}

	/**
	 * Endpoint-style user deletion takes a little bunch of Turtle e.g. [] a
	 * foaf:Agent ; cz:userName "Hugo Ball" .
	 * 
	 * @param userData
	 * @return HTTP/1.1 204 No Content
	 */
	@POST
	@Consumes("text/turtle")
	@Path("delete-user")
	public Response deleteUser(String userData) {

		log.debug("deleteUser called with " + userData);

		Graph inputGraph = readData(userData);

		Iterator<Triple> userNameTriples = inputGraph.filter(null,
				PLATFORM.userName, null);

		Literal userNameNode = (Literal) userNameTriples.next().getObject();

		Iterator<Triple> userTriples = systemGraph.filter(null, null,
				userNameNode);

		Triple userTriple = userTriples.next();
		Iterator<Triple> systemUserTriples = systemGraph.filter(
				userTriple.getSubject(), null, null);

		// gives concurrent mod exception otherwise
		ArrayList<Triple> tripleBuffer = new ArrayList<Triple>();
		while (systemUserTriples.hasNext()) {
			tripleBuffer.add(systemUserTriples.next());
		}
		systemGraph.removeAll(tripleBuffer);

		// it's not actually creating a resource at this URI so this
		// seems the most appropriate response
		return Response.noContent().build();
	}

	@POST
	@Consumes("text/turtle")
	@Path("change-user")
	public Response changeUser(String userData) {

		log.debug("changeUser called with " + userData);

		Graph inputGraph = readData(userData);

		Iterator<Triple> changes = inputGraph.filter(null, null,
				Ontology.Change);

		while (changes.hasNext()) {
			Triple changeTriple = changes.next();

			NonLiteral changeNode = changeTriple.getSubject();

			Literal userName = (Literal) inputGraph
					.filter(changeNode, PLATFORM.userName, null).next()
					.getObject();

			NonLiteral userNode = (NonLiteral) systemGraph
					.filter(null, PLATFORM.userName, userName).next()
					.getSubject();

			UriRef predicateUriRef = (UriRef) inputGraph
					.filter(changeNode, Ontology.predicate, null).next()
					.getObject();

			System.out.println("predicateUriRef = " + predicateUriRef);

			// handle old value (if it exists)
			Iterator<Triple> iterator = inputGraph.filter(changeNode,
					Ontology.oldValue, null);
			Resource oldValue = null;

			if (iterator.hasNext()) {
				oldValue = iterator.next().getObject();

				// Triple oldTriple = systemGraph.filter(null, predicateUriRef,
				// oldValue).next();
				Triple oldTriple = systemGraph.filter(userNode,
						predicateUriRef, oldValue).next();

				systemGraph.remove(oldTriple);
			}

			Resource newValue = inputGraph
					.filter(changeNode, Ontology.newValue, null).next()
					.getObject();

			Triple newTriple = new TripleImpl(userNode, predicateUriRef,
					newValue);

			systemGraph.add(newTriple);
		}

		// it's not actually creating a resource at this URI so this
		// seems the most appropriate response
		return Response.noContent().build();
	}

	/**
	 * RESTful access to individual user data
	 * 
	 * @param userName
	 * @return context graph for user
	 * @throws UnsupportedEncodingException
	 */
	@GET
	@Path("user/{username}")
	@Produces("text/turtle")
	public Response getUserTurtle(@PathParam("username") String userName)
			throws UnsupportedEncodingException {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();

		serializer.serialize(baos, getUser(userName).getNodeContext(),
				SupportedFormat.TURTLE);
		String serialized = new String(baos.toByteArray(), "utf-8");
		// System.out.println("User = "+serialized);
		return Response.ok(serialized).build();
	}

	// ///////////////////////////////////////////////////////////////////////
	// helper methods

	/**
	 * Replaces/inserts literal value for predicate assumes there is only one
	 * triple for the given predicate new value is added before deleting old one
	 * in case user is modifying their own data in which case they need triples
	 * in place for rights etc.
	 * 
	 * @param userNode
	 *            node in systemGraph corresponding to the user to change
	 * @param predicate
	 *            property of the triple to change
	 * @param newValue
	 *            new value for given predicate
	 */
	private void changeLiteral(GraphNode userNode, UriRef predicate,
			String newValue) {

		Iterator<Triple> oldTriples = systemGraph.filter(
				(NonLiteral) userNode.getNode(), predicate, null);

		ArrayList<Triple> oldBuffer = new ArrayList<Triple>();

		// System.out.println("\n\n");

		while (oldTriples.hasNext()) {
			Triple triple = oldTriples.next();

			// System.out.println("*** old triple = "+triple);

			// shouldn't be necessary, but just to catch the edge case where the
			// triple has the wrong type of subject
			// try {
			// // check for newValue == oldValue
			// Literal oldLiteral = (Literal) triple.getObject();
			// if (newValue.equals(oldLiteral.getLexicalForm())) {
			// return;
			// }
			// } catch (Exception e) {
			//
			// UriRef oldResource = (UriRef) triple.getObject();
			//
			// // check for newValue == oldValue
			// if (newValue.equals(oldResource)) {
			// return;
			// }
			// }

			Resource oldValue = triple.getObject();
			if (newValue.equals(oldValue)) {
				return;
			}

			oldBuffer.add(triple);
		}

		// filter appears to see plain literals and xsd:strings as differerent
		// so not
		// userNode.addPropertyValue(predicate, newValue);
		userNode.addProperty(predicate, new PlainLiteralImpl(newValue));

		// System.out.println("*** systemGraph size before removal = "+systemGraph.size());
		systemGraph.removeAll(oldBuffer);
		// System.out.println("*** systemGraph size after removal = "+systemGraph.size());
	}

	/**
	 * Replaces/inserts resource value for predicate assumes there is only one
	 * triple for the given predicate
	 * 
	 * @param userNode
	 *            node in systemGraph corresponding to the user to change
	 * @param predicate
	 *            property of the triple to change
	 * @param newValue
	 *            new value for given predicate
	 */
	private void changeResource(GraphNode userNode, UriRef predicate,
			UriRef newValue) {

		Iterator<Triple> oldTriples = systemGraph.filter(
				(NonLiteral) userNode.getNode(), predicate, null);

		ArrayList<Triple> oldBuffer = new ArrayList<Triple>();

		// System.out.println("\n\n");

		while (oldTriples.hasNext()) {
			Triple triple = oldTriples.next();

			System.out.println("*** old triple = " + triple);

			// try {
			// UriRef oldResource = (UriRef) triple.getObject();
			//
			// // check for newValue == oldValue
			// if (newValue.equals(oldResource)) {
			// return;
			// }
			// } catch (Exception e) {
			//
			// Literal oldLiteral = (Literal) triple.getObject();
			// if (newValue.equals(oldLiteral.getLexicalForm())) {
			// return;
			// }
			// }

			Resource oldValue = triple.getObject();
			if (newValue.equals(oldValue)) {
				return;
			}

			oldBuffer.add(triple);
		}

		// filter appears to see plain literals and xsd:strings as differerent
		// so not
		// userNode.addPropertyValue(predicate, newValue);
		userNode.addProperty(predicate, newValue);

		// System.out.println("*** systemGraph size before removal = "+systemGraph.size());
		systemGraph.removeAll(oldBuffer);
		// System.out.println("*** systemGraph size after removal = "+systemGraph.size());
	}

	private void serializeTriplesWithSubject(OutputStream stream, GraphNode node) {
		serializer.serialize(stream, getTriplesWithSubject(node),
				SupportedFormat.TURTLE);
	}

	private TripleCollection getTriplesWithSubject(GraphNode node) {
		TripleCollection containerGraph = node.getGraph();
		return new SimpleGraph(containerGraph.filter(
				(NonLiteral) node.getNode(), null, null));
	}

	/**
	 * Read string into graph
	 * 
	 * @param data
	 *            Turtle string
	 * @return graph from Turtle
	 */
	private Graph readData(String data) {

		Graph inputGraph;

		try {
			inputGraph = parser.parse(
					new ByteArrayInputStream(data.getBytes("utf-8")),
					"text/turtle");
		} catch (IOException ex) {
			log.error("parsing error with userData", ex);
			throw new WebApplicationException(ex, 500);
		}
		// System.out.println("inputGraph.size() = "+inputGraph.size());
		//
		// Object[] stuff = inputGraph.toArray();
		// for(int i=0;i<stuff.length;i++){
		// System.out.println("as array - "+stuff[i]);
		// }
		return inputGraph;
	}

	private GraphNode getUser(@QueryParam("userName") String userName) {
		return getNamedUser(userName);
	}

	private GraphNode getNamedUser(String userName) {

		// System.out.println("getting named user = "+userName);

		Iterator<Triple> iter = systemGraph.filter(null, PLATFORM.userName,
				new PlainLiteralImpl(userName));
		if (!iter.hasNext()) {
			System.out.println("named user not found " + userName);
			// System.out.println("\n\n\n");

			return null;
		}
		return new GraphNode(iter.next().getSubject(), systemGraph);
	}

	// private GraphNode getNamedUser(Resource nameResource) {
	// Iterator<Triple> iter = systemGraph.filter(null, PLATFORM.userName,
	// nameResource);
	// if (!iter.hasNext()) {
	// return null;
	// }
	// return new GraphNode(iter.next().getSubject(), systemGraph);
	// }

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
			final Iterator<Triple> triples = systemGraph.filter(null, RDF.type,
					type);
			Set<GraphNode> userRoles = new HashSet<GraphNode>();
			while (triples.hasNext()) {
				userRoles.add(new GraphNode(triples.next().getSubject(),
						systemGraph));
			}
			return userRoles;
		} finally {
			readLock.unlock();
		}
	}

}
