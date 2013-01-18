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
package org.apache.stanbol.commons.usermanagement.resource;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
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
import javax.ws.rs.core.UriInfo;

import org.apache.clerezza.platform.config.SystemConfig;
import org.apache.clerezza.rdf.core.BNode;
import org.apache.clerezza.rdf.core.Graph;
import org.apache.clerezza.rdf.core.Literal;
import org.apache.clerezza.rdf.core.NonLiteral;
import org.apache.clerezza.rdf.core.PlainLiteral;
import org.apache.clerezza.rdf.core.Triple;
import org.apache.clerezza.rdf.core.Resource;
import org.apache.clerezza.rdf.core.TripleCollection;
import org.apache.clerezza.rdf.core.UriRef;
import org.apache.clerezza.rdf.core.access.LockableMGraph;
import org.apache.clerezza.rdf.core.impl.PlainLiteralImpl;
import org.apache.clerezza.rdf.core.impl.SimpleGraph;
import org.apache.clerezza.rdf.core.impl.SimpleMGraph;
import org.apache.clerezza.rdf.core.impl.TripleImpl;
import org.apache.clerezza.rdf.core.serializedform.Parser;
import org.apache.clerezza.rdf.core.serializedform.Serializer;
import org.apache.clerezza.rdf.core.serializedform.SupportedFormat;
import org.apache.clerezza.rdf.ontologies.DC;
import org.apache.clerezza.rdf.ontologies.FOAF;
import org.apache.clerezza.rdf.ontologies.PLATFORM;
import org.apache.clerezza.rdf.ontologies.RDF;
import org.apache.clerezza.rdf.ontologies.SIOC;
import org.apache.clerezza.rdf.utils.GraphNode;
import org.apache.clerezza.rdf.utils.MGraphUtils;
import org.apache.clerezza.rdf.utils.MGraphUtils.NoSuchSubGraphException;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.clerezza.rdf.ontologies.PERMISSION;
import org.apache.stanbol.commons.ldpathtemplate.LdRenderer;
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
    @Reference
    private LdRenderer ldRenderer;

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
    public RdfViewable listUsers() {
        return new RdfViewable("listUser.ftl", getUserType(), this.getClass());
    }

    public GraphNode getUserType() {
        return new GraphNode(FOAF.Agent, systemGraph);
    }

    @GET
    @Path("user/{username}")
    public RdfViewable editUser(@PathParam("username") String userName) {
        return new RdfViewable("editUser.ftl", getUser(userName),
                this.getClass());
    }
    private static GraphNode dummyNode;

    static {
        dummyNode = new GraphNode(new BNode(), new SimpleMGraph());
        dummyNode.addProperty(RDF.type, FOAF.Agent);
    }

    @GET
    @Path("create-form")
    public RdfViewable getCreateUserForm(@Context UriInfo uriInfo) {
        return new RdfViewable("editUser.ftl", dummyNode,
                this.getClass());
    }

    @GET
    @Path("view-user")
    @Produces("text/html")
    public RdfViewable viewUser(@QueryParam("userName") String userName) {
        return new RdfViewable("edit.ftl", getUser(userName), this.getClass());
    }

    @POST
    @Path("store-user")
    // @Consumes("multipart/form-data")
    @Consumes("application/x-www-form-urlencoded")
    public Response storeUser(@Context UriInfo uriInfo,
            @FormParam("currentLogin") String currentLogin,
            @FormParam("newLogin") String newLogin,
            @FormParam("fullName") String fullName,
            @FormParam("email") String email,
            @FormParam("password") String password,
            @FormParam("roles") List<String> roles,
            @FormParam("permissions") List<String> permissions) {

        GraphNode userNode;

        if(currentLogin != null){ // 
            currentLogin = currentLogin.trim();
        }
        
        // System.out.println("CURRENTUSERNAME = ["+currentUserName+"]");
        if (currentLogin != null && !currentLogin.equals("")) {
            userNode = getUser(currentLogin);
            return store(userNode, uriInfo, currentLogin, newLogin, fullName, email, password, roles, permissions);
        }
        
//        try {
//             userNode = getUser(newLogin);
//        } catch(Exception e) {
            userNode = createUser(newLogin);
      //  }
        // System.out.println("NEWLOGIN = [" + newLogin + "]");
       
        return store(userNode, uriInfo, newLogin, newLogin, fullName, email, password, roles, permissions);
    }

    /**
     * takes edit form data and pushes into store "" values are ignored
     */
    private Response store(GraphNode userNode, UriInfo uriInfo,
            String currentUserName,
            String newUserName,
            String fullName,
            String email,
            String password,
            List<String> roles,
            List<String> permissions) {

        //   GraphNode userNode = getUser(currentUserName);

        // System.out.println("currentUserName = " + currentUserName);
//        System.out
//                .println("BEFORE ========================================================");
//        // serializeTriplesWithSubject(System.out, userNode);
//        serializer.serialize(System.out, systemGraph, SupportedFormat.TURTLE);

        if (newUserName != null && !newUserName.equals("")) {
            changeLiteral(userNode, PLATFORM.userName, newUserName);
        }
        if (fullName != null && !fullName.equals("")) {
            changeLiteral(userNode, FOAF.name, fullName);
        }
        if (password != null && !password.equals("")) {
            String passwordSha1 = PasswordUtil.convertPassword(password);
            changeLiteral(userNode, PERMISSION.passwordSha1, passwordSha1);
        }
        if (email != null && !email.equals("")) {
            changeResource(userNode, FOAF.mbox, new UriRef("mailto:" + email));
        }

        NonLiteral userResource = (NonLiteral) userNode.getNode();

        if (roles != null) {
            clearRoles(userResource);
            Lock writeLock = systemGraph.getLock().writeLock();
            writeLock.lock();
            try {
                for (int i = 0; i < roles.size(); i++) {
                    roles.set(i, roles.get(i).trim());
                    if (!roles.get(i).equals("")) {
                        addRole(userNode, roles.get(i));
                    }
                }
            } finally {
                writeLock.unlock();
            }
        }
        if (permissions != null) {
            clearPermissions(userResource);
            Lock writeLock = systemGraph.getLock().writeLock();
            writeLock.lock();
            try {
                for (int i = 0; i < permissions.size(); i++) {
                    permissions.set(i, permissions.get(i).trim());
                    if (!permissions.get(i).equals("")) {
                        addPermission(userNode, permissions.get(i));
                    }
                }
            } finally {
                writeLock.unlock();
            }
        }

        //  System.out.println("AFTER ========================================================");
//        serializeTriplesWithSubject(System.out, userNode);
//        serializer.serialize(System.out, systemGraph, SupportedFormat.TURTLE);
//        System.out
//                .println("^^^^ ========================================================");

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
     * NOT CURRENTLY IN USE replaces the subgraph
     * <code>revokedString
     * </code> with the one from
     * <code>assertedString</code>.
     *
     * @param graphUri the graph within which the replacement has to take place
     * or null for the content graph
     * @param assertedString the asserted Graph
     * @param revokedString the revoked Graph
     * @param format the media-type of the rdf format in which the asserted and
     * revoked graph are serialized, default: text/turtle
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

    @GET
    @Path("roles")
    @Produces("text/html")
    public RdfViewable listRoles() {
        return new RdfViewable("listRole.ftl", getRoleType(), this.getClass());
    }

    @GET
    @Path("permissions")
    @Produces("text/html")
    public RdfViewable listPermissions() {
        addClassToPermissions();
        return new RdfViewable("listPermission.ftl", getPermissionType(), this.getClass());
    }

    @GET
    @Path("user/{username}/permissionsCheckboxes")
    @Produces("text/html")
    public RdfViewable permissionsCheckboxes(@PathParam("username") String userName) { //getUser(userName)
        return new RdfViewable("permissionsCheckboxes.ftl", getUser(userName), this.getClass());
    }

    public GraphNode getPermissionType() {
        return new GraphNode(PERMISSION.Permission,
                systemGraph);
    }

    /**
     * a kludge - initially the permissions aren't expressed as instances of
     * Permission class, this adds the relevant triples
     */
    private void addClassToPermissions() {
        Iterator<Triple> permissionTriples = systemGraph.filter(null, PERMISSION.hasPermission, null);

        ArrayList<GraphNode> buffer = new ArrayList<GraphNode>();

        Lock readLock = systemGraph.getLock().readLock();
        readLock.lock();
        try {
            while (permissionTriples.hasNext()) {
                Triple triple = permissionTriples.next();
                Resource permissionResource = triple.getObject();
                buffer.add(new GraphNode(permissionResource, systemGraph));
            }
        } finally {
            readLock.unlock();
        }

        Lock writeLock = systemGraph.getLock().writeLock();
        writeLock.lock();
        try {
            for (int i = 0; i < buffer.size(); i++) {
                buffer.get(i).addProperty(RDF.type, PERMISSION.Permission);
            }
        } finally {
            writeLock.unlock();
        }
    }

    @GET
    @Path("rolesCheckboxes")
    @Produces("text/html")
    public RdfViewable rolesCheckboxes() {
        return new RdfViewable("rolesCheckboxes.ftl", getRoleType(), this.getClass());
    }

    public GraphNode getRoleType() {
        return new GraphNode(PERMISSION.Role,
                systemGraph);
    }

    @POST
    @Path("delete")
    public void removeUser(@FormParam("user") String userName) {
        // System.out.println("DELETE " + userName);
        Resource userResource = getNamedUser(userName).getNode();
        Iterator<Triple> userTriples = systemGraph.filter((NonLiteral) userResource, null, null);

        ArrayList<Triple> buffer = new ArrayList<Triple>();

        Lock readLock = systemGraph.getLock().readLock();
        readLock.lock();
        try {
            while (userTriples.hasNext()) {
                Triple triple = userTriples.next();
                buffer.add(triple);
            }
        } finally {
            readLock.unlock();
        }

        // Graph context = getNamedUser(userName).getNodeContext();
        Lock writeLock = systemGraph.getLock().writeLock();
        writeLock.lock();
        try {
            systemGraph.removeAll(buffer);
        } finally {
            writeLock.unlock();
        }
    }

    /**
     * ********************
     * Endpoint API ********************
     */
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


        Lock writeLock = systemGraph.getLock().writeLock();
        writeLock.lock();
        try {
            while (userTriples.hasNext()) {
                Triple userTriple = userTriples.next();
                systemGraph.add(userTriple);
            }
        } finally {
            writeLock.unlock();
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

        // gives concurrent mod exception otherwise
        ArrayList<Triple> tripleBuffer = new ArrayList<Triple>();
        Lock readLock = systemGraph.getLock().readLock();
        readLock.lock();
        try {
            Iterator<Triple> userTriples = systemGraph.filter(null, null,
                    userNameNode);

            Triple userTriple = userTriples.next();
            Iterator<Triple> systemUserTriples = systemGraph.filter(
                    userTriple.getSubject(), null, null);


            while (systemUserTriples.hasNext()) {
                tripleBuffer.add(systemUserTriples.next());
            }
        } finally {
            readLock.unlock();
        }

        systemGraph.removeAll(tripleBuffer);

        // it's not actually creating a resource at this URI so this
        // seems the most appropriate response
        return Response.noContent().build();
    }

    // needs refactoring and locks adding
    @POST
    @Consumes("text/turtle")
    @Path("change-user")
    public Response changeUser(String userData) {

        log.debug("changeUser called with " + userData);

        Graph inputGraph = readData(userData);

        Lock readLock = systemGraph.getLock().readLock();
        readLock.lock();
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

            // System.out.println("predicateUriRef = " + predicateUriRef);

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
    @Path("users/{username}")
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

    /// LOCKS MAYBE OK TO HERE
    /**
     * RESTful access to user roles (and permissions right now - may change)
     *
     * @param userName
     * @return context graph for user
     * @throws UnsupportedEncodingException
     */
    @GET
    @Path("roles/{username}")
    @Produces("text/turtle")
    public Response getUserRoles(@PathParam("username") String userName)
            throws UnsupportedEncodingException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        GraphNode userNode = getUser(userName);

        Iterator<Resource> functionIterator = userNode
                .getObjects(SIOC.has_function);

        SimpleMGraph rolesGraph = new SimpleMGraph();

        while (functionIterator.hasNext()) {

            GraphNode functionNode = new GraphNode(functionIterator.next(),
                    systemGraph);

            Iterator<Triple> roleIterator = systemGraph.filter(
                    (NonLiteral) functionNode.getNode(), RDF.type,
                    PERMISSION.Role);

            // needs lock?
            while (roleIterator.hasNext()) {
                Triple roleTriple = roleIterator.next();
                // rolesGraph.add(roleTriple);
                NonLiteral roleNode = roleTriple.getSubject();
                SimpleGraph detailsGraph = new SimpleGraph(systemGraph.filter(
                        roleNode, null, null));
                rolesGraph.addAll(detailsGraph);
            }
        }
        // case of no roles not handled - what best to return : empty graph or
        // 404?
        serializer.serialize(baos, rolesGraph, SupportedFormat.TURTLE);
        String serialized = new String(baos.toByteArray(), "utf-8");
        return Response.ok(serialized).build();
    }

    /**
     **********************
     * helper methods
     */
    private GraphNode createUser(String newUserName) {
        BNode subject = new BNode();

        GraphNode userNode = new GraphNode(subject, systemGraph);
        userNode.addProperty(RDF.type, FOAF.Agent);
        userNode.addProperty(PLATFORM.userName, new PlainLiteralImpl(newUserName));

        return userNode;
    }
    // move later?
    public final static String rolesBase = "urn:x-localhost/role/";

    private void clearRoles(NonLiteral userResource) {
        systemGraph.removeAll(filterToArray(userResource, SIOC.has_function, null));
    }

    private ArrayList<Triple> filterToArray(NonLiteral subject, UriRef predicate, Resource object) {
        Iterator<Triple> triples = systemGraph.filter(subject, predicate, object);
        ArrayList<Triple> buffer = new ArrayList<Triple>();
        Lock readLock = systemGraph.getLock().readLock();
        readLock.lock();
        try {
            while (triples.hasNext()) {
                buffer.add(triples.next());
            }
        } finally {
            readLock.unlock();
        }
        return buffer;
    }

    private GraphNode addRole(GraphNode userNode, String roleName) {
        // System.out.println("ROLENAME = " + roleName);

        // is this thing already around? (will be a bnode)
        GraphNode roleNode = getTitleNode(roleName);

        // otherwise make a new one as a named node
        if (roleNode == null) {
            UriRef roleUriRef = new UriRef(rolesBase + roleName);

            roleNode = new GraphNode(roleUriRef, systemGraph);
            roleNode.addProperty(RDF.type, PERMISSION.Role);
            roleNode.addProperty(DC.title, new PlainLiteralImpl(roleName));
            userNode.addProperty(SIOC.has_function, roleUriRef);
        } else {
            userNode.addProperty(SIOC.has_function, roleNode.getNode());
        }
        return userNode;
    }
    public final static String permissionsBase = "urn:x-localhost/role/";

    private GraphNode addPermission(GraphNode userNode, String permissionName) {
        // System.out.println("ROLENAME = " + roleName);

        // is this thing already around? (will be a bnode)
        //   GraphNode permissionNode = getTitleNode(permissionName);

        // otherwise make a new one as a named node
        //  if (permissionNode == null) {
//            UriRef permissionUriRef = new UriRef(permissionsBase + permissionName);
// BNode permissionBNode = new BNode();
        GraphNode permissionNode = new GraphNode(new BNode(), systemGraph);
        permissionNode.addProperty(RDF.type, PERMISSION.Permission);
        // permissionNode.addProperty(DC.title, new PlainLiteralImpl(permissionName));
        userNode.addProperty(PERMISSION.hasPermission, permissionNode.getNode());
        permissionNode.addProperty(PERMISSION.javaPermissionEntry, new PlainLiteralImpl(permissionName));
        return userNode;
    }

//    []    a       <http://xmlns.com/foaf/0.1/Agent> ;
//      <http://clerezza.org/2008/10/permission#hasPermission>
//              [ a       <http://clerezza.org/2008/10/permission#Permission> ;
//                <http://clerezza.org/2008/10/permission#javaPermissionEntry>
//                        "(java.security.AllPermission \"\" \"\")"
//              ] ;
    private void clearPermissions(NonLiteral userResource) {
        systemGraph.removeAll(filterToArray(userResource, PERMISSION.javaPermissionEntry, null));
    }

    /* 
     * must be a neater way of doing this...
     */
    private GraphNode getTitleNode(String title) {
        Iterator<Triple> triples = systemGraph.filter(null, DC.title, new PlainLiteralImpl(title));
        if (triples.hasNext()) {
            Resource resource = triples.next().getSubject();
            return new GraphNode(resource, systemGraph);
        }
        return null;
    }

    /**
     * Replaces/inserts literal value for predicate assumes there is only one
     * triple for the given predicate new value is added before deleting old one
     * in case user is modifying their own data in which case they need triples
     * in place for rights etc.
     *
     * @param userNode node in systemGraph corresponding to the user to change
     * @param predicate property of the triple to change
     * @param newValue new value for given predicate
     */
    private void changeLiteral(GraphNode userNode, UriRef predicate,
            String newValue) {

        Iterator<Triple> oldTriples = systemGraph.filter(
                (NonLiteral) userNode.getNode(), predicate, null);

        ArrayList<Triple> oldBuffer = new ArrayList<Triple>();

        Resource oldObject = null;

        Lock readLock = systemGraph.getLock().readLock();
        readLock.lock();
        try {
            while (oldTriples.hasNext()) {
                Triple triple = oldTriples.next();
                oldObject = triple.getObject();
                oldBuffer.add(triple);
            }
        } finally {
            readLock.unlock();
        }

        // filter appears to see plain literals and xsd:strings as differerent
        // so not
        // userNode.addPropertyValue(predicate, newValue);
        PlainLiteral newObject = new PlainLiteralImpl(newValue);
        userNode.addProperty(predicate, newObject);

        if (newObject.equals(oldObject)) {
            return;
        }
        systemGraph.removeAll(oldBuffer);
    }

    /**
     * Replaces/inserts resource value for predicate assumes there is only one
     * triple for the given predicate
     *
     * @param userNode node in systemGraph corresponding to the user to change
     * @param predicate property of the triple to change
     * @param newValue new value for given predicate
     */
    private void changeResource(GraphNode userNode, UriRef predicate,
            UriRef newValue) {

        Iterator<Triple> oldTriples = systemGraph.filter(
                (NonLiteral) userNode.getNode(), predicate, null);

        ArrayList<Triple> oldBuffer = new ArrayList<Triple>();

        // System.out.println("\n\n");

        Lock readLock = systemGraph.getLock().readLock();
        readLock.lock();
        try {
            while (oldTriples.hasNext()) {
                Triple triple = oldTriples.next();

                Resource oldValue = triple.getObject();
                if (newValue.equals(oldValue)) {
                    return;
                }
                oldBuffer.add(triple);
            }
        } finally {
            readLock.unlock();
        }

        // filter appears to see plain literals and xsd:strings as differerent
        // so not
        // userNode.addPropertyValue(predicate, newValue);
        userNode.addProperty(predicate, newValue);

        systemGraph.removeAll(oldBuffer);
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
     * @param data Turtle string
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
        return inputGraph;
    }

    private GraphNode getUser(@QueryParam("userName") String userName) {
        return getNamedUser(userName);
    }

    /*
     * returns an existing user node from the graph.
     */
    // needs lock?
    private GraphNode getNamedUser(String userName) {
        Iterator<Triple> iter = systemGraph.filter(null, PLATFORM.userName,
                new PlainLiteralImpl(userName));
        if (!iter.hasNext()) {
            return null;
        }
      
       return new GraphNode(iter.next().getSubject(), systemGraph);
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
