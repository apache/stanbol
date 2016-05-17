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

import java.io.ByteArrayOutputStream;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.security.Policy;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.locks.Lock;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.CacheControl;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;
import org.apache.clerezza.platform.config.SystemConfig;
import org.apache.clerezza.commons.rdf.BlankNode;
import org.apache.clerezza.commons.rdf.ImmutableGraph;
import org.apache.clerezza.commons.rdf.Literal;
import org.apache.clerezza.commons.rdf.BlankNodeOrIRI;
import org.apache.clerezza.commons.rdf.RDFTerm;
import org.apache.clerezza.commons.rdf.Triple;
import org.apache.clerezza.commons.rdf.Graph;
import org.apache.clerezza.commons.rdf.IRI;
import org.apache.clerezza.commons.rdf.impl.utils.PlainLiteralImpl;
import org.apache.clerezza.commons.rdf.impl.utils.simple.SimpleGraph;
import org.apache.clerezza.commons.rdf.impl.utils.TripleImpl;
import org.apache.clerezza.rdf.core.serializedform.Parser;
import org.apache.clerezza.rdf.core.serializedform.Serializer;
import org.apache.clerezza.rdf.core.serializedform.SupportedFormat;
import org.apache.clerezza.rdf.ontologies.DC;
import org.apache.clerezza.rdf.ontologies.FOAF;
import org.apache.clerezza.rdf.ontologies.PERMISSION;
import org.apache.clerezza.rdf.ontologies.PLATFORM;
import org.apache.clerezza.rdf.ontologies.RDF;
import org.apache.clerezza.rdf.ontologies.RDFS;
import org.apache.clerezza.rdf.ontologies.SIOC;
import org.apache.clerezza.rdf.utils.GraphNode;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.stanbol.commons.security.PasswordUtil;
import org.apache.stanbol.commons.usermanagement.Ontology;
import org.apache.stanbol.commons.web.viewable.RdfViewable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Handles HTTP requests related to a user
 *
 */
@Component
@Service({Object.class, UserResource.class})
@Property(name = "javax.ws.rs", boolValue = true)
@Path("user-management")
public class UserResource {

    private static Logger log = LoggerFactory.getLogger(UserResource.class);
    @Reference(target = SystemConfig.SYSTEM_GRAPH_FILTER)
    private Graph systemGraph;
    @Reference
    private Serializer serializer;
    @Reference
    private Parser parser;
    private static GraphNode dummyNode;

    static {
        dummyNode = new GraphNode(new BlankNode(), new SimpleGraph());
        dummyNode.addProperty(RDF.type, FOAF.Agent);
    }
    // **********************************
    // ****** SHOW USER DETAILS ********* 
    // **********************************

    //
    // ****** RESTful/RDF *******************
    //
    /**
     * RESTful access to individual user data
     *
     * [has integration test] currently has a kludge to return an empty graph if
     * user not found should return a 404
     *
     * @param userName
     * @return context graph for user
     * @throws UnsupportedEncodingException
     */
    @GET
    @Path("users/{username}")
    public Graph getUserContext(@PathParam("username") String userName)
            throws UnsupportedEncodingException {
        GraphNode userNode = getUser(userName);
        if (userNode == null) { // a kludge
            return new SimpleGraph();
        }
        return userNode.getNodeContext();
    }

    //
    // ****** HTML *******************
    //
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
    @Path("users/edit/{username}")
    public RdfViewable editUser(@PathParam("username") String userName) {
        return new RdfViewable("editUser", getUser(userName),
                this.getClass());
    }

  

    /**
     * RESTful access to user roles (and nested permissions right now - may
     * change) [has integration test]
     *
     * @param userName
     * @return role graph for user
     * @throws UnsupportedEncodingException
     */
    @GET
    @Path("roles/{username}")
    @Produces(SupportedFormat.TURTLE)
    public Graph getUserRoles(@PathParam("username") String userName)
            throws UnsupportedEncodingException {
        Graph rolesGraph = getUserRolesGraph(userName);

        // case of no roles not handled - what best to return : empty graph or
        // 404?
        return rolesGraph;
    }

