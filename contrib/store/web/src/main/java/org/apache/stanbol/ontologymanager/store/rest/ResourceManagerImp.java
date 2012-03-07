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
package org.apache.stanbol.ontologymanager.store.rest;

import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.Vector;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Service;
import org.apache.stanbol.ontologymanager.store.api.ResourceManager;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Class that relates URL(Path)s used by the RESTful interface with the URIs used by the underlying triple
 * store
 * 
 * @author gunes
 */

@Component(enabled = true)
@Service
public class ResourceManagerImp implements ResourceManager {

    private static final String DELIMITER = "|";
    
    private static final String DB_URL = "jdbc:derby:ps_db;create=true";

    private static String ontologyPathPrefix = "/ontology/";

    private static String classPathPrefix = "classes/";

    private static String dataPropertyPathPrefix = "datatypeProperties/";

    private static String objectPropertyPathPrefix = "objectProperties/";

    private static String individualPathPrefix = "individuals/";

    private static String ontologiesTable = "resource_manager_ontologies";

    private static String resourcesTable = "resource_manager_resources";

    public static String CLASS_RESOURCE = "CLASS";

    public static String DATA_PROPERTY_RESOURCE = "DATA_PROPERTY";

    public static String OBJECT_PROPERTY_RESOURCE = "OBJECT_PROPERTY";

    public static String INDIVIDUAL_RESOURCE = "INDIVIDUAL";

    Dictionary<String,String> properties = null;

    /** Logger instance **/
    private static Logger logger = LoggerFactory.getLogger(ResourceManagerImp.class);

    /* private variables to store ontology-path (URI-URL) relations */
    /* ontologyURI --> ontologyPath */
    private Hashtable<String,String> ontologies = null;

    /* ontologyPath --> ontologyURI */
    private Hashtable<String,String> ontologiesInverted = null;

    /*
     * private varibles to store resource(class/property/individual)-path (resource URI - resource URL)
     * relations
     */
    /* resourceURI --> resourcePath */
    private Hashtable<String,String> resources = null;

    /* resourcePath --> resourceURI */
    private Hashtable<String,String> resourcesInverted = null;

    /*
     * the type of the resource could be values attributed by the static variables: CLASS_RESOURCE,
     * DATA_PROPERTY_RESOURCE, OBJECT_PROPERTY_RESOURCE and INDIVIDUAL_RESOURCE
     */
    /* resourceURI --> resourceType */
    private Hashtable<String,String> resourcesTypes = null;

    /*
     * private variable to store the mapping between resource URI - ontology URI, where the resource belongs
     * to that ontology
     */
    /* resourceURI --> ontologyURI */
    private Hashtable<String,String> resourceToOntologyURIs = null;

    private static ResourceManagerImp INSTANCE = null;

