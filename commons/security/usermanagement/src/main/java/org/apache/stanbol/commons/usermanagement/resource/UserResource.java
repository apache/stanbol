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

import com.sun.jersey.multipart.FormDataParam;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.util.ArrayList;
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
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;
import org.apache.clerezza.platform.config.SystemConfig;
import org.apache.clerezza.rdf.core.BNode;
import org.apache.clerezza.rdf.core.Graph;
import org.apache.clerezza.rdf.core.Literal;
import org.apache.clerezza.rdf.core.MGraph;
import org.apache.clerezza.rdf.core.NonLiteral;
import org.apache.clerezza.rdf.core.PlainLiteral;
import org.apache.clerezza.rdf.core.Resource;
import org.apache.clerezza.rdf.core.Triple;
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
import org.apache.clerezza.rdf.ontologies.PERMISSION;
import org.apache.clerezza.rdf.ontologies.PLATFORM;
import org.apache.clerezza.rdf.ontologies.RDF;
import org.apache.clerezza.rdf.ontologies.SIOC;
import org.apache.clerezza.rdf.utils.GraphNode;
import org.apache.clerezza.rdf.utils.MGraphUtils;
import org.apache.clerezza.rdf.utils.MGraphUtils.NoSuchSubGraphException;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.stanbol.commons.security.PasswordUtil;
import org.apache.stanbol.commons.usermanagement.Ontology;
import org.apache.stanbol.commons.web.viewable.RdfViewable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component
@Service( {Object.class, UserResource.class })
@Property(name="javax.ws.rs", boolValue=true)
@Path("user-management")
public class UserResource {

    private static Logger log = LoggerFactory.getLogger(UserResource.class);
    @Reference(target = SystemConfig.SYSTEM_GRAPH_FILTER)
    private LockableMGraph systemGraph;
    @Reference
    private Parser parser;

    private static GraphNode dummyNode;

    static {
        dummyNode = new GraphNode(new BNode(), new SimpleMGraph());
        dummyNode.addProperty(RDF.type, FOAF.Agent);
    }
    // **********************************
    // ****** SHOW USER DETAILS ****** 
    // **********************************

    /**
     * lookup a user by name.
     *
     * @param userName
     * @return
     */
    @GET
    @Path("view-user")
    @Produces(MediaType.TEXT_HTML)
    public RdfViewable viewUser(@QueryParam("userName") String userName) {
        return new RdfViewable("edit", getUser(userName), this.getClass());
    }

    /**
     * lookup a user by name presenting it with "editUser" as rendering
     * instruction.
     *
     * @param userName
     * @return
     */
    @GET
    @Path("user/{username}")
    @Produces(MediaType.TEXT_HTML)
    public RdfViewable editUser(@PathParam("username") String userName) {
        return new RdfViewable("editUser", getUser(userName),
                this.getClass());
    }

    /**
     * Produces suitable permission-checkboxes
     */
    @GET
    @Path("user/{username}/permissionsCheckboxes")
    @Produces(MediaType.TEXT_HTML)
    public RdfViewable permissionsCheckboxes(@PathParam("username") String userName) { //getUser(userName)
        return new RdfViewable("permissionsCheckboxes", getUser(userName), this.getClass());
    }

    /**
     * RESTful access to individual user data [has integration test]
     *
     * @param userName
     * @return context graph for user
     * @throws UnsupportedEncodingException
     */
    @GET
    @Path("users/{username}")
    public TripleCollection getUserContext(@PathParam("username") String userName)
            throws UnsupportedEncodingException {
        return getUser(userName).getNodeContext();
    }

    /**
     * RESTful access to user roles (and permissions right now - may change)
     * [has integration test]
     *
     * @param userName
     * @return context graph for user
     * @throws UnsupportedEncodingException
     */
    @GET
    @Path("roles/{username}")
    @Produces(SupportedFormat.TURTLE)
    public TripleCollection getUserRoles(@PathParam("username") String userName)
            throws UnsupportedEncodingException {
        MGraph rolesGraph = getUserRolesGraph(userName);

        // case of no roles not handled - what best to return : empty graph or
        // 404?
        return rolesGraph;
    }