    /**
     * Update user details adds triples as appropriate to system graph
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
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    public Response storeUser(@Context UriInfo uriInfo,
            @FormParam("currentLogin") String currentLogin,
            @FormParam("newLogin") String newLogin,
            @FormParam("fullName") String fullName,
            @FormParam("email") String email,
            @FormParam("password") String password,
            @FormParam("roles") List<String> roles,
            @FormParam("permissions") List<String> permissions) {

        GraphNode userNode = null;

        if (currentLogin != null) { // 
            currentLogin = currentLogin.trim();
        }
        if (currentLogin != null && !currentLogin.equals("")) {
            userNode = getUser(currentLogin);
            if (userNode != null) {
                return store(userNode, uriInfo, currentLogin, newLogin, fullName, email, password, roles, permissions);
            }
        }
        userNode = createUser(newLogin);
        return store(userNode, uriInfo, newLogin, newLogin, fullName, email, password, roles, permissions);
    }

    /**
     * Modify user given a graph describing the change.
     *
     * @param inputGraph change graph
     * @return HTTP response
     */
    @POST
    @Consumes(SupportedFormat.TURTLE)
    @Path("change-user")
    public Response changeUser(ImmutableGraph inputGraph) {

        Lock readLock = systemGraph.getLock().readLock();
        readLock.lock();

        Iterator<Triple> changes = inputGraph.filter(null, null,
                Ontology.Change);

        Triple oldTriple = null;
        Triple newTriple = null;

        if (changes.hasNext()) {
            Triple changeTriple = changes.next();

            BlankNodeOrIRI changeNode = changeTriple.getSubject();

            Literal userName = (Literal) inputGraph
                    .filter(changeNode, PLATFORM.userName, null).next()
                    .getObject();

            Iterator<Triple> userTriples = systemGraph
                    .filter(null, PLATFORM.userName, userName);

            //     if (userTriples.hasNext()) {
            BlankNodeOrIRI userNode = userTriples.next()
                    .getSubject();

            IRI predicateIRI = (IRI) inputGraph
                    .filter(changeNode, Ontology.predicate, null).next()
                    .getObject();

            // handle old value (if it exists)
            Iterator<Triple> iterator = inputGraph.filter(changeNode,
                    Ontology.oldValue, null);

            RDFTerm oldValue = null;

            if (iterator.hasNext()) {

                oldValue = iterator.next().getObject();
                // Triple oldTriple = systemGraph.filter(null, predicateIRI,
                // oldValue).next();
                Iterator<Triple> oldTriples = systemGraph.filter(userNode,
                        predicateIRI, oldValue);
                if (oldTriples.hasNext()) {
                    oldTriple = oldTriples.next();
                }
            }

            RDFTerm newValue = inputGraph
                    .filter(changeNode, Ontology.newValue, null).next()
                    .getObject();

            newTriple = new TripleImpl(userNode, predicateIRI,
                    newValue);
            // }
        }
        readLock.unlock();

        Lock writeLock = systemGraph.getLock().writeLock();
        writeLock.lock();
        if (oldTriple != null) {
            systemGraph.remove(oldTriple);
        }
        systemGraph.add(newTriple);
        writeLock.unlock();

        // it's not actually creating a resource so this
        // seems the most appropriate response
        return Response.noContent().build();
    }

