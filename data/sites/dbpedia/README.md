# DBpedia.org with local index for the Apache Stanbol Entityhub

This module is no longer available. It was replaced by the Indexing Tool for
DBpedia.org that now automatically creates all required resources after the 
indexing completes.


## Building the DBpedia.org index

To build a local Index for DBPedia the Apache Entityhub provides an own utility
The module is located at

    {stanbol}/entityhub/indexing/dbpedia

A detailed documentation on how to use this utility is provided by the
README file.

### Note 

The indexing tool for DBPedia.org now creates also a bundle that can be used as
replacement for this one. However it will not contain the configuration for the
NamedEntityTaggingEngine.
This service needs to be manually configured by using the following values:

    org.apache.stanbol.enhancer.engines.entitytagging.nameField="rdfs:label"
    org.apache.stanbol.enhancer.engines.entitytagging.personType="dbp-ont:Person"
    org.apache.stanbol.enhancer.engines.entitytagging.personState=B"true"
    org.apache.stanbol.enhancer.engines.entitytagging.referencedSiteId="dbpedia"
    org.apache.stanbol.enhancer.engines.entitytagging.placeState=B"true"
    org.apache.stanbol.enhancer.engines.entitytagging.organisationState=B"true"
    org.apache.stanbol.enhancer.engines.entitytagging.organisationType="dbp-ont:Organisation"
    org.apache.stanbol.enhancer.engines.entitytagging.placeType="dbp-ont:Place"
    