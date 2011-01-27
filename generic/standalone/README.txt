** WHAT **

Minimal FISE server implementation that should run in any OSGi container
and require a minimum set of bundles.


** HOW **

To run with the Felix Karaf launcher:

1) Build and install FISE from source:

  $ svn co  http://iks-project.googlecode.com/svn/sandbox/fise/trunk fise
  $ cd fise
  $ mvn clean install -Dmaven.test.skip=true

2) Download apache-felix-karaf-1.4.0 from http://felix.apache.org/site/downloads.cgi

3) Unpack karaf, cd apache-felix-karaf-1.4.0

4) Copy src/main/resources/karaf/fise-standalone-karaf-descriptor.xml to the karaf "deploy" folder.

5) Start karaf using bin/karaf or bin/karaf.bat
   (More info at http://felix.apache.org/site/2-quick-start.html)

6) In the karaf shell, do:

  karaf@root> features:install fise-standalone

7) The curl test described below should now work

8) The Felix webconsole can be installed using:

  karaf@root> features:install webconsole

And access it at http://localhost:8181/system/console, credentials karaf/karaf


** TESTING WITH CURL **

Here's an example where we add a ContentItem, retrieve it and notice
that our dummy enhancement engine has added some metadata:

# HTTP PUT to add the ContentItem

  $ curl -T someImage.jpg http://localhost:8181/fise/testId
  /testId

# HTTP GET to retrieve it, enhanced
# (if enhancements are async, metadata might come later)

  $ curl http://localhost:8181/fise/testId

ContentItemImpl id=[/testId], mimeType[=null], data=[108781] bytes, metadata=[org.apache.stanbol.enhancer.standalone.impl.DummyEnhancementEngine#<enhanced>#/testId, Romeo#<loves>#Julieta]

Enhancement engines might be restricted to process only some media types, hence
it might be important for the client to mention the media type of the payload in
the first query.

  $ curl -H "Content-Type: text/plain" -T furtwangen.txt http://localhost:8181/fise/furtwangen.txt