    /**
     * Provides HTML corresponding to a user's roles
     *
     * all roles are listed with checkboxes, the roles this user has are checked
     *
     * (isn't very pretty but is just a one-off)
     *
     * @param userName the user in question
     * @return HTML checkboxes as HTTP response
     */
    @GET
    @Path("users/{username}/rolesCheckboxes")
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
                GraphNode roleNode = new GraphNode(triple.getSubject(), systemGraph);
                Iterator<Literal> titlesIterator = roleNode.getLiterals(DC.title);
                while (titlesIterator.hasNext()) {
                    allRoleNames.add(titlesIterator.next().getLexicalForm());
                }
            }
        } finally {
            readLock.unlock();
        }

        Graph rolesGraph = getUserRolesGraph(userName);

        ArrayList<String> userRoleNames = new ArrayList<String>();

        Iterator<Triple> userRoleTriples = rolesGraph.filter(null, RDF.type, PERMISSION.Role);
        while (userRoleTriples.hasNext()) {
            Triple triple = userRoleTriples.next();
            GraphNode roleNode = new GraphNode(triple.getSubject(), rolesGraph);

            Iterator<Literal> titlesIterator = roleNode.getLiterals(DC.title);
            while (titlesIterator.hasNext()) {
                userRoleNames.add(titlesIterator.next().getLexicalForm());
            }
        }
        for (int i = 0; i < allRoleNames.size(); i++) {
            String role = allRoleNames.get(i);
            if (role.equals("BasePermissionsRole")) { // filter out
                continue;
            }
            if (userRoleNames.contains(role)) {
                html.append("<input class=\"role\" type=\"checkbox\" id=\"").append(role).append("\" name=\"").append(role).append("\" value=\"").append(role).append("\" checked=\"checked\" />");
            } else {
                html.append("<input class=\"role\" type=\"checkbox\" id=\"").append(role).append("\" name=\"").append(role).append("\" value=\"").append(role).append("\" />");
            }
            html.append("<label for=\"").append(role).append("\">").append(role).append("</label>");
            html.append("<br />");
        }
        return Response.ok(html.toString()).build();
    }

    /**
     * List the users. renders the user type with the "listUser" rendering
     * template
     *
     * @return rendering specification
     */
    @GET
    @Path("users")
    @Produces(MediaType.TEXT_HTML)
    public RdfViewable listUsers() {
        return new RdfViewable("listUser", getUserType(), this.getClass());
    }

    public GraphNode getUserType() {
        return new GraphNode(FOAF.Agent, systemGraph);
    }

    /*
     * RESTful creation of user
     * @TODO validity check input
     */
    @PUT
    @Path("users/{username}")
    @Consumes(SupportedFormat.TURTLE)
    public Response createUser(@Context UriInfo uriInfo, @PathParam("username") String userName, ImmutableGraph inputGraph) {
        Lock writeLock = systemGraph.getLock().writeLock();
        writeLock.lock();
        systemGraph.addAll(inputGraph);
        writeLock.unlock();
        UriBuilder uriBuilder = uriInfo.getBaseUriBuilder();
        URI createdResource = uriBuilder.replacePath("/user-management/users/" + userName).build();
        return Response.created(createdResource).build();
    }

    /**
     * Create a user. returns a dummy use with "editUser" as rendering
     * specification (this will be a HTML form)
     *
     * @param uriInfo request details
     * @return rendering specification
     */
    @GET
    @Path("create-form")
    @Produces(MediaType.TEXT_HTML)
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
     * @TODO check for password
     *
     * @param userData
     * @return HTTP/1.1 204 No Content
     */
    @POST
    @Consumes(SupportedFormat.TURTLE)
    @Path("add-user")
    public Response addUser(@Context UriInfo uriInfo, ImmutableGraph inputGraph) {

        Iterator<Triple> agents = inputGraph.filter(null, null, FOAF.Agent);

        BlankNodeOrIRI userNode = agents.next().getSubject();

        Iterator<Triple> userTriples = inputGraph.filter(userNode, null, null);

        String userName = "";
        Triple userTriple = null;

        Lock writeLock = systemGraph.getLock().writeLock();
        writeLock.lock();
        try {
            GraphNode systemUserNode = new GraphNode(userNode, systemGraph);
            addRole(systemUserNode, "BasePermissionsRole");
            while (userTriples.hasNext()) {
                userTriple = userTriples.next();
                systemGraph.add(userTriple);
            }
            userName = ((Literal) userTriple.getObject()).getLexicalForm();
        } finally {
            writeLock.unlock();
        }

        UriBuilder uriBuilder = uriInfo.getBaseUriBuilder();
        URI createdResource = uriBuilder.replacePath("/user-management/users/" + userName).build();
        return Response.created(createdResource).build();
    }

