LDPath implementation for the Entityhub
=======================================

This artifact adds LDPath support to the Entityhub. It contains RDFBackend
implementations for all Repository APIs available for the Entityhub.

* YardBackend: The Yard is the lower level Storage API of the Entityhub that
  does not require OSGI to run.
* SiteBackend: This is the RDFBackend implementation for ReferencedSite. This
  implementation is intended to be used for using LdPath with a single entity
  source registered with the entityhub.
* SiteManagerBackend: This RDFBackend implementation can be used to execute
  LDPath expressions on all ReferencedSites registered with the Entityhub
* EntityhubBackend: This can be used to use LDPath on locally managed Entities.
* QueryResultBackend: This Backend wraps an other RDFBackend and a query result
  and allows to perform LDPath on query results without requiring to lookup 
  Representations already selected by the query.