    private void restore() throws IOException {
        try {
            Connection con = obtainConnection();
            Statement statement = con.createStatement();

            boolean ontologiesTableExist = checkTableExists(con, ontologiesTable);
            boolean resourceTableExist = checkTableExists(con, resourcesTable);

            if (!ontologiesTableExist) {
                Statement stmt = con.createStatement();
                String sql = "CREATE TABLE resource_manager_ontologies ("
                             + "id INTEGER NOT NULL GENERATED ALWAYS AS IDENTITY (START WITH 1, INCREMENT BY 1),"
                             + "ontologyURI varchar(255) DEFAULT NULL,"
                             + "ontologyPath varchar(255) DEFAULT NULL," + "PRIMARY KEY (id)" + ")";
                stmt.executeUpdate(sql);
            }

            if (!resourceTableExist) {
                // the table does not exist
                Statement stmt = con.createStatement();
                String sql = "CREATE TABLE resource_manager_resources (" + "id INTEGER DEFAULT NULL,"
                             + "resourceURI varchar(255) DEFAULT NULL,"
                             + "resourcePath varchar(255) DEFAULT NULL,"
                             + "resourceType varchar(255) DEFAULT NULL" + ") ";
                stmt.executeUpdate(sql);
            }

            ResultSet rSet = statement.executeQuery("SELECT * FROM " + ontologiesTable);
            while (rSet.next()) {
                String ontologyURI = rSet.getString("ontologyURI");
                String ontologyPath = rSet.getString("ontologyPath");
                ontologies.put(ontologyURI, ontologyPath);
                ontologiesInverted.put(ontologyPath, ontologyURI);
            }
            Statement statement2 = con.createStatement();
            ResultSet rSet2 = statement2.executeQuery("SELECT * from " + resourcesTable);
            while (rSet2.next()) {
                String resourceURI = rSet2.getString("resourceURI");
                String resourcePath = rSet2.getString("resourcePath");
                String resourceType = rSet2.getString("resourceType");
                String ontologyTableRowRef = rSet2.getString("id");
                Statement statement3 = con.createStatement();
                ResultSet rSet3 = statement3.executeQuery("SELECT * from " + ontologiesTable + " WHERE id="
                                                          + ontologyTableRowRef);
                if (rSet3.next()) {
                    String ontologyURIForResource = rSet3.getString("ontologyURI");
                    String ontologyPathForResource = rSet3.getString("ontologyPath");
                    resources.put(ontologyURIForResource + DELIMITER + resourceURI, resourcePath);
                    resourcesInverted.put(ontologyPathForResource + DELIMITER + resourcePath, resourceURI);
                    resourceToOntologyURIs.put(resourceURI, ontologyURIForResource);
                    resourcesTypes.put(ontologyURIForResource + DELIMITER + resourceURI, resourceType);
                }
            }
            con.close();
        } catch (Exception e) {
            throw new RuntimeException("Resource manager can not restore ", e);
        }
    }

    private boolean checkTableExists(Connection con, String tableName) throws SQLException {
        // FIXME This should be done by jdbc's table functionality but it does
        // not seem to work with Apache Derby
        Statement stmt = con.createStatement();
        String stmtString = "SELECT COUNT(*) FROM " + tableName;
        boolean exists = true;
        try {
            stmt.execute(stmtString);
        } catch (SQLException e) {
            String state = (e).getSQLState();
            if ("42X05".equals(state)) exists = false;
        } finally {
            stmt.close();
        }
        return exists;
    }

    @Activate
    public void activate(ComponentContext cc) {
        try {
            this.restore();
        } catch (IOException e) {
            throw new RuntimeException("Can not restore stored information", e);
        }
        logger.info("Resource Manager properties are set");
    }

    public ResourceManagerImp() {
        INSTANCE = this;
        ontologies = new Hashtable<String,String>();
        ontologiesInverted = new Hashtable<String,String>();
        resources = new Hashtable<String,String>();
        resourcesInverted = new Hashtable<String,String>();
        resourceToOntologyURIs = new Hashtable<String,String>();
        resourcesTypes = new Hashtable<String,String>();
    }

