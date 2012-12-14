
The use of this component requires a API key from OpenCalais. Without providing an API key, the engine will not work.
Such a key can be obtained from

	http://www.opencalais.com/APIkey

In the OSGi configuration console the key can be set as value of the property

	org.apache.stanbol.enhancer.engines.opencalais.license



Also, the tests require the API key. Without the key some tests will be skipped. For Maven the key can be set as a system property on the command line:

	mvn -Dorg.apache.stanbol.enhancer.engines.opencalais.license=YOUR_API_KEY [install|test]



Configuration properties that influence the enhancements delivered from the engine at runtime are:

- org.apache.stanbol.enhancer.engines.opencalais.typeMap
	The value is the name of a file for mapping the NER types from OpenCalais to other types. By default, a mapping to the DBPedia types is provided. If no mapping is desired one might pass an empty mapping file.
	
- org.apache.stanbol.enhancer.engines.opencalais.NERonly
	A boolean property to specify whether in addition to the NER enhancements also the OpenCalais Linked Data references are included as entity references. By default, these are omitted.

