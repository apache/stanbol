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

# Apache Stanbol CMS Adapter

CMS Adapter component aims to integrate content management systems with Apache Stanbol. The functionalities provided by CMS Adapter undertakes the bridging role between Apache Stanbol and content management systems. 

## Bidirectional Mappings Between JCR/CMIS Compliant Content Management Systems and RDF Data

This feature enables users to update existing content repository items or create new ones based on the RDF data specified. The other direction of this feature makes generating RDF from content repository possible. This functionalities are available as RESTful services. After running the Full Launcher of Apache Stanbol with default configurations (See the section *Building and Launching CMS Adapter*), this functionality is available under [http://localhost:8080/cmsadapter/map(http://localhost:8080/cmsadapter/map)].
 
The functionality described in this feature is realized by a two-step process. This process includes sequential execution of different components of CMS Adapter. Considering the update of content repository based on external RDF data, the first step is to annotate the given raw RDF data with CMS vocabulary entities. Possible properties that can exist in CMS vocabulary annotated RDF is explained in the section *CMS Vocabulary* below. The external RDF submitted to the CMS Adapter should be in **"application/rdf+xml"** format. In the second step, annotated RDF is reflected to content repository by an [RDFMapper](http://svn.apache.org/repos/asf/incubator/stanbol/trunk/cmsadapter/servicesapi/src/main/java/org/apache/stanbol/cmsadapter/servicesapi/mapping/RDFMapper.java) instance. 

Considering the other direction of this feature i.e generating RDF from content repository, the first step done is to generated a CMS vocabulary annotated RDF from the content repository. This process is also done by an *RDFMapper*. In the second step, additional statements are added based on the *RDFBridge* implementation. 

The main two parts developed in the scope of this feature will be explained in the sections below.

## RDF Mapping

As roughly explained, *RDFMapper* has a bidirectional nature. It can update content repository based on CMS vocabulary annotated RDF and generate CMS vocabulary annotated RDF from a content repository. There are two implementations for JCR and CMIS specifications namely [JCRRDFMapper](http://svn.apache.org/repos/asf/incubator/stanbol/trunk/cmsadapter/jcr/src/main/java/org/apache/stanbol/cmsadapter/jcr/repository/JCRRDFMapper.java) and [CMISRDFMapper](http://svn.apache.org/repos/asf/incubator/stanbol/trunk/cmsadapter/cmis/src/main/java/org/apache/stanbol/cmsadapter/cmis/repository/CMISRDFMapper.java).

### JCR RDF Mapping

While updating a content repository, *JCRRDFMapper* processes the RDF and detects root items i.e resources that do not have *CMS\_OBJECT\_PARENT\_REF* property. The hierarchy belonging to each root item is reflected to the content repository. Specific to JCR, only primary node type and mixin types properties which are described in CMS vocabulary are considered. The location to create the node or look for the updated node is determined by the *CMS\_OBJECT\_PATH* property. After creating the node, all properties in the RDF are tried to be added to the node itself. 

While generating RDF content repository, path of the root node is necessary. The root object is processed recursively. During this process, each node is converted to RDF and related CMS vocabulary annotations e.g *JCR\_PRIMARY\_TYPE*, *CMS\_OBJECT\_PARENT\_REF*, etc are added.

### CMIS RDF Mapping 

*CMISRDFMapper* has the same logic with *JCRRDFMapper*. While updating content repository, in the first step root items in the RDF are detected and afterwards based on the *CMS\_OBJECT\_PARENT\_REF* objects are created in the repository. As CMIS specification does not allow addition of custom properties to *Documents* or *Folders*, a separate document holding the metadata as RDF of actual object is created within the folder in which the actual object exists. The name of the additional metadata document is formed by appending **_metadata** at the end of the name of the actual object.   

While generating RDF from content *CMISRDFMapper* also needs a root object path. While processing root object recursively, each object is transformed to RDF and this generated RDF is merged with the metadata stored in a separate document.

## RDF Bridging  

In the scope of this feature, we developed the [RDFBridge](http://svn.apache.org/repos/asf/incubator/stanbol/trunk/cmsadapter/servicesapi/src/main/java/org/apache/stanbol/cmsadapter/servicesapi/mapping/RDFBridge.java) interface. This interface has also a bidirectional feature. In one direction, it intends to annotate external RDF data with CMS vocabulary properties, on the other hand it adds additional assertions to CMS vocabulary annotated RDF based on the *RDFBridge* implementation. 

CMS Adapter component provides a default implementation ([DefaultRDFBridgeImpl](http://svn.apache.org/repos/asf/incubator/stanbol/trunk/cmsadapter/core/src/main/java/org/apache/stanbol/cmsadapter/core/mapping/DefaultRDFBridgeImpl.java)) for *RDFBridge* interface. The following parameters can be configured for the default implementation.

*   Resource Selector: While annotating an external RDF, this configuration provides selection of resources from an RDF data. For example if this configuration is set with **rdf:type > skos:Concept**, resource having *skos:Concept* as their *rdf:type* property will be selected from the RDF data. On the other hand, while adding assertions to CMS vocabulary annotated RDF, for each resource having *CMS_OBJECT* URI as its *rdf:type*, a statement having predicate **rdf:type** and value **skos:Concept** will be added. 

*   Resource Name Predicate: While annotating an external RDF, this configuration indicates the predicate which points to the name of content repository item. A single URI such as **rdfs:label** or **http://www.w3.org/2000/01/rdf-schema#label** should be set for its value. If an empty configuration is passed name of the content repository items will be set as the local name of the URI representing the content repository object. While adding assertions to CMS vocabulary annotated RDF a assertion having the specified predicate will be added to RDF thanks to this configuration.

*   Children: This configuration specifies the children properties of content items. Value of this configuration should be like **skos:narrower > narrowerObject** or **skos:narrower > rdfs:label**. First option directly specifies the  name of the child content repository item. In the second case, value rdfs:label predicate of resource representing child item will be set as the name of child item. This option would be useful to create hierarchies. It is also possible to set only predicate indicating the subsumption relations such as only **skos:narrower**. In this case name of the child resource will be obtained from the local name of URI representing this CMS object.

*   Default Child Predicate: This configuration is used only when generating an RDF from the repository. If there are more than one child selector in *Children* configuration, it is not possible to detect the predicate that will be used as the child assertion while adding assertions to CMS vocabulary annotated RDF. In that case, this configuration is used to set child assertion between parent and child objects. This configuration is optional. But if there is a case in which this configuration should be used and if it is not set, this causes missing assertions in the generated RDF.

*   Content Repository Path: This configuration specifies the root path in content repository in which the new CMS objects will be created or existing ones will be updated. 
 
 
## CMS Vocabulary

Current CMS vocabulary contains the following URI references in the [CMSAdapterVocabulary](http://svn.apache.org/repos/asf/incubator/stanbol/trunk/cmsadapter/servicesapi/src/main/java/org/apache/stanbol/cmsadapter/servicesapi/helper/CMSAdapterVocabulary.java) class. 

### General Properties

*   CMS_OBJECT: In a CMS vocabulary annotated RDF, if a resource has this URI reference as value of its *rdf:type* property, the subject of that resource represents a content repository item e.g a *node* in JCR compliant content repositories or an *object* in CMIS compliant content repositories.
*   CMS\_OBJECT\_NAME: This URI reference represents the name of the content repository item.
*   CMS\_OBJECT\_PATH: This URI reference represents the absolute path of the content repository item.
*   CMS\_OBJECT\_PARENT\_REF: This URI reference represents the item to be created as parent of the item having this property.  
*   CMS\_OBJECT\_HAS\_URI: This URI reference represents the URI which is associated with the content repository item.  

### JCR Specific Properties

*   JCR\_PRIMARY\_TYPE: This URI reference represents primary node of the content repository item associated with the resource within the RDF.
*   JCR\_MIXIN\_TYPES: This URI reference represents the mixin type of the content repository item associated with the resource within the RDF.

### CMIS Specific Properties

*   CMIS\_BASE\_TYPE\_ID: This URI reference represents the base type of the content repository item associated with the resource within the RDF.

## Examples 

This [article](http://blog.iks-project.eu/adding-knowledge-to-jcrcmis-content-repositories/) from October 2011 describes the process of adding knowledge to a CMIS repository.

## Building and Launching the CMS Adapter

Since CMS Adapter is included in the *Full Launcher* of Apache Stanbol it is built with Apache Stanbol by default and can be launched under Apache Stanbol Full Launcher. For detailed instructions to build and launch Apache Stanbol see this [README](http://svn.apache.org/repos/asf/incubator/stanbol/trunk/README.md) file.