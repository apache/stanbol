
The use of this component requires a API key from OpenCalais. Without providing an API key, the engine will not work.
Such a key can be obtained from

	http://www.opencalais.com/APIkey

In the OSGi configuration console the key can be set as value of the property

	eu.iksproject.fise.engines.opencalais.license



Also, the tests require the API key. Without the key some tests will be skipped. For Maven the key can be set as a system property on the command line:

	mvn -Deu.iksproject.fise.engines.opencalais.license=YOUR_API_KEY [install|test]
