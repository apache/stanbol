<#--
  Licensed to the Apache Software Foundation (ASF) under one or more
  contributor license agreements.  See the NOTICE file distributed with
  this work for additional information regarding copyright ownership.
  The ASF licenses this file to You under the Apache License, Version 2.0
  (the "License"); you may not use this file except in compliance with
  the License.  You may obtain a copy of the License at

      http://www.apache.org/licenses/LICENSE-2.0

  Unless required by applicable law or agreed to in writing, software
  distributed under the License is distributed on an "AS IS" BASIS,
  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  See the License for the specific language governing permissions and
  limitations under the License.
-->
<#import "/imports/common.ftl" as common>
<#escape x as x?html>
<@common.page title="Stanbol Entityhub: Google Refine Reconciliation Support" hasrestapi=true> 

<div class="panel" id="webview">
<p>This adds support for using the Stanbol Entityhub together with
<a href=""http://code.google.com/p/google-refine/">Google Refine</a> for reconciliation of Entities.</p>
<p>Google Refine is a tool used to clean up messy data originating e.g. from 
lists managed with spread sheet tools, data base dumps ... The 
<a href="http://code.google.com/p/google-refine/wiki/Reconciliation"> reconciliation
steps</a> allows than to link literal values of those data with Entities defined in
some knowledge base - in this case Entities available via the Apache Stanbol
Entityhub.</p>

<#-- START Collapseable -->
<div class="docu"> 
    <div class="collapsed">
        <h3 id="grefeineinstall" class="docuTitle">
            Google Refine Installation:</h3>
        <script>
            $("#grefeineinstall").click(function () {
              $("#grefeineinstall").parent().toggleClass("collapsed");
            }); 
        </script>
        <div class="docuCollapsable">

Users that want to use this service need first to 
<a href="http://code.google.com/p/google-refine/wiki/InstallationInstructions">
install Google Refine</a>. Typically Stanbol users will also be interested in
installing the 
<a href="http://lab.linkeddata.deri.ie/2010/grefine-rdf-extension/"> RDF
Extension</a> as this will allow you to map you data to RDF schemata and export
them as RDF (e.g. to import them afterwards to the Stanbol Entityhub.)
</p>
<#-- END Collapseable -->
        </div>
    </div>
</div>  

<#-- START Collapseable -->
<div class="docu"> 
    <div class="collapsed">
        <h3 id="reconcileservieconfig" class="docuTitle">
            Configuring the Reconciliation Service</h3>
        <script>
            $("#reconcileservieconfig").click(function () {
              $("#reconcileservieconfig").parent().toggleClass("collapsed");
            }); 
        </script>
        <div class="docuCollapsable">

<p>
To configure a reconciliation service you need first to create a new (or open
an existing) Google Refine Project. If you do not yet have an project 
you can use the 
'<a href="http://svn.apache.org/repos/asf/lucene/dev/tags/lucene_solr_3_6_0/solr/example/exampledocs/books.csv">
book.scv</a>' file included in the Apache Solr distribution to create a new one.
</p><p>
If you created a new Google Refine Project (e.g. by using the 'books.csv' example)
you will see the imported data in tabular form. The following Screenshot
visualises how to open the Reconciliation dialog for Book Authors.</p>
<img src="${it.staticRootUrl}/entityhub/images/google_refine_reconciliation-open_dialog.png"/>
<p> Via the Reconciliation dialog you can now "install" the Entityhub, Referenced
Sites or the '/sites' endpoint as <b>Standard Reconciliation Service</b> by
by pressing the [Add Standard Service ...] Button add copying the URL of 
this page to the dialog. 
</p><p>
Service URL:
<code><pre>
    ${it.requestUri}
</pre></code>
</p><p>
The following Screenshot shows how the install the
Referenced Site for DBpedia.org.</p>
<img src="${it.staticRootUrl}/entityhub/images/google_refine_reconciliation-add_service.png"/>
<h4>Testing the Service</h4>
<p>After this step a new Reconciliation Service will show up in the left link.
In addition the newly installed site will be selected and used to provide 
suggestions for the initially selected column of you Google Refine project
(Book Authors if you used the 'book.csv' sample data and selected the 'author_t'
column). 
</p><p>
The next Screenshot shows the installed Reconciliation service based on the
<b>Stanbol Entityhub: dbpedia Referenced Site</b> that is ready to be used
to reconcile Entities.</p>
<img src="${it.staticRootUrl}/entityhub/images/google_refine_reconciliation-use_service.png"/>

