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
        <h3 id="reconcileDocTitle" class="docuTitle">
            Installation/Usage:</h3>
        <script>
            $("#reconcileDocTitle").click(function () {
              $("#reconcileDocTitle").parent().toggleClass("collapsed");
            }); 
        </script>
        <div class="docuCollapsable">

<h3>Google Refine Installation</h3>
Users that want to use this service need first to 
<a href="http://code.google.com/p/google-refine/wiki/InstallationInstructions">
install Google Refine</a>. Typically Stanbol users will also be interested in
installing the 
<a href="http://lab.linkeddata.deri.ie/2010/grefine-rdf-extension/"> RDF
Extension</a> as this will allow you to map you data to RDF schemata and export
them as RDF (e.g. to import them afterwards to the Stanbol Entityhub.)
</p><p>
<h3>Reconciliation Service Installation</h3>
After installing Google Refine you will need to create a first project. For the
sake of testing Reconciliation with the Stanbol Entityhub you can use the
'<a href="http://svn.apache.org/repos/asf/lucene/dev/tags/lucene_solr_3_6_0/solr/example/exampledocs/books.csv">
book.scv</a>' file included in the Apache Solr distribution.
</p><p>
If you created a new Google Refine Project (e.g. by using the 'books.csv' example)
you will see the imported data in tabular form. The following Screenshot
visualises how to open the Reconciliation dialog for Book Authors.</p>
<img src="${it.staticRootUrl}/entityhub/images/google_refine_reconciliation-open_dialog.png"/>
<p> Via the Reconciliation dialog you can now "install" the Entityhub, Referenced
Sites or the '/sites' endpoint as <b>Standard Reconciliation Service</b> by
by pressing the [Add Standard Service ...] Button add copying the URL of 
this page to the dialog. The following Screenshot shows how the install the
Referenced Site for DBpedia.org.</p>
<img src="${it.staticRootUrl}/entityhub/images/google_refine_reconciliation-add_service.png"/>
<h3>Reconciliation Service Usage</h3>
<p>After this step a new Reconciliation Service will show up in the left link.
In addition the newly installed site will be selected and used to provide 
suggestions for the initially selected column of you Google Refine project
(Book Authors if you used the 'book.csv' sample data and selected the 'author_t'
column). 
</p><p>
The final Screenshot shows the installed Reconciliation service based on the
<b>Stanbol Entityhub: dbpedia Referenced Site</b> that is ready to be used
to reconcile Entities.</p>
<img src="${it.staticRootUrl}/entityhub/images/google_refine_reconciliation-use_service.png"/>

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
