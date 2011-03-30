IKS FISE Persistence Store
==========================
This bundle defines a persistence store interface through org.apache.stanbol.ontologymanager.store.PersistenceStore.   

Run
===
Sling launcher can be used to create the FISE launcher with persistence store deployed on it. 
Other instructions can be found at /launchers/fise/README.txt 


Restful Services
================
Following paths can be used to explore persistence store with prefix http://localhost:8080/ontologymanager/store/
If here is no JAX-RS handler in system to bind rest resources, then the component eu.iksproject.fise.stores.persistencestore.rest.ServletRegisterer can be activated to register a servlet which contains the rest resources.
The servlet alias is /persistencestore and can be configured through the property eu.iksproject.fise.stores.persistencestore.rest.alias.name.

ontologies
ontologies/{ontologypath}
ontologies/{ontologypath}/classes
ontologies/{ontologyPath}/classes/{classPath}
ontologies/{ontologypath}/individuals
ontologies/{ontologypath}/datatypeProperties
ontologies/{ontologyPath}/datatypeProperties/{datatypePropertyPath}
ontologies/{ontologypath}/individuals
ontologies/{ontologyPath}/individuals/{individualPath}
ontologies/{ontologypath}/objectProperties
ontologies/{ontologyPath}/objectProperties/{objectPropertyPath}

To access some of these services you may need to give following permissions from http://localhost:8080/admin/user-manager/manage-role-permissions?roleTitle=BasePermissionsRole

( java.net.SocketPermission "localhost:3306" "resolve, connect")
( java.util.PropertyPermission "user.language" "write" )
( java.io.FilePermission "*" "read" )
( java.lang.RuntimePermission "accessDeclaredMembers")
( java.lang.reflect.ReflectPermission "suppressAccessChecks" )