<#-- END Collapseable -->
        </div>
    </div>
</div>  

<#-- START Collapseable -->
<div class="docu"> 
    <div class="collapsed">
        <h3 id="reconcileserviceusage" class="docuTitle">
            Usage of the Reconciliation Service</h3>
        <script>
            $("#reconcileserviceusage").click(function () {
              $("#reconcileserviceusage").parent().toggleClass("collapsed");
            }); 
        </script>
        <div class="docuCollapsable">
<p>
This provides first an overview about the usage of the Google Reconciliation service
dialog and second the documentation of special features provided by this
implementation.
</p>
<h4>Reconciliation Dialog</h4>
<img src="${it.staticRootUrl}/entityhub/images/google_refine_reconciliation-use_service.png"/>
<p>Reconciliation Dialog Fields</p><ul>
<li><b>Reconclie Services:</b> On the left site the list of available Services is shown.
As soon as you select one Google Refine will send a query of the first ten Entries of
your current project to that service to obtain some meta data.</li>
<li><b>Suggested Types:</b>In the middle a list of suggested types is presented.
This list will be empty if the service does not return any results for the request
of the first ten entries. You can also manually add the type in the Field below
the list. It is also possible to reconcile without constraining the type by
selecting the last option.</li>
<li><b>Using additional Properties:</b> On the right side the list of all 
columns of your project is shown. Information of those columns can be used to
for reconciliation. To use values of other columns the name of the property
must be specified on the text field next to the column name. The Stanbol
Entityhub also supports some special option like the semantic context-, full
text- and similarity-search (see below for details). <br>
Note that it is possible to use the same property (and special fields) for
mapping several columns. In this case values of all those columns are merged.</li>
</ul>
<p>
The Entityhub does support qnames (e.g. rdfs:label) for prefixes registered in
the <a href="http://svn.apache.org/repos/asf/incubator/stanbol/trunk/entityhub/generic/servicesapi/src/main/java/org/apache/stanbol/entityhub/servicesapi/defaults/NamespaceEnum.java">
NamespaceEnum</a>.</p>
<h4>Special Property support</h4>
<p>
The Reconciliation Dialog allows to use values of other columns to improve
reconciliation support. To further improve this ability the Stanbol Entityhub
supports the following special fields:
</p><ul>
<li><b>Full Text</b> '<code>@fullText</code>': This allows to use textual values
of other fields to be matched against any textual value that is linked with
suggested Entities (e.g. the values of rdfs:comment, skos:note, dbp-ont:abstract,
...).</li>
<li><b>Semantic Context</b> '<code>@references</code>': This allows to match
the URI values of other columns (that are already reconciled) with suggested
Entities. This is very useful to link further columns of an project if you have
already reconciled (and possibly manually corrected/improved) an other column
of the project. Note that this requires the dataset to define those links</li>
<li><b>Similarity Search</b> '<code>@similarity</code>': This will use textual
values to rank returned values based on their similarity (using 
<a href="http://wiki.apache.org/solr/MoreLikeThis">Solr MoreLikeThis</a>).<br>
By default this also uses the full text field however users can change this
by explicitly parsing a {property} URI (or qname)
'<code>@similarity:{property}</code>' as parameter. Note that parsed fields
need to be correctly configured to support Solr MLT queries. The documentation
of the Apache Entityhub Indexing Tool provides more information on that.</li>
</ul>
<p>
The following example shows how to use the '<code>@similarity</code>' for 
disambiguating music artists based on the name of the track and the album. To
make this work the <a href="">Musicbrainz</a> was imported in the Entityhub in
a way that the labels of Albums and Tracks where indexed with the Artists.
</p>
<img src="${it.staticRootUrl}/entityhub/images/google_refine_reconciliation-similarityexample.png"/>

<#-- END Collapseable -->
        </div>
    </div>
</div>  


</div>

<div class="panel" id="restapi" style="display: none;">
  <h3>Google Refine Reconciliation API</h3>

  <p>This is the Stanbol Entityhub based implementation of the 
  <a href="http://code.google.com/p/google-refine/wiki/ReconciliationServiceApi">
  Google Refine Reconciliation API</a>.</p>
</div>

</@common.page>
</#escape>