    /**
     * Update user details.
     *
     * @param uriInfo
     * @param currentLogin
     * @param newLogin
     * @param fullName
     * @param email
     * @param password
     * @param roles
     * @param permissions
     * @return
     */
    @POST
    @Path("store-user")
    // @Consumes("multipart/form-data")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    public Response storeUser(@Context UriInfo uriInfo,
            @FormParam("currentLogin") String currentLogin,
            @FormParam("newLogin") String newLogin,
            @FormParam("fullName") String fullName,
            @FormParam("email") String email,
            @FormParam("password") String password,
            @FormParam("roles") List<String> roles,
            @FormParam("permissions") List<String> permissions) {

        GraphNode userNode;

        if (currentLogin != null) { // 
            currentLogin = currentLogin.trim();
        }

        if (currentLogin != null && !currentLogin.equals("")) {
            userNode = getUser(currentLogin);
            return store(userNode, uriInfo, currentLogin, newLogin, fullName, email, password, roles, permissions);
        }

        userNode = createUser(newLogin);


        return store(userNode, uriInfo, newLogin, newLogin, fullName, email, password, roles, permissions);
    }

    /**
     * produces suitable role checkboxes
     *
     * @return
     */
    @GET
    @Path("rolesCheckboxes")
    @Produces(SupportedFormat.HTML)
    public RdfViewable rolesCheckboxes() {
        return new RdfViewable("rolesCheckboxes", getRoleType(), this.getClass());
    }

    /*
     * Modify user given give a graph describing the change.
     */
    @POST
    @Consumes(SupportedFormat.TURTLE)
    @Path("change-user")
    public Response changeUser(Graph inputGraph) {

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

    /*
     * Isn't very pretty but is just a one-off
     */
    @GET
    @Path("user/{username}/rolesCheckboxes")
    @Produces(MediaType.TEXT_HTML)
    public Response rolesCheckboxes(@PathParam("username") String userName) {
        // return new RdfViewable("rolesCheckboxes", getRoleType(), this.getClass());
        StringBuffer html = new StringBuffer();

        Iterator<Triple> allRoleTriples = systemGraph.filter(null, RDF.type, PERMISSION.Role);

        ArrayList<String> allRoleNames = new ArrayList<String>();

        Lock readLock = systemGraph.getLock().readLock();
        readLock.lock();
        try { // pulls out all role names
            while (allRoleTriples.hasNext()) {
                Triple triple = allRoleTriples.next();
                //                if (triple.getPredicate().equals(DC.title)) {
                //                    allRoleNames.add(((Literal) triple.getObject()).getLexicalForm());
                //                    System.out.println("system role = "+((Literal) triple.getObject()).getLexicalForm());
                //                }
                //   NonLiteral roleNode = triple.getSubject();
                GraphNode roleNode = new GraphNode(triple.getSubject(), systemGraph);
                Iterator<Literal> titlesIterator = roleNode.getLiterals(DC.title);
                while (titlesIterator.hasNext()) {
                    allRoleNames.add(titlesIterator.next().getLexicalForm());
                    //   System.out.println("system role = " + titlesIterator.next().getLexicalForm());
                }
            }
        } finally {
            readLock.unlock();
        }

        MGraph rolesGraph = getUserRolesGraph(userName);

        ArrayList<String> userRoleNames = new ArrayList<String>();

        Iterator<Triple> userRoleTriples = rolesGraph.filter(null, RDF.type, PERMISSION.Role);
        while (userRoleTriples.hasNext()) {
            Triple triple = userRoleTriples.next();
            GraphNode roleNode = new GraphNode(triple.getSubject(), rolesGraph);

            Iterator<Literal> titlesIterator = roleNode.getLiterals(DC.title);
            while (titlesIterator.hasNext()) {
                userRoleNames.add(titlesIterator.next().getLexicalForm());
                //   System.out.println("user role = " + titlesIterator.next().getLexicalForm());
            }
        }
        for (int i = 0; i < allRoleNames.size(); i++) {
            // BasePermissionsRole
            String role = allRoleNames.get(i);
            if (role.equals("BasePermissionsRole")) {
                continue;
            }
            if (userRoleNames.contains(role)) {
                html.append("<input class=\"role\" type=\"checkbox\" id=\"" + role + "\" name=\"" + role + "\" value=\"" + role + "\" checked=\"checked\" />");
            } else {
                html.append("<input class=\"role\" type=\"checkbox\" id=\"" + role + "\" name=\"" + role + "\" value=\"" + role + "\" />");
            }
            html.append("<label for=\"" + role + "\">" + role + "</label>");
            html.append("<br />");
        }
        return Response.ok(html.toString()).build();
    }

    /**
     * List the users. I.e. renders the user type with the "listUser" rendering
     * specification.
     *
     * @return
     */
    @GET
    @Path("users")
    @Produces(MediaType.TEXT_HTML)
    public RdfViewable listUsers() {
        return new RdfViewable("listUser", getUserType(), this.getClass());
    }

    /**
     * Create a user. I.e. returns a dummy use with "editUSer" as rendering
     * specification.
     *
     * @param uriInfo
     * @return
     */
    @GET
    @Path("create-form")
    public RdfViewable getCreateUserForm(@Context UriInfo uriInfo) {
        return new RdfViewable("editUser", dummyNode,
                this.getClass());
    }

    /**
     * Endpoint-style user creation takes a little bunch of Turtle e.g. [] a
     * foaf:Agent ; cz:userName "Hugo Ball" .
     *
     * [has test]
     *
     * @param userData
     * @return HTTP/1.1 204 No Content
     */
    @POST // @TODO add RESTful PUT version
    @Consumes(SupportedFormat.TURTLE)
    @Path("add-user")
    public Response addUser(@Context UriInfo uriInfo, Graph inputGraph) {

        Iterator<Triple> agents = inputGraph.filter(null, null, FOAF.Agent);

        NonLiteral userNode = agents.next().getSubject();

        Iterator<Triple> userTriples = inputGraph.filter(userNode, null, null);

        String userName = "";
        Triple userTriple = null;

        Lock writeLock = systemGraph.getLock().writeLock();
        writeLock.lock();
        try {
            while (userTriples.hasNext()) {
                userTriple = userTriples.next();
                systemGraph.add(userTriple);
            }
            userName = ((Literal) userTriple.getObject()).getLexicalForm();
        } finally {
            writeLock.unlock();
        }

        UriBuilder uriBuilder = uriInfo.getBaseUriBuilder();

        URI createdResource = null;
        //    try {
        //  createdResource = new URI("http://localhost:8080/user-management/users/" + userName);
        createdResource = uriBuilder.replacePath("/user-management/users/" + userName).build();
//        } catch (URISyntaxException ex) {
//            java.util.logging.Logger.getLogger(UserResource.class.getName()).log(Level.SEVERE, null, ex);
//        }
        System.out.println("URI =" + createdResource);
// from HTTPbis
//The request has been fulfilled and has resulted in one or more new
//   resources being created.
//        Response.ResponseBuilder builder = Response.status(Response.Status.CREATED);
//       // builder.header("Location", createdResource);
//        
//        Response response = builder.build();
//         MultivaluedMap<String,Object> meta = response.getMetadata(); 
//         meta.putSingle("Location", createdResource);
        return Response.created(createdResource).build();
    }

// **********************************
// ****** REMOVE USER *************** 
// **********************************
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
     * Endpoint-style user deletion takes a little bunch of Turtle e.g. [] a
     * foaf:Agent ; cz:userName "Hugo Ball" .
     *
     * @param userData
     * @return HTTP/1.1 204 No Content
     */
    @POST
    @Consumes(SupportedFormat.TURTLE)
    @Path("delete-user")
    public Response deleteUser(Graph inputGraph) {

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

// **********************************
// ****** SHOW ROLE DETAILS *********
// **********************************
// **********************************
// ****** LIST ROLES **************** 
// **********************************
    @GET
    @Path("roles")
    @Produces(MediaType.TEXT_HTML)
    public RdfViewable listRoles() {
        return new RdfViewable("listRole", getRoleType(), this.getClass());
    }

// **********************************
// ****** ADD ROLE ****************** 
// **********************************
// **********************************
// ****** REMOVE ROLE *************** 
// **********************************
// **********************************
// ****** ASSIGN ROLE TO USER ******* 
// **********************************
// **********************************
// ****** REMOVE ROLE FROM USER ***** 
// **********************************
// **********************************
// ****** LIST PERMISSIONS ********** 
// **********************************
    @GET
    @Path("permissions")
    @Produces(MediaType.TEXT_HTML)
    public RdfViewable listPermissions() {
        addClassToPermissions();
        return new RdfViewable("listPermission", getPermissionType(), this.getClass());
    }

// **********************************
// ****** ADD PERMISSION TO USER **** 
// **********************************
// **************************************
// ****** REMOVE PERMISSION FROM USER *** 
// **************************************
// ************************************
// ****** ADD PERMISSION TO ROLE ****** 
// ************************************
// **************************************
// ****** REMOVE PERMISSION FROM ROLE *** 
// **************************************
    // misc
   /* @GET
    public String index() throws UnsupportedEncodingException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        serializer.serialize(baos, systemGraph, SupportedFormat.TURTLE);
        String serialized = new String(baos.toByteArray(), "utf-8");
        return serialized;
    }*/

    public GraphNode getUserType() {
        return new GraphNode(FOAF.Agent, systemGraph);
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
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    public void replaceSubGraph(@QueryParam("graph") UriRef graphUri,
            @FormDataParam("assert") String assertedString,
            @FormDataParam("revoke") String revokedString,
            @FormDataParam("format") @DefaultValue(SupportedFormat.TURTLE) String format) {
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

    public GraphNode getRoleType() {
        return new GraphNode(PERMISSION.Role,
                systemGraph);
    }

    private MGraph getUserRolesGraph(String userName) {
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
        return rolesGraph;
    }

    /**
     * Creates a new user withe the specified user name
     *
     * @param newUserName
     * @return
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
            Set<GraphNode> resources = new HashSet<GraphNode>();
            while (triples.hasNext()) {
                resources.add(new GraphNode(triples.next().getSubject(),
                        systemGraph));
            }
            return resources;
        } finally {
            readLock.unlock();
        }
    }
}
