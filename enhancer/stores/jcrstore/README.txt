JCR Storage Engine
==================

This bundle implements a storage engine for FISE that stores triples (i.e. content enhancements in
a JCR store). It implements the org.apache.stanbol.enhancer.servicesapi.Store interface, not the Clerezza
one, so for now SPARQL queries are not supported.

How to deploy
=============

You need to have the following bundles deployed in your OSGi runtime.

In the future they will be put in the pom.xml, but ATM this is not working, yet:

These bundles are also available at http://dl.dropbox.com/u/2022914/fise_jcrstore_bundles.zip
(for your convenience)

* Apache Derby 10.5 derby       10.5.3000000.802917      
* Apache Tika OSGi bundle org.apache.tika.bundle     0.7       
* Jackrabbit SPI org.apache.jackrabbit.jackrabbit-spi     2.0.0     jcr,jackrabbit         
* Jackrabbit SPI Commons org.apache.jackrabbit.jackrabbit-spi-commons     2.0.0     jcr,jackrabbit         
* Jackrabbit JCR-RMI org.apache.jackrabbit.jackrabbit-jcr-rmi     2.0.0     jcr,jackrabbit
* Jackrabbit JCR Commons org.apache.jackrabbit.jackrabbit-jcr-commons     2.0.0     jcr,jackrabbit         
* Apache Jackrabbit API org.apache.jackrabbit.jackrabbit-api     2.0.0     jcr,jackrabbit
* Apache Sling API org.apache.sling.api     2.0.9.SNAPSHOT     sling
* Content Repository for JavaTM Technology API javax.jcr     2.0     jcr         
* Apache Sling JCR Base Bundle org.apache.sling.jcr.base     2.0.7.SNAPSHOT     sling,jcr,jackrabbit         
* Apache Sling Repository API Bundle org.apache.sling.jcr.api     2.0.7.SNAPSHOT     sling,jcr,jackrabbit         
* Apache Sling Jackrabbit Embedded Repository org.apache.sling.jcr.jackrabbit.server     2.0.7.SNAPSHOT     sling,jcr,jackrabbit    

Then, deploy with
mvn clean install

Also, make sure to disable the default InMemoryStore in the OSGi console at http://localhost:8080/system/console/bundles
