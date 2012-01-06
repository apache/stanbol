<!--
  Licensed to the Apache Software Foundation (ASF) under one or more
  contributor license agreements.  See the NOTICE file distributed with
  this work for additional information regarding copyright ownership.
  The ASF licenses this file to You under the Apache License, Version 2.0
  (the "License"); you may not use this file except in compliance with
  the License.  You may obtain a copy of the License at

      http://www.apache.org/licenses/LICENSE-2.0

  Unless required by applicable law or agreed to in writing, software
  distributed under the License is distributed on an "AS IS" BASIS,
  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  See the License for the specific language governing permissions and
  limitations under the License.
-->

# Stanbol Commons Solr

Solr is used by several Apache Stanbol components. The Apache Stanbol Solr Commons artifacts provide a set of utilities that ease the use of Solr within OSGi, allow the initialization and management of Solr indexes as well as the publishing of Solrs RESTful interface on the OSGi HttpService.

Although this utilities where implemented with the requirements of Apache Stanbol in mind they do not depend on other Stanbol components that are not themselves part of
"stanbol.commons".


## Solr OSGi Bundle

The "org.apache.commons.solr.core" bundle currently includes all dependencies required by Solr and also exports the client as well as the server API. For details please have a look at the pom file of the "solr.core" artifact.

Please note also the exclusion list, because some libraries currently not directly used by Stanbol are explicitly excluded. Using such features within a "solrConf.xml" or "schema.xml" will result in "ClassNotFoundException" and "ClassNotFoundErrors".

If you require an additional Library that is currently not included please give us a short notice on the stanbol-dev mailing list.


## Solr Server Components

This section provides information how to managed and get access to the server side CoreContainer and SolrCore components of Solr.


### Accessing CoreContainers and SolrCores

All CoreContainer and SolrCores initialized by the Stanbol Solr framework are registered with the OSGi Service Registry. This means that other Bundels can obtain them by using

    CoreContainer defaultSolrServer;
    ServiceReference ref = bundleContext.getServiceReference(
        CoreContainer.class.getName())
    if (ref != null) {
        defaultSolrServer = (CoreContainer) bundleContext.getService(ref);
    } else {
        defaultSolrServer = null; //no SolrServer available
    }

It is also possible to track service registration and unregistration events by using the OSGi ServiceTracker utility.

The above Code snippet would always return the SolrServer with the highest priority (the highest value for the "service.ranking" property). However the OSGi Service Registry allows also to obtain/track service by the usage of filters. For specifying such filters it is important to know what metadata are provided when services are registered with the OSGi Service Registry.


#### Metadata for CoreContainer:

* **org.apache.solr.core.CoreContainer.name**: The name of the SolrServer. The name MUST BE provided for each Solr CoreContainer registered with this framework. It is a required field for each configuration. If two CoreContainers are registered with the same name the "service.ranking" property shall be used to determine the current active CoreContainer for an request. However others registered for the same name may be used as fallbacks. The container name is used as a URL path component when the `publishREST` parameter is true. It is recommended to use lowercase names without non ASCII characters.
* **org.apache.solr.core.CoreContainer.dir**: The directory of a CoreContainer. This is the directory containing the "solr.xml" file.
* **org.apache.solr.core.CoreContainer.solrXml**: The name of the Solr CoreContainer configuration file. Currently always "sold.xml".
* **org.apache.solr.core.CoreContainer.cores**: A read only collection of the names of all cores registered with the CoreContainer.
* **service.ranking**: The OSGi "service.ranking" property is used to specify the ranking of a CoreContainer. The CoreContainer with the highest ranking is considered as the default server and will be returned by calls to bundleContext.getServiceReference(..) without the use of an filter.
* **org.apache.solr.core.CoreContainer.publishREST**: Boolean switch that allows to enable/disable the publishing of the Solr RESTful API on "http://{host}:{port}/solr/{server-name}". Requires the "SolrServerPublishingComponent" to be active.


#### Metadata for SolrCores:

* **org.apache.solr.core.SolrCore.name**: The name of the SolrCore as registered with the CoreContainer
* **org.apache.solr.core.SolrCore.dir**: The instance directory of the SolrCore
* **org.apache.solr.core.SolrCore.datadir**: The data directory of the SolrCore
* **org.apache.solr.core.SolrCore.indexdir**: The directory of the index used by this SolrCore
* **org.apache.solr.core.SolrCore.schema**: The name (excluding the directory) of the Solr schema used by this core
* **org.apache.solr.core.SolrCore.solrconf**: The name (excluding the directory) of the Solr core configuration file

In addition the following metadata of the CoreContainer for this SolrCore are also available