// **********************************
// ****** REMOVE USER *************** 
// **********************************
    /**
     * Deletes a named user
     *
     * (called from HTML form)
     *
     * @param userName
     */
    @POST
    @Path("delete")
    public void removeUser(@FormParam("user") String userName) {
        remove(userName);

    }

    /**
     * Deletes a named user
     *
     * @param userName
     */
    private void remove(String userName) {
        RDFTerm userResource = getNamedUser(userName).getNode();
        Iterator<Triple> userTriples = systemGraph.filter((BlankNodeOrIRI) userResource, null, null);

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

        // ImmutableGraph context = getNamedUser(userName).getNodeContext();
        Lock writeLock = systemGraph.getLock().writeLock();
        writeLock.lock();
        try {
            systemGraph.removeAll(buffer);
        } finally {
            writeLock.unlock();
        }
    }

    /**
     * RESTful user deletion
     *
     * called direct from the URI, e.g.
     * http://localhost:8080/user-management/users/fred
     *
     * @param userName name of the user to delete
     * @return HTTP/1.1 204 No Content
     */
    @DELETE
    @Path("users/{username}")
    public Response delete(@PathParam("username") String userName) {
        remove(userName);
        return Response.noContent().build();
    }

    /**
     * Endpoint-style user deletion takes a little bunch of Turtle describing
     * the user to delete e.g. [] a foaf:Agent ; cz:userName "Hugo Ball" .
     *
     * @param userData
     * @return HTTP/1.1 204 No Content
     */
    @POST
    @Consumes(SupportedFormat.TURTLE)
    @Path("delete-user")
    public Response deleteUser(ImmutableGraph inputGraph) {

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
            if (userTriples.hasNext()) {
                Triple userTriple = userTriples.next();
                Iterator<Triple> systemUserTriples = systemGraph.filter(
                        userTriple.getSubject(), null, null);

                while (systemUserTriples.hasNext()) {
                    tripleBuffer.add(systemUserTriples.next());
                }
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
    /**
     * Lists all roles using a rendering as specified in template listRole
     *
     * @return
     */
    @GET
    @Path("roles")
    @Produces(MediaType.TEXT_HTML)
    public RdfViewable listRoles() {
        return new RdfViewable("listRole", getRoleType(), this.getClass());
    }

    /**
     * Provides the node in the system graph corresponding to rdf:type Role
     *
     * @return Role class node
     */
    public GraphNode getRoleType() {
        return new GraphNode(PERMISSION.Role,
                systemGraph);
    }



// **********************************
// ****** ADD ROLE ****************** 
// **********************************
    /**
     * Create a role. returns "editRole" as rendering specification (this will
     * be a HTML form)
     *
     * @param uriInfo request details
     * @return rendering specification
     */
    @GET
    @Path("create-role")
    @Produces(MediaType.TEXT_HTML)
    public RdfViewable getCreateRoleForm(@Context UriInfo uriInfo) {
        return new RdfViewable("editRole", dummyNode,
                this.getClass());
    }

    // /user-management/roles/edit/'+roleName,
    /**
     * lookup a role by name presenting it with "editRole" as rendering
     * instruction.
     *
     * @param userName
     * @return
     */
    @GET
    @Path("roles/edit/{rolename}")
    @Produces(MediaType.TEXT_HTML)
    public RdfViewable editRole(@PathParam("rolename") String roleName) {
        return new RdfViewable("editRole", getRole(roleName),
                this.getClass());
    }

    private GraphNode getRole(@QueryParam("roleName") String roleName) {
        return getNamedRole(roleName);
    }

    /*
     * returns an existing user node from the graph.
     */
    private GraphNode getNamedRole(String roleName) {
        GraphNode roleNode = null;
        Iterator<Triple> roleIterator = systemGraph.filter(null, RDF.type, PERMISSION.Role);
        //new PlainLiteralImpl(userName));
        if (!roleIterator.hasNext()) {
            return null;
        }
        ArrayList<Triple> tripleBuffer = new ArrayList<Triple>();
        Lock readLock = systemGraph.getLock().readLock();
        readLock.lock();

        try {
            while (roleIterator.hasNext()) {
                BlankNodeOrIRI role = roleIterator.next().getSubject();
                Iterator<Triple> roleNameTriples = systemGraph.filter(role, DC.title,
                        null);
                while (roleNameTriples.hasNext()) {
                    Literal roleLiteral = (Literal) roleNameTriples.next().getObject();
                    if (roleName.equals(roleLiteral.getLexicalForm())) {
                        roleNode = new GraphNode(role, systemGraph);
                        break;
                    }
                }
                if (roleNode != null) {
                    break;
                }
            }

        } finally {
            readLock.unlock();
        }
        return roleNode;
    }
// **********************************
// ****** REMOVE ROLE *************** 
// **********************************

    /**
     * Deletes a named role
     *
     * (called from HTML form)
     *
     * @param roleName
     */
    @POST
    @Path("delete-role")
    public void removeRole(@FormParam("role") String roleName) {
        deleteRole(roleName);

    }

    /**
     * Deletes a named user
     *
     * @param userName
     */
    private void deleteRole(String roleName) {
        RDFTerm roleResource = getNamedRole(roleName).getNode();
        Iterator<Triple> roleTriples = systemGraph.filter((BlankNodeOrIRI) roleResource, null, null);

        ArrayList<Triple> buffer = new ArrayList<Triple>();

        Lock readLock = systemGraph.getLock().readLock();
        readLock.lock();
        try {
            while (roleTriples.hasNext()) {
                Triple triple = roleTriples.next();
                buffer.add(triple);
            }
        } finally {
            readLock.unlock();
        }

        // is lock needed?
        Lock writeLock = systemGraph.getLock().writeLock();
        writeLock.lock();
        try {
            systemGraph.removeAll(buffer);
        } finally {
            writeLock.unlock();
        }
    }

    /**
     * Update role details - adds triples as appropriate to system graph
     *
     */
    @POST
    @Path("store-role")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    public Response storeRoleFormHandler(@Context UriInfo uriInfo,
            @FormParam("roleName") String roleName,
            @FormParam("comment") String comment,
            @FormParam("permissions") List<String> permissions) {

        GraphNode roleNode = null;

        if (roleName != null) { // 
            roleName = roleName.trim();
        }
        if (roleName != null && !roleName.equals("")) {
            roleNode = getRole(roleName);
            if (roleNode != null) {
                return storeRole(roleNode, uriInfo, roleName, comment, permissions);
            }
        }
        roleNode = createRole(roleName, comment);
        return storeRole(roleNode, uriInfo, roleName, comment, permissions);
    }

    /**
     * Creates a new role wit the the specified role name
     *
     * @param newUserName
     * @return user node in system graph
     */
    private GraphNode createRole(String newRoleName, String comment) {
        BlankNode subject = new BlankNode();
        GraphNode roleNode = new GraphNode(subject, systemGraph);
        roleNode.addProperty(RDF.type, PERMISSION.Role);
        roleNode.addProperty(DC.title, new PlainLiteralImpl(newRoleName));
        roleNode.addProperty(RDFS.comment, new PlainLiteralImpl(comment));
        return roleNode;
    }

    private Response storeRole(GraphNode roleNode, UriInfo uriInfo,
            String roleName,
            String comment,
            List<String> permissions) {

        BlankNodeOrIRI roleResource = (BlankNodeOrIRI) roleNode.getNode();

        if (permissions != null) {
            clearPermissions(roleResource);
            Lock writeLock = systemGraph.getLock().writeLock();
            writeLock.lock();
            try {
                for (int i = 0; i < permissions.size(); i++) {
                    permissions.set(i, permissions.get(i).trim());
                    if (!permissions.get(i).equals("")) {
                        addPermission(roleNode, permissions.get(i));
                    }
                }
            } finally {
                writeLock.unlock();
            }
        }

	//refresh the policy so it will recheck the permissions
	Policy.getPolicy().refresh();

        // showSystem();

        URI pageUri = uriInfo.getBaseUriBuilder()
                .path("system/console/usermanagement").build();

        // header Cache-control: no-cache, just in case intermediaries are
        // holding onto old stuff
        CacheControl cc = new CacheControl();
        cc.setNoCache(true);

        //showSystem();

        // see other my not be the best response, but does seem the best given
        // the jax-rs things available
        return Response.seeOther(pageUri).cacheControl(cc).build();
    }

// **********************************
// ****** ASSIGN ROLE TO USER ******* 
// **********************************
// **********************************
// ****** REMOVE ROLE FROM USER ***** 
// **********************************
// **********************************
// ****** LIST PERMISSIONS ********** 
// **********************************
    /*
     * Provides listing of all permissions present in system graph
     * rendered according to specification in listPermission template
     */
    @GET
    @Path("permissions")
    @Produces(MediaType.TEXT_HTML)
    public RdfViewable listPermissions() {
        addClassToPermissions(); // workaround
        return new RdfViewable("listPermission", getPermissionType(), this.getClass());
    }

    /**
     * Provides the node in the system graph corresponding to rdf:type
     * Permission
     *
     * @return Permission class node
     */
    public GraphNode getPermissionType() {
        return new GraphNode(PERMISSION.Permission,
                systemGraph);
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
    ////////////////////////////////////////////////////////////////
    /**
     * Pushes user data into system graph
     *
     * @param userNode
     * @param uriInfo
     * @param currentUserName
     * @param newUserName
     * @param fullName
     * @param email
     * @param password
     * @param roles
     * @param permissions
     * @return
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
            changeResource(userNode, FOAF.mbox, new IRI("mailto:" + email));
        }

        BlankNodeOrIRI userResource = (BlankNodeOrIRI) userNode.getNode();

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

        //showSystem();

        // see other my not be the best response, but does seem the best given
        // the jax-rs things available
        return Response.seeOther(pageUri).cacheControl(cc).build();
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
                RDFTerm permissionResource = triple.getObject();
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

    /**
     * Provides a graph containing Role triples associated with a given user
     *
     * @param userName
     * @return roles graph
     */
    private Graph getUserRolesGraph(String userName) {
        GraphNode userNode = getUser(userName);

        Iterator<RDFTerm> functionIterator = userNode
                .getObjects(SIOC.has_function);

        SimpleGraph rolesGraph = new SimpleGraph();

        while (functionIterator.hasNext()) {

            GraphNode functionNode = new GraphNode(functionIterator.next(),
                    systemGraph);

            Iterator<Triple> roleIterator = systemGraph.filter(
                    (BlankNodeOrIRI) functionNode.getNode(), RDF.type,
                    PERMISSION.Role);

            // needs lock?
            while (roleIterator.hasNext()) {
                Triple roleTriple = roleIterator.next();
                // rolesGraph.add(roleTriple);
                BlankNodeOrIRI roleNode = roleTriple.getSubject();
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
     * @return user node in system graph
     */
    private GraphNode createUser(String newUserName) {
        BlankNode subject = new BlankNode();

        GraphNode userNode = new GraphNode(subject, systemGraph);
        userNode.addProperty(RDF.type, FOAF.Agent);
        userNode.addProperty(PLATFORM.userName, new PlainLiteralImpl(newUserName));
        addRole(userNode, "BasePermissionsRole");
        return userNode;
    }
    // move later?
    public final static String rolesBase = "urn:x-localhost/role/";

    private void clearRoles(BlankNodeOrIRI userResource) {
        systemGraph.removeAll(filterToArray(userResource, SIOC.has_function, null));
    }

    /**
     * convenience - used for buffering
     *
     * @param subject
     * @param predicate
     * @param object
     * @return
     */
    private List<Triple> filterToArray(BlankNodeOrIRI subject, IRI predicate, RDFTerm object) {
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

    /**
     * Add a role to a given user in system graph
     *
     * @param userNode node corresponding to user
     * @param roleName name of the role
     * @return user node
     */
    private GraphNode addRole(GraphNode userNode, String roleName) {

        // is this thing already around? (will be a bnode)
        GraphNode roleNode = getTitleNode(roleName);

        // otherwise make a new one as a named node
        if (roleNode == null) {
            IRI roleIRI = new IRI(rolesBase + roleName);

            roleNode = new GraphNode(roleIRI, systemGraph);
            roleNode.addProperty(RDF.type, PERMISSION.Role);
            roleNode.addProperty(DC.title, new PlainLiteralImpl(roleName));
            userNode.addProperty(SIOC.has_function, roleIRI);
        } else {
            userNode.addProperty(SIOC.has_function, roleNode.getNode());
        }
        return userNode;
    }

    // public final static String permissionsBase = "urn:x-localhost/role/";
    private GraphNode addPermission(GraphNode subjectNode, String permissionString) {

        if (hasPermission(subjectNode, permissionString)) {
            return subjectNode;
        }
        GraphNode permissionNode = new GraphNode(new BlankNode(), systemGraph);
        permissionNode.addProperty(RDF.type, PERMISSION.Permission);
        // permissionNode.addProperty(DC.title, new PlainLiteralImpl(permissionName));
        subjectNode.addProperty(PERMISSION.hasPermission, permissionNode.getNode());
        permissionNode.addProperty(PERMISSION.javaPermissionEntry, new PlainLiteralImpl(permissionString));
        return subjectNode;
    }

    private boolean hasPermission(GraphNode userNode, String permissionString) {
        boolean has = false;
        Iterator<Triple> existingPermissions = systemGraph.filter((BlankNodeOrIRI) userNode.getNode(), PERMISSION.hasPermission, null);
        Lock readLock = systemGraph.getLock().readLock();
        readLock.lock();
        try { // check to see if the user already has this permission
            while (existingPermissions.hasNext()) {
                BlankNodeOrIRI permissionNode = (BlankNodeOrIRI) existingPermissions.next().getObject();
                Iterator<Triple> permissionTriples = systemGraph.filter(permissionNode, PERMISSION.javaPermissionEntry, null);
                while (permissionTriples.hasNext()) {
                    Literal permission = (Literal) permissionTriples.next().getObject();
                    if (permissionString.equals(permission.getLexicalForm())) {
                        has = true;
                    }
                }

            }
        } finally {
            readLock.unlock();
        }
        return has;
    }

//    []    a       <http://xmlns.com/foaf/0.1/Agent> ;
//      <http://clerezza.org/2008/10/permission#hasPermission>
//              [ a       <http://clerezza.org/2008/10/permission#Permission> ;
//                <http://clerezza.org/2008/10/permission#javaPermissionEntry>
//                        "(java.security.AllPermission \"\" \"\")"
//              ] ;
    private void clearPermissions(BlankNodeOrIRI subject) {
        ArrayList<Triple> buffer = new ArrayList<Triple>();

        Lock readLock = systemGraph.getLock().readLock();
        readLock.lock();
        try {
            Iterator<Triple> permissions = systemGraph.filter(subject, PERMISSION.hasPermission, null);
            while (permissions.hasNext()) {
                Triple permissionTriple = permissions.next();
                buffer.add(permissionTriple);
                BlankNodeOrIRI permissionNode = (BlankNodeOrIRI) permissionTriple.getObject();
                Iterator<Triple> permissionTriples = systemGraph.filter(permissionNode, null, null);
                while (permissionTriples.hasNext()) {
                    buffer.add(permissionTriples.next());
                }
            }
        } finally {
            readLock.unlock();
        }
        systemGraph.removeAll(buffer);
    }

    /* 
     * must be a neater way of doing this...
     */
    private GraphNode getTitleNode(String title) {
        Iterator<Triple> triples = systemGraph.filter(null, DC.title, new PlainLiteralImpl(title));
        if (triples.hasNext()) {
            RDFTerm resource = triples.next().getSubject();
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
    private void changeLiteral(GraphNode userNode, IRI predicate,
            String newValue) {

        Iterator<Triple> oldTriples = systemGraph.filter(
                (BlankNodeOrIRI) userNode.getNode(), predicate, null);

        ArrayList<Triple> oldBuffer = new ArrayList<Triple>();

        RDFTerm oldObject = null;

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
        Literal newObject = new PlainLiteralImpl(newValue);
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
    private void changeResource(GraphNode userNode, IRI predicate,
            IRI newValue) {

        Iterator<Triple> oldTriples = systemGraph.filter(
                (BlankNodeOrIRI) userNode.getNode(), predicate, null);

        ArrayList<Triple> oldBuffer = new ArrayList<Triple>();

        Lock readLock = systemGraph.getLock().readLock();
        readLock.lock();
        try {
            while (oldTriples.hasNext()) {
                Triple triple = oldTriples.next();

                RDFTerm oldValue = triple.getObject();
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

    private Set<GraphNode> getResourcesOfType(IRI type) {
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

    /*
     * Dumps a Turtle serialization of the system graph to System.out
     * handy for debugging
     */
    private void showSystem() {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            serializer.serialize(baos, systemGraph, SupportedFormat.TURTLE);
            System.out.println(new String(baos.toByteArray(), "utf-8"));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
