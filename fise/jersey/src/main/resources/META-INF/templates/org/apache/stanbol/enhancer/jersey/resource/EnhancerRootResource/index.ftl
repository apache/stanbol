<#import "/imports/common.ftl" as common>
<#import "/imports/sparql.ftl" as sparql>
<#escape x as x?html>
<@common.page title="Welcome to Apache Stanbol!" hasrestapi=false> 

<p>Apache Stanbol is an Open Source HTTP service meant to help Content
Management System developers to semi-automatically enhance unstructured
content (text, image, ...) with semantic annotations to be able to link
documents with related entities and topics.</p>

<p>Please go to <a href="http://incubator.apache.org/stanbol">the
official website</a> to learn more on the project, read the
documentation and join the mailing list.</p>

<p>Here are the main HTTP entry points. Each resource comes with a web
view that documents the matching RESTful API for applications:</p>
<dl>

  <dt><a href="engines">/engines</a><dt>
  <dd>This is a <strong>stateless interface</strong> to allow clients to submit content to <strong>analyze</strong>
   by the <code>EnhancementEngine</code>s and get the resulting <strong>RDF enhancements</strong> at once
   without storing anything on the server-side.</dd>

  <dt><a href="store">/store</a><dt>
  <dd>This is a <strong>stateful interface</strong> to submit content to <strong>analyze and store
   the results</strong> on the server. It is then possible to browse the resulting enhanced
   content items.</dd>

  <dt><a href="sparql">/sparql</a><dt>
  <dd>This is the <strong>SPARQL endpoint</strong> for the Stanbol store.
     <a href="http://en.wikipedia.org/wiki/Sparql">SPARQL</a> is the
     standard query language the most commonly used to provide interactive
     access to semantic knowledge bases.</dd>

  <dt><a href="system/console">/system/console</a><dt>
  <dd>
    <p>This is the OSGi administration console (for administrators and developers). The initial
       username / password is set to <em>admin / admin</em>.</p>
    <p>Use the console to add new bundles and activate, de-activate and configure components.</p>
    <p>The console can also be used to perform hot-(re)deployment of any OSGi bundles.
       For instance to re-deploy a new version of this web interface, go to the <tt>$STANBOL_HOME/enhancer/jersey</tt>
       source folder and run the following command:</p>
<pre>
mvn install -o -DskipTests -PinstallBundle -Dsling.url=${it.publicBaseUri}system/console
</pre>
  </dd>

</dl>
</@common.page>
</#escape>
