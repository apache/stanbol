The "solr" directory includes the default configuration of an Solr Server used
with the SolrYard configuration.

To use this configuration override the default configuration that comes with
the solr server by this one.

This default configuration uses a multi-core configuration with three predefined
cores. Note however that the SolrYard does not require a multi core
configuration.

For some notes about the predefined cores see the notes within the solr.xml

To add additional Cores one need to add a core within the solr.xml and create
the directory with the configuration.

Precomputed Indices (e.g. for dbpedia) will be distributed in archive containing
the configuration and the data of the Solr Index. This can be used as
configuration for a Solr Server or can be configured as an additional core for
an existing Solr Server using a multi-core configuration.

Even when using an EmbeddedSolrServer the file structure need to be the same.


Configuration of the Solr index (or a Core)

For detailed information please see the Solr documentation.
For the customisation of the Solr Server (Core) configuration the schema.xml
and the solrconf.xml are central.

Solr Schema (schema.xml)

see comments within the shema.xml for more information on how to
change/optimize the Solr schema used by the SolrYard.


SolrServer (core) configuration (solrconf.xml)

The solrconfig.xml provided with this default configuration is the same
as used by the default installation of the SolrServer.
See the documentation of Solr for possibility to improve search performance.
See http://wiki.apache.org/solr/SolrConfigXml for more infos
