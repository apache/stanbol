IKS Autotagging
===============

:author: ogrisel@nuxeo.com

Text document classification / topic assignment service based on the text
content of DBpedia. The implementation is based on lucene and the
`MoreLikeThis` similarity query that leverages term frequencies of the index.


Building
========

1- Download maven_ and ensure that the `mvn` command is registered in your
`PATH` environment variable.

2- From the top of the `iks-autoagging/` folder build using maven as usual (this
   will install the `iks-autotagging-X.X.X-SNAPSHOT.jar` jar in your local maven
   repository and make it available to dependent projects such as Stanbol Enhancer)::

  % mvn install

3- From the same folder, build a standalone jar suitable for commandline
   usage. The resulting jar will be named:
   `target/iks-autotagging-X.X.X-SNAPSHOT-jar-with-dependencies.jar`::

  % mvn assembly:assembly

4- (Optional) To import the project in eclipse, first run::

  % mvn eclipse:eclipse

This will generate `.project` and `.classpath` files to that you can
"Import > Import existing projects into workspace" from the Eclipse
UI. Alternatively you can install the `m2eclipse` plugin to directly
import maven projects into eclipse.

.. _maven: http://maven.apache.org


Command line usage
==================

You can use the autotagger with a default embedded lucene index of
the top 10000 entities of DBpedia:

  % java -jar target/iks-autotagging-*-SNAPSHOT-jar-with-dependencies.jar \
    suggest -f samples/bob_marley.txt
  [...]
  Annotating 'samples/bob_marley.txt'... done in 739ms:
  Suggestion #1 (score: 4.648216): 'Bob Marley'
  URI:	http://dbpedia.org/resource/Bob_Marley
  type:	http://www.w3.org/2002/07/owl#Thing
  type:	http://dbpedia.org/ontology/Person
  type:	http://dbpedia.org/ontology/Artist
  type:	http://dbpedia.org/ontology/MusicalArtist
  Suggestion #2 (score: 0.127039): 'Bunny Wailer'
  URI:	http://dbpedia.org/resource/Bunny_Wailer
  type:	http://www.w3.org/2002/07/owl#Thing
  type:	http://dbpedia.org/ontology/Person
  type:	http://dbpedia.org/ontology/Artist
  type:	http://dbpedia.org/ontology/MusicalArtist
  Suggestion #3 (score: 0.121009): 'Desmond Dekker'
  URI:	http://dbpedia.org/resource/Desmond_Dekker
  type:	http://www.w3.org/2002/07/owl#Thing
  type:	http://dbpedia.org/ontology/Person
  type:	http://dbpedia.org/ontology/Artist
  type:	http://dbpedia.org/ontology/MusicalArtist

For better recall performance it is strongly recommended
to use a more comprehensive index of DBpedia entities.

To do so you first need to build or download a dedicated
DBpedia lucene index in a folder named `/path/to/lucene-idx` (for instance)
on the local filesystem (see later sections for instructions). You can download
a prebuilt index from here:

  http://dl.dropbox.com/u/5743203/IKS/autotagging/iks-dbpedia-lucene-idx-20100331-0.tar.bz2

(A better index is currently under construction...)

You can then add the "-i /path/to/lucene-idx" option to the
previous command line to use your custom index.

Instructions for building your own index from scratch are available in the following
sections.


Restful API
===========

:TODO: implement me first!

Launch a HTTP server to provide the service using a RESTful API thanks to jetty
and Jersey::

  % mvn jetty:run
  % curl -T file-to-anotate.txt http://localhost:8080/autotagging

RDF/JSON annotations could be serialized using this convents: http://jdil.org/.

Also the Stanbol Enhancer project features an OSGi embedding of this library 
combined with RESTful interface and persistent annotation and content stores:

  http://svn.apache.org/repos/asf/incubator/stanbol/trunk/enhancer/


Building a lucene index from DBpedia dumps
==========================================

1- Download and uncompress (using `bzip2 -d <filename>`)the following datasets from DBpedia:

  - instancetype_en.nt.bz2_

  - longabstract_en_nt.bz2_

  - article_label_en_nt.bz2_

.. _instancetype_en_nt.bz2: http://downloads.dbpedia.org/3.4/en/instancetype_en.nt.bz2
.. _longabstract_en_nt.bz2: http://downloads.dbpedia.org/3.4/en/longabstract_en.nt.bz2
.. _article_label_en_nt.bz2: http://downloads.dbpedia.org/3.4/en/article_label.nt.bz2


2- Build a temporary Jena TDB store::

  % java -Xmx2g -server -jar target/iks-autotagging-*-SNAPSHOT-jar-with-dependencies.jar \
    model /path/to/dbpedia-tdb /path/to/instancetype_en.nt /path/to/longabstract.nt /path/to/articles_label_en.nt

Alternatively you can download and use the `bin/tdbloader` tool from the TDB
distribution.

3- Index the Jena TDB into a Lucene `FSDirectory`::

  % java -Xmx2g -server -jar target/iks-autotagging-*-SNAPSHOT-jar-with-dependencies.jar \
    index /path/to/dbpedia-tdb /path/to/lucene-directory

You can then use luke_ to check the content of the resulting index::

  % java -jar /path/to/lukeall-1.0.0.jar -index /path/to/lucene-idx

.. _luke: http://www.getopt.org/luke/


Recently implemented
====================

0- Finish implementing the `JenaIndexer#main` method to be able to create a Jena
   TDB store out of DBpedia dumps from the command line.

1- Use the lucene `ShingleFilter` to generate bi-grams (or tri-grams) of token
   and improve the accuracy of the results at the expense of the size of the
   index.

2- Extend the `Autotagger` API to allow the requester to ask for a
   specific entity type (useful to combine with the output of a Named Entity
   detection module).

4- Extend the `TagInfo` class to feedback the caller with confidence levels
   for each suggestions (from lucene scores).

5- Improve the `JenaIndexer` to index other text literal sources such as labels,
   comments, ...

6- Build a small index of the most popular people / place / organization from
   DBpedia to be packaged easily as the default IKS model


Roadmap
=======

3- Implement standalone jersey-based JAX-RS components that takes the text
   content as an input and output suggested annotations as RDF/XML of RDF/JSON
   synchronously.

7- Index the DBpedia categories dumps and by aggregating literal text from
   directly related entities and propose suggestions with type topic to
   complement entities typed tags.

8- Use a complete wikimedia markup dump as a fulltext source for the index
   instead of just DBpedia

9- Index the textual context (enclosing paragraph) of all incoming links
   to an entity coming from other wikipedia articles.

