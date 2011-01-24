
Solr Confoguration for RICK SolrYard:

This folder contains default configuration of the SolrServer as used
for the IKS RICK SolrYard.

Cores:

Typically a multi core configuration is used, because than multiple
Solr Yards can write data in different lucene indexes. However it is
also possible to share a single index with multiple Yards if the
"Multiple Yard Layout" is activated for all Yards using the same index.

The recommendation is to:
 - use an own index for the RickYard. Because this makes it easy to
   backup the data of the Rick or to reconfigure the Rick to use a
   different data source
 - use own index for full caches (caches that store all entities of
   a referenced site). THis is because this makes it easy to create/
   download a new version of the index and replace the old one.
   It makes it also easy to replicate the cache and move it to an
   other infrastructure
 - you can safely use the same index for multiple yards if they are
   used for caches that store used items of referenced sites or for
   referenced sites with a limited amount of informations (e.g. a 
   company thesaurus É)

Configuration:

All cores of this default configuration use the same configuration.
This means that all files in the conf directory are the same for
all cores.

Solr Schema (schema.xml)

The schema.xml included in the configuration is specific to the implementation
of the SolrYard (especially the SolrPathFieldMapper and the constants defined
by SolrConst). 
Things that should not be changed include
 - the class of fieldTypes
 - changes or removal of defined fields
 - removal and changed to existing copyFields
All such actions will most likely cause some assumptions made by the SolrYard
implementation invalid and result in unexpected behavior and/or errors.

Configurations that can be changed include
 - adding fieldTypes for different languages
 - adding of additional fields for known fields in the indexed data
 - adding dynamicField for additional languages
 - changing store/indexed parameters for all kind of fields
 - adding copyField commands

Note that the default configuration assumes that the SolrYard needs also to
store the entity information. If the Yard is only used for search and not
for retrieval, than changing most of the fields to store=false will increase
performance and reduce index size greatly.

See http://wiki.apache.org/solr/SchemaXml for more infos

SolrServer (core) configuration (solrconf.xml)

The solrconfig.xml provided with this default configuration is the same
as used by the default installation of the SolrServer.
See the documentation of Solr for possibility to improve search performance. 
See http://wiki.apache.org/solr/SolrConfigXml for more infos