    public static ResourceManagerImp getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new ResourceManagerImp();
        }

        return INSTANCE;
    }

    /*
     * (non-Javadoc)
     * 
     * @seeeu.iksproject.fise.stores.persistencestore.rest.IResourceManager#
     * registerOntology(java.lang.String)
     */
    public void registerOntology(String ontologyURI) {
        try {
            if (!ontologies.containsKey(ontologyURI)) {
                String ontologyPath = normalizeURI(ontologyURI);
                ontologies.put(ontologyURI, ontologyPath);
                ontologiesInverted.put(ontologyPath, ontologyURI);

                Connection con = obtainConnection();
                Statement statement = con.createStatement();
                statement.executeUpdate("INSERT INTO " + ontologiesTable
                                        + " (ontologyURI, ontologyPath) VALUES ('" + ontologyURI + "', '"
                                        + ontologyPath + "')");
                con.close();
            }
        } catch (Exception e) {
            logger.error("Error ", e);
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @seeeu.iksproject.fise.stores.persistencestore.rest.IResourceManager# registerClass(java.lang.String,
     * java.lang.String)
     */
    public void registerClass(String ontologyURI, String classURI) {
        try {
            if (!resources.containsKey(ontologyURI + DELIMITER + classURI)) {
                String classPath = normalizeURI(classURI);
                resources.put(ontologyURI + DELIMITER + classURI, classPath);
                resourcesInverted.put(ontologies.get(ontologyURI) + DELIMITER + classPath, classURI);
                resourceToOntologyURIs.put(classURI, ontologyURI);
                resourcesTypes.put(ontologyURI + DELIMITER + classURI, CLASS_RESOURCE);
                Connection con = obtainConnection();
                Statement statement = con.createStatement();
                ResultSet rSet = statement.executeQuery("SELECT id from " + ontologiesTable
                                                        + " WHERE ontologyURI='" + ontologyURI + "'");
                if (rSet.next()) {
                    String ontologyRowID = rSet.getString("id");
                    statement.executeUpdate("INSERT INTO " + resourcesTable
                                            + " (id, resourceURI, resourcePath, resourceType) VALUES ("
                                            + ontologyRowID + ", '" + classURI + "', '" + classPath + "', '"
                                            + CLASS_RESOURCE + "')");
                }
                con.close();
            }
        } catch (Exception e) {
            logger.error("Error ", e);
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @seeeu.iksproject.fise.stores.persistencestore.rest.IResourceManager#
     * registerDatatypeProperty(java.lang.String, java.lang.String)
     */
    public void registerDatatypeProperty(String ontologyURI, String dataPropertyURI) {
        try {
            if (!resources.containsKey(ontologyURI + DELIMITER + dataPropertyURI)) {
                String dataPropertyPath = normalizeURI(dataPropertyURI);
                resources.put(ontologyURI + DELIMITER + dataPropertyURI, dataPropertyPath);
                resourcesInverted.put(ontologies.get(ontologyURI) + DELIMITER + dataPropertyPath, dataPropertyURI);
                resourceToOntologyURIs.put(dataPropertyURI, ontologyURI);
                resourcesTypes.put(ontologyURI + DELIMITER + dataPropertyURI, DATA_PROPERTY_RESOURCE);
                Connection con = obtainConnection();
                Statement statement = con.createStatement();
                ResultSet rSet = statement.executeQuery("SELECT id from " + ontologiesTable
                                                        + " WHERE ontologyURI='" + ontologyURI + "'");
                if (rSet.next()) {
                    String ontologyRowID = rSet.getString("id");
                    statement.executeUpdate("INSERT INTO " + resourcesTable
                                            + " (id, resourceURI, resourcePath, resourceType) VALUES ("
                                            + ontologyRowID + ", '" + dataPropertyURI + "', '"
                                            + dataPropertyPath + "', '" + DATA_PROPERTY_RESOURCE + "')");
                }
                con.close();
            }
        } catch (Exception e) {
            logger.error("Error ", e);
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @seeeu.iksproject.fise.stores.persistencestore.rest.IResourceManager#
     * registerObjectProperty(java.lang.String, java.lang.String)
     */
    public void registerObjectProperty(String ontologyURI, String objectPropertyURI) {
        try {
            if (!resources.containsKey(ontologyURI + DELIMITER + objectPropertyURI)) {
                String objectPropertyPath = normalizeURI(objectPropertyURI);
                resources.put(ontologyURI + DELIMITER + objectPropertyURI, objectPropertyPath);
                resourcesInverted.put(ontologies.get(ontologyURI) + DELIMITER + objectPropertyPath,
                    objectPropertyURI);
                resourceToOntologyURIs.put(objectPropertyURI, ontologyURI);
                resourcesTypes.put(ontologyURI + DELIMITER + objectPropertyURI, OBJECT_PROPERTY_RESOURCE);
                Connection con = obtainConnection();
                Statement statement = con.createStatement();
                ResultSet rSet = statement.executeQuery("SELECT id from " + ontologiesTable
                                                        + " WHERE ontologyURI='" + ontologyURI + "'");
                if (rSet.next()) {
                    String ontologyRowID = rSet.getString("id");
                    statement.executeUpdate("INSERT INTO " + resourcesTable
                                            + " (id, resourceURI, resourcePath, resourceType) VALUES ("
                                            + ontologyRowID + ", '" + objectPropertyURI + "', '"
                                            + objectPropertyPath + "', '" + OBJECT_PROPERTY_RESOURCE + "')");
                }
                con.close();
            }
        } catch (Exception e) {
            logger.error("Error ", e);
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @seeeu.iksproject.fise.stores.persistencestore.rest.IResourceManager#
     * registerIndividual(java.lang.String, java.lang.String)
     */
    public void registerIndividual(String ontologyURI, String individualURI) {
        try {
            if (!resources.containsKey(ontologyURI + DELIMITER + individualURI)) {
                String individualPath = normalizeURI(individualURI);
                resources.put(ontologyURI + DELIMITER + individualURI, individualPath);
                resourcesInverted.put(ontologies.get(ontologyURI) + DELIMITER + individualPath, individualURI);
                resourceToOntologyURIs.put(individualURI, ontologyURI);
                resourcesTypes.put(ontologyURI + DELIMITER + individualURI, INDIVIDUAL_RESOURCE);
                Connection con = obtainConnection();
                Statement statement = con.createStatement();
                ResultSet rSet = statement.executeQuery("SELECT id from " + ontologiesTable
                                                        + " WHERE ontologyURI='" + ontologyURI + "'");
                if (rSet.next()) {
                    String ontologyRowID = rSet.getString("id");
                    String statementstr = "INSERT INTO " + resourcesTable
                                          + " (id, resourceURI, resourcePath, resourceType) VALUES ("
                                          + ontologyRowID + ", '" + individualURI + "', '" + individualPath
                                          + "', '" + INDIVIDUAL_RESOURCE + "')";
                    statement.executeUpdate(statementstr);
                }
                con.close();
            }
        } catch (Exception e) {
            logger.error("Error ", e);
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * 
     * org.apache.stanbol.persistencestore.rest.IResourceManager#hasOntology (java.lang.String)
     */
    public boolean hasOntology(String ontologyURI) {
        boolean result = ontologies.containsKey(ontologyURI);
        return result;
    }

    /*
     * (non-Javadoc)
     * 
     * @seeeu.iksproject.fise.stores.persistencestore.rest.IResourceManager# getOntologyPath(java.lang.String)
     */
    public String getOntologyPath(String ontologyURI) {
        String result = null;
        if (ontologies.containsKey(ontologyURI)) {
            result = ontologies.get(ontologyURI);
        }
        return result;
    }

    /*
     * (non-Javadoc)
     * 
     * @seeeu.iksproject.fise.stores.persistencestore.rest.IResourceManager#
     * getOntologyFullPath(java.lang.String)
     */
    public String getOntologyFullPath(String ontologyURI) {
        String result = null;
        if (ontologies.containsKey(ontologyURI)) {
            result = ontologyPathPrefix + ontologies.get(ontologyURI);
        }
        return result;
    }

    /*
     * (non-Javadoc)
     * 
     * @seeeu.iksproject.fise.stores.persistencestore.rest.IResourceManager#
     * getResourceFullPath(java.lang.String)
     */
    public String getResourceFullPath(String resourceURI) {
        String result = null;
        String ontologyURI = resourceToOntologyURIs.get(resourceURI);
        if (ontologies.containsKey(ontologyURI) && resources.containsKey(ontologyURI + DELIMITER + resourceURI)) {
            String resourceType = resourcesTypes.get(ontologyURI + DELIMITER + resourceURI);
            String pathPrefix = null;
            if (resourceType.equalsIgnoreCase(CLASS_RESOURCE)) {
                pathPrefix = classPathPrefix;
            } else if (resourceType.equalsIgnoreCase(OBJECT_PROPERTY_RESOURCE)) {
                pathPrefix = objectPropertyPathPrefix;
            } else if (resourceType.equalsIgnoreCase(DATA_PROPERTY_RESOURCE)) {
                pathPrefix = dataPropertyPathPrefix;
            } else if (resourceType.equalsIgnoreCase(INDIVIDUAL_RESOURCE)) {
                pathPrefix = individualPathPrefix;
            }
            result = ontologyPathPrefix + ontologies.get(ontologyURI) + "/" + pathPrefix
                     + resources.get(ontologyURI + DELIMITER + resourceURI);
        }
        return result;
    }

    /*
     * (non-Javadoc)
     * 
     * @seeeu.iksproject.fise.stores.persistencestore.rest.IResourceManager#
     * getOntologyURIForPath(java.lang.String)
     */
    public String getOntologyURIForPath(String ontologyPath) {
        String result = null;
        if (ontologiesInverted.containsKey(ontologyPath)) {
            result = ontologiesInverted.get(ontologyPath);
        }
        return result;
    }

    /*
     * (non-Javadoc)
     * 
     * @seeeu.iksproject.fise.stores.persistencestore.rest.IResourceManager#
     * getResourceURIForPath(java.lang.String, java.lang.String)
     */
    public String getResourceURIForPath(String ontologyPath, String resourcePath) {
        String result = null;
        if (resourcesInverted.containsKey(ontologyPath + DELIMITER + resourcePath)) {
            result = resourcesInverted.get(ontologyPath + DELIMITER + resourcePath);
        }
        return result;
    }

    /*
     * (non-Javadoc)
     * 
     * @seeeu.iksproject.fise.stores.persistencestore.rest.IResourceManager# getResourceType(java.lang.String)
     */
    public String getResourceType(String resourceURI) {
        String result = resourcesTypes.get(resourceURI);
        return result;
    }

    /*
     * (non-Javadoc)
     * 
     * @seeeu.iksproject.fise.stores.persistencestore.rest.IResourceManager# removeOntology(java.lang.String)
     */
    public void removeOntology(String ontologyURI) {
        try {
            // first clear hashtables
            String ontologyPath = ontologies.get(ontologyURI);
            if (ontologyPath == null) {
                logger.warn("Ontology {} already deleted ", ontologyURI);
                return;
            }
            ontologies.remove(ontologyURI);
            ontologiesInverted.remove(ontologyPath);

            List<String> keysToRemove = new Vector<String>();
            Set<String> resources_keys = resources.keySet();
            Iterator<String> resources_keys_itr = resources_keys.iterator();
            while (resources_keys_itr.hasNext()) {
                String key = resources_keys_itr.next();
                //FIXME were startswith
                if (key.split("\\"+DELIMITER)[0].contentEquals(ontologyURI)) {
                    keysToRemove.add(key);
                }
            }
            Iterator<String> keysToRemoveItr = keysToRemove.iterator();
            while (keysToRemoveItr.hasNext()) {
                resources.remove(keysToRemoveItr.next());
            }

            keysToRemove = new Vector<String>();
            Set<String> resourcesInverted_keys = resourcesInverted.keySet();
            Iterator<String> resourcesInverted_keys_itr = resourcesInverted_keys.iterator();
            while (resourcesInverted_keys_itr.hasNext()) {
                String key = resourcesInverted_keys_itr.next();
                //FIXME were startswith
                if (key.split("\\"+DELIMITER)[0].contentEquals(ontologyURI)) {
                    keysToRemove.add(key);
                }
            }
            keysToRemoveItr = keysToRemove.iterator();
            while (keysToRemoveItr.hasNext()) {
                resourcesInverted.remove(keysToRemoveItr.next());
            }

            keysToRemove = new Vector<String>();
            Set<String> resourcesTypes_keys = resourcesTypes.keySet();
            Iterator<String> resourcesTypes_keys_itr = resourcesTypes_keys.iterator();
            while (resourcesTypes_keys_itr.hasNext()) {
                String key = resourcesTypes_keys_itr.next();
                //FIXME were startswith
                if (key.split("\\"+DELIMITER)[0].contentEquals(ontologyURI)) {
                    keysToRemove.add(key);
                }
            }
            keysToRemoveItr = keysToRemove.iterator();
            while (keysToRemoveItr.hasNext()) {
                resourcesTypes.remove(keysToRemoveItr.next());
            }

            keysToRemove = new Vector<String>();
            Set<String> resourceToOntologyURIs_keys = resourceToOntologyURIs.keySet();
            Iterator<String> resourceToOntologyURIs_keys_itr = resourceToOntologyURIs_keys.iterator();
            while (resourceToOntologyURIs_keys_itr.hasNext()) {
                String key = resourceToOntologyURIs_keys_itr.next();
                String value = resourceToOntologyURIs.get(key);
                if (value.equalsIgnoreCase(ontologyURI)) {
                    keysToRemove.add(key);
                }
            }
            keysToRemoveItr = keysToRemove.iterator();
            while (keysToRemoveItr.hasNext()) {
                resourceToOntologyURIs.remove(keysToRemoveItr.next());
            }

            // then clear database
            Connection con = obtainConnection();
            Statement statement = con.createStatement();
            String sql = "SELECT id from " + ontologiesTable + " WHERE ontologyURI='" + ontologyURI + "'";
            ResultSet rSet = statement.executeQuery(sql);
            String id = null;
            if (rSet.next()) {
                id = rSet.getString("id");
            }

            statement = con.createStatement();
            sql = "DELETE from " + ontologiesTable + " WHERE id=" + id;
            statement.executeUpdate(sql);

            statement = con.createStatement();
            sql = "DELETE from " + resourcesTable + " WHERE id=" + id;
            statement.executeUpdate(sql);

            con.close();

        } catch (Exception e) {
            logger.error("Error ", e);
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @seeeu.iksproject.fise.stores.persistencestore.rest.IResourceManager# removeResource(java.lang.String)
     */
    public void removeResource(String resourceURI) {
        try {
            // first clear hashtables
            String classPath = resources.get(resourceURI);
            if (classPath == null) {
                logger.warn("Resource {} not found", resourceURI);
                return;
            }

            String ontologyURI = resourceToOntologyURIs.get(resourceURI);
            String ontologyPath = ontologies.get(ontologyURI);
            resources.remove(ontologyURI + DELIMITER + resourceURI);
            resourcesInverted.remove(ontologyPath + DELIMITER + classPath);
            resourcesTypes.remove(ontologyURI + DELIMITER + resourceURI);
            resourceToOntologyURIs.remove(resourceURI);

            // then clear database
            Connection con = obtainConnection();
            Statement statement = con.createStatement();
            String sql = "DELETE from " + resourcesTable + " WHERE resourceURI='" + resourceURI + "'";
            statement.executeUpdate(sql);
            con.close();
        } catch (Exception e) {
            logger.error("Error ", e);
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @seeeu.iksproject.fise.stores.persistencestore.rest.IResourceManager#
     * resolveOntologyURIFromResourceURI(java.lang.String)
     */
    public String resolveOntologyURIFromResourceURI(String resourceURI) {
        String result = null;
        if (resourceToOntologyURIs.containsKey(resourceURI)) {
            result = resourceToOntologyURIs.get(resourceURI);
        }
        return result;
    }

    /*
     * (non-Javadoc)
     * 
     * @seeeu.iksproject.fise.stores.persistencestore.rest.IResourceManager# clearResourceManager()
     */
    public void clearResourceManager() {
        try {
            // first clear hashtables
            ontologies.clear();
            ontologiesInverted.clear();
            resources.clear();
            resourcesInverted.clear();
            resourcesTypes.clear();
            resourceToOntologyURIs.clear();

            // then clear database
            Connection con = obtainConnection();
            boolean ontologiesTableExists = checkTableExists(con, ontologiesTable);
            boolean resourceTableExists = checkTableExists(con, resourcesTable);
            Statement statement = con.createStatement();
            if (ontologiesTableExists) {
                String sql1 = "DELETE  FROM " + ontologiesTable;
                statement.executeUpdate(sql1);
            }
            if (resourceTableExists) {
                statement = con.createStatement();
                String sql2 = "DELETE FROM " + resourcesTable;
                statement.execute(sql2);
            }
            con.close();
            logger.info("Connection Closed");
        } catch (Exception e) {
            logger.error("Error ", e);
        }
    }

    public Connection obtainConnection() throws InstantiationException,
                                        IllegalAccessException,
                                        ClassNotFoundException,
                                        SQLException {
        return DriverManager.getConnection(DB_URL);
    }

    private String normalizeURI(String uri) {
        // FIXME Find a solution to # problem
        return uri.replace("#", "/");
    }

    @Override
    public String convertEntityRelativePathToURI(String entityPath) {
        return entityPath.substring(0, entityPath.lastIndexOf("/")) + "#"
               + entityPath.substring(entityPath.lastIndexOf("/") + 1);
    }
}