* **org.apache.solr.core.CoreContainer.id**: The `SERVICE_ID` of the CoreContainer this SolrCore is registered with. This is usually the easiest way to obtain the ServiceReference to the CoreContainer of an SolrCore.
* **org.apache.solr.core.CoreContainer.name**: The name of the CoreContainer this SolrCore is registered with. Note that multiple CoreContainers may be registered for the same name. Therefore this property MUST NOT be used to filter for the ServiceReference to the CoreContainer of an SolrCore.
* **org.apache.solr.core.CoreContainer.dir**: The Solr directory of the CoreContainer for this SolrCore.
* **service.ranking**: The OSGi service.ranking of the CoreContainer this SolrCore is registered with. SolrCores do not define there own service.ranking but use the ranking of the CoreContainer they are registered with.

The the mentioned keys used for metadata of registered CoreContainer and SolrCores are defined as public constants in the [SolrConstants](http://svn.apache.org/repos/asf/incubator/stanbol/trunk/commons/solr/core/src/main/java/org/apache/stanbol/commons/solr/SolrConstants.java) class.


### ReferencedSolrServer

This component allows to initialize a Solr server running within the same JVM as Stanbol based on indexes provided by a directory on the local file system. This does not support management capabilities, but it initializes a Solr CoreContainer based on the data in the file system and registers it (including all SolrCores) with the OSGi Service Registry as described above.

The ReferencedSolrServer uses the ManagedServiceFactory pattern. This means that instances are created by parsing configurations to the OSGi ConfigurationAdmin service. Practically this means that:

* users can create instances by using the Configuration tab of the Apache Felix Web Console
* programmers can directly use the ConfigurationAdmin service to create/update and delete configurations
* Configurations can also parsed via the Apache Sling [OSGi installer](http://sling.apache.org/site/osgi-installer.html) framework. Meaning configurations can be includes within the Stanbol launchers, Bundles or copied to a directory configured for the [File Provider](http://svn.apache.org/repos/asf/sling/trunk/installer/providers/file/)

Configurations need to include the following properties (see also section "Metadata for CoreContainer" for details about such properties)

* **org.apache.solr.core.CoreContainer.name**: The name for the Solr Server
* **org.apache.solr.core.CoreContainer.dir**: The path to the directory on the local file system that is used to initialize the CoreContainer
* **service.ranking**: The OSGi service ranking used to register the CoreContainer and its SolrCores. If not specified '0' will be used as default. The value MUST BE an integer number.
* **org.apache.solr.core.CoreContainer.publishREST**: Boolean switch that allows to enable/disable the publishing of the Solr RESTful API on "http://{host}:{port}/solr/{server-name}". Requires the "SolrServerPublishingComponent" to be active.

**NOTE:** Keep in mind that of the RESTful API of the SolrServer is published users might use the Admin Request handler to manipulate the SolrConfiguration. In such cases the metadata provided by the ServiceReferences for the CoreContainer and SolrCores might get out of sync with the actual configuration of the Server.


### ManagedSolrServer

This component allows to manage a multi core Solr server. It provides an API to create, update and remove SolrCores. In addition cores can be activated and deactivated.


#### Creating ManagedServerInstances

The ManagedSolrServer uses the ManagedServiceFactory pattern. This means that instances are created by parsing configurations to the OSGi ConfigurationAdmin service. Practically this means that:

* users can create instances by using the Configuration tab of the Apache Felix Web Console
* programmers can directly use the ConfigurationAdmin service to create/update and delete configurations
* Configurations can also parsed via the Apache Sling [OSGi installer](http://sling.apache.org/site/osgi-installer.html) framework. Meaning configurations can be includes within the Stanbol launchers, Bundles or copied to a directory configured for the [File Provider](http://svn.apache.org/repos/asf/sling/trunk/installer/providers/file/)

Configurations need to include the following properties (see also section "Metadata for CoreContainer" for details about such properties). Although the properties are the same as for the ReferencedSolrServer their semantics differs in some aspects.

* **org.apache.solr.core.CoreContainer.name**: The name for the Solr Server
* **org.apache.solr.core.CoreContainer.dir**: Optionally an directory to store the data. If not specified the data will be stored in an directory with the configured server-name at the default location (currently "${sling.home}/indexes/" or "indexes/" if the environment variable 'sling.home' is not present). Users that want to create multiple ManagedSolrServer with the same name need to specify the directory or servers will override each others data.
* **service.ranking**: The OSGi service ranking used to register the CoreContainer and its SolrCores. If not specified '0' will be used as default. The value MUST BE an integer number. In scenarios where a single ManagedSolrServer is expected it is highly recommended to specify `Integer.MAX_VALUE` (2147483647) as service ranking. This will ensure that this server can not be overridden by others.
* **org.apache.solr.core.CoreContainer.publishREST**: Boolean switch that allows to enable/disable the publishing of the Solr RESTful API on "http://{host}:{port}/solr/{server-name}". Requires the "SolrServerPublishingComponent" to be active.

**NOTE:** Keep in mind that of the RESTful API of the SolrServer is published users might use the Admin Request handler to manipulate the SolrConfiguration. In such cases the metadata provided by the ServiceReferences for the CoreContainer and SolrCores might get out of sync with the actual configuration of the Server.


#### Managing Solr Indexes

This describes how to manage (create, update, remove, activate, deactivate) Indexes on a ManagedSolrServer.

Managed Indexes do not 1:1 correspond to SolrCores registered on the CoreContainer. However all SolrCores on the CoreContainer do have a 1:1 mapping with a managed index on the Managed SolrServer.

Managed Index can be in one of the following States (defined by the ManagedIndexState enumeration):

* **UNINITIALISED**: An index that was created but is still missing the configuration and/or index data is in that state. The ManagedSolrServer API allows to create indexes by referring to a Solr-Index-Archive. Such archives are than requested via the Stanbol DataFileProvider service. Usually users can provide them by copying the lined index to the "/sling/datafiles" folder.
* **INACTIVE**: This indicated that an index is was deactivated via the ManagedSolrServer API. The data are still kept, but the SolrCore was removed from the CoreContainer.
* **ACTIVE**: This indicates that an index is active and can be used. Only Indexes that are ACTIVE are registered with the CoreContainer.
* **ERROR**: This state indicates some error during the the initialization. The stack trace of the error is available in the IndexMetadata.

Indexes can not only be managed by calls to the API of the ManagedSolrServer. The "org.apache.stanbol.commons.solr.install" bundle provides also support for installing/uninstalling indexes by using the Apache Sling [OSGi installer](http://sling.apache.org/site/osgi-installer.html) framework. This allows to install indexes by providing Solr-Index-Archives or Solr-Index-Archive-References to any available Provider. By default Apache Stanbol includes Provider for the Launchers and Bundles. However the Sling Installer Framework also includes Providers for Directories on the File and JCR Repositories.

Solr-Index-Archives do use the following name pattern:

    {name}.solrindex[.zip|.gz|.bz2]

* They are normal achieves starting with the instance directory of a Solr Core.
* The name of this instance directory MUST BE the same as the {name} of the archive.
* The second extensions specifies the type of the archive. If no extension is specified the type of the Archive might still be detected by reading the first few bytes of the Archive.

Solr-Index-Archive-References are normal Java properties files and do use the following name pattern:

    {name}.solrindex.ref

The following keys are used (see also org.apache.stanbol.commons.solr.managed.ManagedIndexConstants):

* **Index-Archive**: Comma separated list of Solr-Index-Archives that can be used for initializing this index. The first index archive in the list has the highest priority. Higher priority archives will replace the data of lower priority once as soon as they become available. This feature is intended to be used to allow the replacement of a small sample dataset (e.g. shipped within a Bundle or the Launcher) with the full dataset download later from a remote Internet archive or pushed manually to the `sling/datafiles` folder of a previously installed Stanbol instance. For instance the `dbpedia.solrindex.ref` archive reference configuration provided in the default launcher has the line: `Index-Archive=dbpedia.solrindex.zip,dbpedia_43k.solrindex.zip` and only `dbpedia_43k.solrindex.zip` is shipped in the default launchers allowing for override by any archive named `dbpedia.solrindex.zip`.
* **Index-Name**: The name of the Index. If not specified the {name} part of the first Index-Archive in the list will be used.
* **Server-Name**: The name of the ManagedSolrServer this Solr index MUST BE deployed on. If not present it will be deployed on the default ManagedSolrServer (the ManagedSolrServer with the highest priority.
* **Synchronized**: Boolean switch. If enabled the index will be synchronized with the referenced Solr-Index-Archives. That means the DataFileTracker service will be used to periodically track the states of referenced Solr-Index-Archives. This allows to initialize/update and uninitialise managed Solr indexes by simple making Solr-Index-Archives un-/available to the DataFileProvider infrastructure (such as Users copying/deleting files in the "/sling/datafiles" directory).
* **other Properties**: All parsed properties are forwarded to the DataFileProvider/DataFileTracker service when looking for the referenced Solr-Index-Archives. This components might also define some special keys associated with specific functionalities. Please look at the documentation of this services for details.


#### Other interesting Notes

* SolrCore directory names created by the ManagedSolrServer use the current date as suffix. If a directory with that name already exists (e.g. because the same index was already updated on the very same day) than an additional "-{count}" suffix will be added to the end.
* The Managed SolrServer stores its configuration within the persistent space of the Bundle provided by the OSGi environment. When using one of the default Stanbol launchers this is within "{sling.home}/felix/bundle{bundle-id}/data". The "{bundle-id}" of the "org.apache.stanbol.commons.solr.managed" bundle can be looked up the the [Bundle tab](http://localhost:8080/system/console/bundles) of the Apache Felix Webconsole. The actual configuration of a ManagedSolrServer is than in ".config/index-config/{service.pid}". The "{service.pid}" can be also looked up via the Apache Felix Web-console in the [Configuration Status tab](http://localhost:8080/system/console/config). Within this folder the Solr index reference files (normal java properties files) with all the information about the current state of the managed indexes are present.
* Errors that occur during the asynchronous initialization of SolrCores are stored within the IndexingProperties. They can therefore be requested via the API of the ManagedSolrServer but also be looked up within the persistent state of the ManagedSolrServer (see above where such files are located).


## Solr Client Components

This sections describes how to use Solr servers and indexes referenced and managed by the "org.apache.stanbol.commons.solr" framework.
Principally there are two possibilities: (1) to directly access Solr indexes via the SolrServer Java API and (2) to publish locally managed index on the OSGi HttpService and than use such indexes via the Solr RESTful API.

The Stanbol Solr framework does not provide utilities for accessing remote Solr servers, because this is already easily possible by using SolrJ.


### Java API

This describes how to lookup and access a Solr Server initialized by the "org.apache.stanbol.commons.solr" framework. The client side Java API of Solr is defined by the SolrServer abstract class. The implementation used for accessing a SolrCore running in the same JVM is the EmbeddedSolrServer.

All Solr server (CoreContainer) and Solr indexes (SolrCore) initialized by the ReferencedSolrServer and/or ManagedSolrServer are registered with the OSGi service registry. More information about this can be found in the first part of the "Solr Server Components" of this documentation.

OSGi already provides APIs and utilities to lookup and track registered services. In the following I will provide some examples how to lookup SolrServers registered as OSGi services.


#### IndexReference

The IndexReference is a Java class that manages a reference to an Index. It defines a constructor that takes a serverName and coreName. In addition there is a static parse(String ref) method that takes

* file URLs
* file paths and
* [server-name:]core-name like references.

The IndexMetadata class also defines a getter to get the IndexReference.

One feature of the IndexReference is also that it provides getters of Filters as used to lookup/track the referenced CoreContainer/SolrCore in the OSGi service Registry. The returned filter include the constraint for the registered interface (OBJECTCLASS). Therefore when using this filters one can parse NULL for the class parameter

To lookup the CoreContainer of the referenced index:

    bundleContext.getServiceReferences(null, indexReference.getServerFilter());

To lookup the SolrCore for the referenced index:

    bundleContext.getServiceReferences(null, indexReference.getIndexFilter());


#### Lookup Solr Indexes

This example shows how to lookup the default CoreContainer and create a SolrServer for the core "mydata".

    ComponentContext context; // typically passed to the activate method
    BundleContext bc = context.getBundleContext();
    ServiceReference coreContainerRef =
        bc.getServiceReference(CoreContainer.class.getName());
    CoreContainer coreContainer = (CoreContainer) bc.getService(coreContainerRef)
    SolrServer server = new EmbeddedSolrServer(coreContainer, "mydata");

Now there might be cases where several CoreContainers are available and "mydata" is not available on the default one. The "default" refers to the one with the highest "service.ranking" value. In this case we need to know a available property we can use to filter for the right CoreContainer. In this case we assume the index is on a CoreContainer registered with the name "myserver".

    ComponentContext context; // typically passed to the activate method
    BundleContext bc = context.getBundleContext();

    // Now let's use the IndexReference to create the filter
    IndexReference indexRef = new IndexReference("myserver", "mydata");
    ServiceReference[] coreContainerRefs = bc.getServiceReferences(
        null, indexRef.getServerFilter());

    // TODO: check that coreContainerRefs != null AND not empty!
    // Now we have all References to CoreContainers with the name "myserver"
    // Yes one can register several for the same name (e.g. to have fallbacks)
    // let get the one with the highest service.ranking
    Arrays.sort(coreContainerRefs, ServiceReferenceRankingComparator.INSTANCE);

    // Create the SolrServer (same as above)
    CoreContainer coreContainer = (CoreContainer) bc.getService(coreContainerRefs[0])
    SolrServer server = new EmbeddedSolrServer(coreContainer, indexRef.getIndex());

In cases where one only knows the name of the SolrCore (and not the CoreContainer) the initialization looks like this.

    ComponentContext context; // typically passed to the activate method
    BundleContext bc = context.getBundleContext();
    String nameFilter = String.format("(%s=%s)", SolrConstants.PROPERTY_CORE_NAME, "mydata");
    ServiceReference[] solrCoreRefs = bc.getServiceReferences(
        SolrCore.class.getName(), nameFilter);

    // TODO: check that != null AND not empty!
    // Now we have all References to CoreContainer with a SolrCore "mydata"
    // let get the one with the highest service.ranking
    Arrays.sort(solrCoreRefs, ServiceReferenceRankingComparator.INSTANCE);

    // Now get the SolrCore and create the SolrServer
    SolrCore core = (SolrCore) bc.getService(solrCoreRefs[0]);

    // core.getCoreDescriptor() might be null if SolrCore is not
    // registered with a CoreContainer
    SolrServer server = new EmbeddedSolrServer(
        core.getCoreDescriptor().getCoreContainer(), "mydata");


#### Tracking Solr Indexes

The above examples do a lookup at a single point in time. However because OSGi is an dynamic environment where services can come the go at every time in most cases users might rather want to track services. To do this OSGi provides the ServiceTracker utility.

To ease the tracking of SolrServers the "org.apache.stanbol.commons.solr.core" bundle provides the RegisteredSolrServerTracker. The following examples show how to create a Managed SolrIndex and than track the SolrServer.

First during the activation we need to check if "mydata" is already created and create it if not. Than we can start tracking the index:

    BundleContext bc;
    // The ManagedSolrServer instance can be looked up manually using a service
    // reference or using declarative services / SCR injection
    IndexMetadata metadata = managedServer.getIndexMetadata("mydata");
    if (metadata == null) {
        // No index with that name:
        // Asynchronously init the index as soon as the solrindex archive is available
        metadata = managedServer.createSolrIndex("mydata", "mydata.solrindex.zip", null);
    }
    RegisteredSolrServerTracker indexTracker =
        new RegisteredSolrServerTracker(bc, metadata.getIndexReference());

    // Do not forget to close the tracker while deactivating
    indexTracker.open();

Now every time we need the SolrServer we can retrieve it from the indexTracker

    private SolrServer getServer() {
        SolrServer server = indexTracker.getService();
        if(server == null) {
            // Report the missing server
            throw new IllegalStateException("Server 'mydata' not active");
        } else {
            return server;
        }
    }

The RegisteredSolrServerTracker does take "service.ranking" into account. So if there are more Services available that match the passed IndexReference those methods will always return the one with the highest "service.ranking". In case arrays are returned such arrays are sorted accordingly.


### RESTful API

The following describes how to publish the RESTful API of CoreContainer registered as OSGi services on the OSGi HttpService. The functionality described in this section is provided by the "org.apache.stanbol.commons.solr.web" artifact.


#### SolrServerPublishingComponent

This is an OSGi component that starts immediate and does not require a configuration. Its main purpose is to track all CoreContainers with the property "org.apache.solr.core.CoreContainer.publishREST=true". For all such CoreContainers it publishes the RESTful API under the URL

    http://{host}:{port}/solr/{server-name}

If two CoreContainers with the same {server-name} (the value of the "org.apache.solr.core.CoreContainer.name" property) are registered the one with the highest "service.ranking" is published.

The root-prefix ("/solr" by default) can be configured by setting the "org.apache.stanbol.commons.solr.web.dispatchfilter.prefix" property.


#### SolrDispatchFilterComponent

This Component provides the same functionality as the SolrServerPublishingComponent, but can be configured specifically for a CoreContainer. It is intended to be used if one wants to publish the RESTful API of a specific CoreContainer under a specific location. To deactivate the publishing of the same core on the SolrServerPublishingComponent users need to set the "org.apache.solr.core.CoreContainer.publishREST" to false.

This component is configured by two properties

* **org.apache.stanbl.commons.solr.web.dispatchfilter.name**: The {server-name} of the CoreContainer to publish ({server-name} refers to the value of the "org.apache.solr.core.CoreContainer.name" property).
* **org.apache.stanbl.commons.solr.web.dispatchfilter.prefix**: The prefix path to publish the server. The {server-name} is NOT appended to the configured prefix. Note that a Servlet Filter with `{prefix}/.*` is registered with the OSGi HttpService.

If two CoreContainers with the same {server-name} (the value of the "org.apache.solr.core.CoreContainer.name" property) are registered the one with the highest "service.ranking" is published.

