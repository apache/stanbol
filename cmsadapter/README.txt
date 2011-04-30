Stanbol CMS Adapter
==========================
This component of STANBOL is designed for extracting the already available semantics in the content 
models as an ontology and store it in knowledge base. The extraction process is realized by first lifting
already existing node type and property definitions in the content managament system and then defining 
bridges between content management system and knowledge base and processing them. There are 3 kinds of bridges 
which are listed below. 

•	Concept Bridge
•	Instance Bridge
•	Subsumption Bridge
•	Property Bridge (not directly created)


Concept Bridge: This bridge type is designed for creating a class hierarchy in the generated ontology from a node
hierarchy in the content management system. The paths of the nodes should be specified in the bridge 
definition. It is possible to define subsumption and property bridges within a concept bridge. A Subsumption
bridge in a concept bridge creates parent/child relationship between the node specified in the bridge and
another node that is referenced through a property which is also specified in the bridge. A property bridge
within a concept bridge creates "disjointWith", "equivalentClass" relationships between the nodes.

Example xml:
<?xml version="1.0" encoding="UTF-8"?>
<BridgeDefinitions xmlns="mapping.model.servicesapi.cmsadapter.stanbol.apache.org">
  <ConceptBridge>
    <Query>/TestDir/%</Query>
    <SubsumptionBridge>
      <PredicateName>subsumptionProperty</PredicateName>
    </SubsumptionBridge>
    <PropertyBridge>
      <PredicateName>equiClass</PredicateName>
      <PropertyAnnotation>
        <Annotation>equivalentClass</Annotation>
      </PropertyAnnotation>
    </PropertyBridge>
    <PropertyBridge>
      <PredicateName>childClass</PredicateName>
      <PropertyAnnotation>
        <Annotation>subsumption</Annotation>
      </PropertyAnnotation>
    </PropertyBridge>
    <PropertyBridge>
      <PredicateName>disjointClass</PredicateName>
      <PropertyAnnotation>
        <Annotation>disjointWith</Annotation>
      </PropertyAnnotation>
    </PropertyBridge>
  </ConceptBridge>
</BridgeDefinitions>
  
First of all, this concept bridge will create a class hierarchy from the nodes under the path "/TestDir". Thanks to "%" 
all nodes of subtree under TestDir node will be a separate class in the ontology. Also parent/child relations within them 
will also be set. 

This concept bridge includes a subsumption bridge inside. One or more parent/child relation will be created for each 
processed node having "subsumptionProperty" property. Target classes will be created for the values of "subsumptionProperty".

This concept bridge also includes three property bridges inside. Property bridges within a concept bridge are valid only if 
they have one of the "equivalentClass", "disjointWith" or "subsumption" annotations. For property bridge having predicateName 
"equiClass", an "equivalentClass" relationship will be formed between the node under "/TestDir" path and the node values of 
"equiClass" property. To get the expected result, "equiClass" property should have a referencable type e.g "REFERENCE", "PATH"...

 
Instance Bridge: This bridge type is designed for creating instances in the generated ontology from a node hierarchy in the 
content management system. The paths of the nodes should be specified in the bridge definition. It is possible to define 
property bridges within an instance bridge. A property bridge within an instance bridge creates assertions for the nodes that
are specified in the bridge definition.

Example xml:
<?xml version="1.0" encoding="UTF-8"?>
<BridgeDefinitions xmlns="mapping.model.servicesapi.cmsadapter.stanbol.apache.org">
  <InstanceBridge>
    <Query>/NewsArticles/%</Query>
    <PropertyBridge>
      <PredicateName>relatedItem</PredicateName>
      <PropertyAnnotation>
        <Annotation>symmetric</Annotation>
      </PropertyAnnotation>
    </PropertyBridge>
    <PropertyBridge>
      <PredicateName>categorizedBy</PredicateName>
      <PropertyAnnotation>
        <Annotation>instanceOf</Annotation>
      </PropertyAnnotation>
    </PropertyBridge>
    <PropertyBridge>
      <PredicateName>title</PredicateName>
    </PropertyBridge>
  </InstanceBridge>
</BridgeDefinitions>

First of all, this instance bridge will create individuals from the nodes under the path "/NewsArticles". Thanks to "%" 
all nodes of subtree under NewsArticles node will be a separate individual in the ontology. 

This instance bridge includes three property bridges inside. Valid annotations for property bridge within an instance bridge
are "functional", "inverseFunctional", "symmetric", "transitive" and "instanceOf". According to the type of the property that
is set within content management system, datatype or object property assertions will be set to created individual. If there is 
any specified annotation, it will added to already lifted property in the ontology. 

Thanks to property bridge having predicateName "categorizedBy", one or more type assertions will be added to the created
individual. A class will be created in the ontology for each value of categorizedBy property.


As subsumption and property bridges are explained indirectly, they are skipped. A last remark is that while it is possible to
create separate definitions of concept, instance and subsumption bridges, property bridges can only be attached to a concept
or instance bridge.
 


Building CMS Adapter
==========================

* Checkout Apache Stanbol
	$ svn co http://svn.apache.org/repos/asf/incubator/stanbol/trunk/
* Build Stanbol [see] (http://svn.apache.org/repos/asf/incubator/stanbol/trunk/README.md)
* Build Stanbol Ontology Manager - Store
	$ cd trunk/ontologymanager/store
	$ mvn install
* Build Stanbol Commons Web Ontology
	$ cd trunk/commons/web/ontology
	$ mvn install
* Go to CMS Adapter root (/stanbol/trunk/cmsadapter)
	$ mvn install


Run launcher:

* Run full launcher 
	$ cd  launchers/lite
	$ java -jar -Xmx1g -XX:MaxPermSize=128m target/org.apache.stanbol.cmsadapter.launchers.lite-0.9-SNAPSHOT.jar


Configuration
==========================
