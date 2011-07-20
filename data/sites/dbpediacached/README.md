# DBpedia.org with local cache

This build a bundle will install (configure) all required OSGI components to 
use the SPARQL Endpoint of DBPedia.org for query and retrieval. In addition a
local cache is configured that stores retrieved entities. 

This module is not part of the default build process.

## NOTE

One needs to uninstall/delete any other DBPedia.org configurations before using
this bundle. This is especially true for the default configuration for dbpedia
included in the Stanbol launchers.

Simple search for installed bundles starting with

    org.apache.stanbol.data.sites.dbpedia.*
    
and stop/remove them before activating this one.


