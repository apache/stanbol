<#import "/imports/common.ftl" as common>
<#escape x as x?html>
<@common.page title="RESTful services">

<!-- start of file -->
<style type="text/css">

table {
    border-collapse:collapse;
    border: 1px solid;
}
table th,
table td{
    padding: 5px;
    font-size: 0.8em;
   }
pre{
    margin:10px 0px;
    overflow:auto;
}
</style>

<UL class="indent">
<LI><A NAME="tex2html559"
  HREF="#SECTION000114100000000000000">/ontonet/ontology</A>
<UL>
<LI><A NAME="tex2html560"
  HREF="#SECTION000114110000000000000">GET</A>
<LI><A NAME="tex2html561"
  HREF="#SECTION000114120000000000000">PUT</A>
<LI><A NAME="tex2html562"
  HREF="#SECTION000114130000000000000">POST</A>
<LI><A NAME="tex2html563"
  HREF="#SECTION000114140000000000000">DELETE</A>
</UL>
<BR>
<LI><A NAME="tex2html564"
  HREF="#SECTION000114200000000000000">/ontonet/session</A>
<UL>
<LI><A NAME="tex2html565"
  HREF="#SECTION000114210000000000000">GET</A>
<LI><A NAME="tex2html566"
  HREF="#SECTION000114220000000000000">POST</A>
<LI><A NAME="tex2html567"
  HREF="#SECTION000114230000000000000">PUT</A>
<LI><A NAME="tex2html568"
  HREF="#SECTION000114240000000000000">DELETE</A>
</UL>
<BR>
<LI><A NAME="tex2html569"
  HREF="#SECTION000114300000000000000">/rule</A>
<UL>
<LI><A NAME="tex2html570"
  HREF="#SECTION000114310000000000000">GET</A>
<LI><A NAME="tex2html571"
  HREF="#SECTION000114320000000000000">POST</A>
<LI><A NAME="tex2html572"
  HREF="#SECTION000114330000000000000">DELETE</A>
</UL>
<BR>
<LI><A NAME="tex2html573"
  HREF="#SECTION000114400000000000000">/recipe</A>
<UL>
<LI><A NAME="tex2html574"
  HREF="#SECTION000114410000000000000">GET</A>
<LI><A NAME="tex2html575"
  HREF="#SECTION000114420000000000000">POST</A>
<LI><A NAME="tex2html576"
  HREF="#SECTION000114430000000000000">DELETE</A>
</UL>
<BR>
<LI><A NAME="tex2html577"
  HREF="#SECTION000114500000000000000">/reasoner/classify</A>
<LI><A NAME="tex2html578"
  HREF="#SECTION000114600000000000000">/reasoner/check-consistency</A>
<UL>
<LI><A NAME="tex2html579"
  HREF="#SECTION000114610000000000000">GET</A>
<LI><A NAME="tex2html580"
  HREF="#SECTION000114620000000000000">POST</A>
</UL>
<BR>
<LI><A NAME="tex2html581"
  HREF="#SECTION000114700000000000000">/reasoner/enrich</A>
<LI><A NAME="tex2html582"
  HREF="#SECTION000114800000000000000">/refactor (/lazy and /consistent)</A>
<UL>
<LI><A NAME="tex2html583"
  HREF="#SECTION000114810000000000000">GET</A>
<LI><A NAME="tex2html584"
  HREF="#SECTION000114820000000000000">POST</A>
</UL>
<BR>
<LI><A NAME="tex2html585"
  HREF="#SECTION000114900000000000000">/reengineer/reengineer</A>
<UL>
<LI><A NAME="tex2html586"
  HREF="#SECTION000114910000000000000">Input is a document (Mime-type based)</A>
<LI><A NAME="tex2html587"
  HREF="#SECTION000114920000000000000">Input is a DBMS connection</A>
</UL></UL>
<!--End of Table of Child-Links-->
<HR>

<H3><A NAME="SECTION000114000000000000000"></A>
<A NAME="sec:restful"></A>
<BR>

<FONT COLOR="#000000">RESTful services</FONT>
</H3>
KReS functionalities can be exploited by client applications through a set of RESTful services. In this section we give an overview of the services, with few examples on how can be queried using the command line tool <TT>cURL</TT><A NAME="tex2html75"
  HREF="footnode.html#foot1829"><SUP>36</SUP></A> (the syntax and supported options can change depending on the tool version and operating system). All services returns data in RDF compliant formats.

<P>

<H3><A NAME="SECTION000114100000000000000">
/ontonet/ontology</A>
</H3>
This service provides access and management to ontology scopes in KReS. With this service, client applications can add, remove or modify scopes. Such modifications are globally shared, since they are performed internally in the <I>custom</I> space. Ontologies added at scope creation time can also be placed in the <I>core</I> space, and are then not removable. All changes in the network performed by this service are shared globally by any KReS component. In the future, this services will be probably restricted to a sub-set of users with controlled policies.

<P>
Supported HTTP methods are: <B>GET</B>, <B>PUT</B>, <B>POST</B>, <B>DELETE</B>

<P>

<H4><A NAME="SECTION000114110000000000000">
GET</A>
</H4> method provides access to scopes and ontologies. This method can be used to obtain the globally shared version of the scope (it represents the <I>custom</I> space):

<OL>
<LI>The active scopes
</LI>
<LI>All the scopes
</LI>
<LI>A particular ontology
</LI>
</OL>

<P>
Supported parameters for <B>GET</B> requests are: 
<BR><TABLE CELLPADDING=3 BORDER="1" WIDTH="100%">
<TR><TH ALIGN="LEFT"><B>Parameter</B></TH>
<TH ALIGN="LEFT"><B>Mandatory</B></TH>
<TH ALIGN="LEFT" VALIGN="TOP" WIDTH=283><B>Description</B></TH>
</TR>
<TR><TH ALIGN="LEFT"><I>with-inactive</I></TH>
<TD ALIGN="LEFT">N</TD>
<TD ALIGN="LEFT" VALIGN="TOP" WIDTH=283>If set to <B>true</B> will obtain information about all the scopes</TD>
</TR>
<TR><TH ALIGN="LEFT"><I>session</I></TH>
<TD ALIGN="LEFT">N</TD>
<TD ALIGN="LEFT" VALIGN="TOP" WIDTH=283>If set will return a specific session space over the scope</TD>
</TR>
</TABLE>

<P>
The following are three examples on how to obtain information about the scopes:
<PRE>
 $ curl -X GET -H "Accept: text/turtle" \
     http://localhost:8080/ontonet/ontology

 $ curl -X GET -H "Accept: text/turtle" \
     http://localhost:8080/ontonet/ontology?with-inactive=true
</PRE>

<P>
Get the root ontology for the scope 'User'.
<PRE>
 $ curl -X GET -H "Accept: text/turtle" \
     http://localhost:8080/ontonet/ontology/User
</PRE>

<P>
Next, the exact version of ontology FOAF that is loaded within scope 'User':

<P>
<PRE>
 $ curl -X GET -H "Accept: text/turtle" \
	http://localhost:8080/ontonet/ontology/User/http://xmlns.com/foaf/0.1/
</PRE>

<P>
Notice how FOAF must be addressed to by its logical URI, not the physical one (as could be, for instance, <TT><A NAME="tex2html77"
  HREF="http://xmlns.com/foaf/spec/index.rdf">http://xmlns.com/foaf/spec/index.rdf</A></TT>).

<P>
The last requests GETs an ontology within the network, regardless of the scope where it is loaded:
<PRE>
 $ curl -X GET -H "Accept: text/turtle" \
http://localhost:8080/ontonet/ontology/get
    ?iri=http://www.ontologydesignpatterns.org/cp/owl/agentrole.owl
</PRE>

<P>
In all cases the following responses can be obtained:
<DL>
<DT><STRONG>200</STRONG></DT>
<DD>Data is retrieved
 
</DD>
<DT><STRONG>404</STRONG></DT>
<DD>The scope/ontology does not exists in the network
 
</DD>
<DT><STRONG>500</STRONG></DT>
<DD>Some error occurred
</DD>
</DL>

<P>

<H4><A NAME="SECTION000114120000000000000">
PUT</A>
</H4> method is used to create a new ontology scope. Ontologies can be added as single or referring to an ontology registry.

<P>
Supported parameters for <B>PUT</B> request: 
<BR>
<P>
<TABLE CELLPADDING=3 BORDER="1" WIDTH="100%">
<TR><TH ALIGN="LEFT"><B>Parameter</B></TH>
<TH ALIGN="LEFT"><B>Mandatory</B></TH>
<TH ALIGN="LEFT" VALIGN="TOP" WIDTH=283><B>Description</B></TH>
</TR>
<TR><TH ALIGN="LEFT"><I>coreont</I></TH>
<TD ALIGN="LEFT">N</TD>
<TD ALIGN="LEFT" VALIGN="TOP" WIDTH=283>An ontology to be added in the core space of the newly created scope (immutable) 

<P>
<B>Cannot be used in conjunction with <I>corereg</I></B>.</TD>
</TR>
<TR><TH ALIGN="LEFT"><I>corereg</I></TH>
<TD ALIGN="LEFT">N</TD>
<TD ALIGN="LEFT" VALIGN="TOP" WIDTH=283>An ontology registry to be added in the core space of the scope (immutable)

<P>
<B>Cannot be used in conjunction with <I>coreont</I></B>.</TD>
</TR>
<TR><TH ALIGN="LEFT"><I>customont</I></TH>
<TD ALIGN="LEFT">N</TD>
<TD ALIGN="LEFT" VALIGN="TOP" WIDTH=283>An ontology to be added in the custom space of the newly created scope (mutable) 

<P>
<B>Cannot be used in conjunction with <I>customreg</I></B>.</TD>
</TR>
<TR><TH ALIGN="LEFT"><I>customreg</I></TH>
<TD ALIGN="LEFT">N</TD>
<TD ALIGN="LEFT" VALIGN="TOP" WIDTH=283>An ontology registry to be added in the custom space of the scope (mutable)

<P>
<B>Cannot be used in conjunction with <I>corereg</I></B>.</TD>
</TR>
<TR><TH ALIGN="LEFT"><I>activate</I></TH>
<TD ALIGN="LEFT">N</TD>
<TD ALIGN="LEFT" VALIGN="TOP" WIDTH=283>[true|false] Wheather activate the newly created. Default value is false.  <I>scope</I></TD>
</TR>
</TABLE>

<P>
The following creates the <I>User</I> scope and adds the FOAF ontology to the core space:

<P>
<PRE>
  $ curl -X PUT -G \
   -d coreont="http://xmlns.com/foaf/spec/index.rdf" \
     http://localhost:8080/ontonet/ontology/User
</PRE>

<P>
The next example creates the new ontology scope 'User' with the FOAF vocabulary as a core ontology and the ontologies from the 'KReStest' registry as custom ontologies:

<P>
<PRE>
 $ curl -X PUT  -G \
   -d coreont="http://xmlns.com/foaf/spec/index.rdf" \
   -d customreg="http://www.ontologydesignpatterns.org/registry/krestest.owl" \
     http://localhost:8080/ontonet/ontology/User
</PRE>

<P>

<H4><A NAME="SECTION000114130000000000000">
POST</A>
</H4>
Ontologies can be added to a scope through the POST request. Modifications have affect in the <I>custom space</I>, this means that the ontology can then be removed by a DELETE request.

<P>
Supported parameters for <B>POST</B> requests are: 
<BR><TABLE CELLPADDING=3 BORDER="1" WIDTH="100%">
<TR><TH ALIGN="LEFT"><B>Parameter</B></TH>
<TH ALIGN="LEFT"><B>Mandatory</B></TH>
<TH ALIGN="LEFT" VALIGN="TOP" WIDTH=283><B>Description</B></TH>
</TR>
<TR><TH ALIGN="LEFT"><I>location</I></TH>
<TD ALIGN="LEFT">Y</TD>
<TD ALIGN="LEFT" VALIGN="TOP" WIDTH=283>The physical IRI of the ontology to load in the scope.</TD>
</TR>
</TABLE>

<P>
Here is an example with the dummy scope User:

<P>
Load an ontology into the custom space of ontology scope 'User':
<PRE>
 $ curl -X POST \
	-F location="http://www.ontologydesignpatterns.org/cp/owl/agentrole.owl" \
	http://localhost:8080/ontonet/ontology/User
</PRE>

<P>
Possible responses can be:
<DL>
<DT><STRONG>200</STRONG></DT>
<DD>The ontology has been loaded
 
</DD>
<DT><STRONG>404</STRONG></DT>
<DD>The ontology to be loaded does not exists or the target scope is not available
 
</DD>
<DT><STRONG>500</STRONG></DT>
<DD>Some error occurred 
</DD>
</DL>

<P>

<H4><A NAME="SECTION000114140000000000000">
DELETE</A>
</H4> method ca be used to unload resources from the ontology network manager. To unload an ontology from a Scope the following request is sufficient:

<P>
<PRE>
 $ curl -X DELETE -G \
	http://localhost:8080/ontonet/ontology/User/http://xmlns.com/foaf/0.1/
</PRE>

<P>
To deregister (=&gt;deactivate and remove) a scope 'User':
<PRE>
 $ curl -X DELETE \
	http://localhost:8080/ontonet/ontology/User
</PRE>

<P>
Possible responses can be:
<DL>
<DT><STRONG>204</STRONG></DT>
<DD>The ontology/scope has been deleted
 
</DD>
<DT><STRONG>500</STRONG></DT>
<DD>Some error occurred 
</DD>
</DL>

<P>

<H3><A NAME="SECTION000114200000000000000">
/ontonet/session</A>
</H3>
Sessions can be created and used for accessing ontology scopes without making the changes to the model to be shared by other applications or components.

<P>
The session IRI can then be used as parameter in addition of the scope (to indicate the space to be read from) in other high level services (such classification or enrichment).

<P>
Supported HTTP methods: <B>GET</B>, to retrieve a session; <B>POST</B>, to create sessions; <B>PUT</B>, to add ontologies to the session;   <B>DELETE</B>, to remove ontologies from the session or to delete the session. 
<BR>
<P>

<H4><A NAME="SECTION000114210000000000000">
GET</A>
</H4> can be used to retrieve the session metadata. The session IRI must follow the base IRI of the service in this way:

<P>
<PRE>
 $ curl -X GET http://localhost:8080/ontonet/session/http://session-iri
</PRE>

<P>

<H4><A NAME="SECTION000114220000000000000">
POST</A>
</H4> request creates a new session.

<P>
Supported parameters are: 
<BR>
<P>
<TABLE CELLPADDING=3 BORDER="1" WIDTH="100%">
<TR><TH ALIGN="LEFT"><B>Parameter</B></TH>
<TH ALIGN="LEFT"><B>Mandatory</B></TH>
<TH ALIGN="LEFT" VALIGN="TOP" WIDTH=283><B>Description</B></TH>
</TR>
<TR><TH ALIGN="LEFT"><I>scope</I></TH>
<TD ALIGN="LEFT">Y</TD>
<TD ALIGN="LEFT" VALIGN="TOP" WIDTH=283>the scope IRI on which the session must be created.</TD>
</TR>
</TABLE>

<P>
The following call will create a new session over the 'User' scope:
<PRE>
 $ curl -X POST \
-F scope="http://localhost:8080/ontonet/ontology/User"
	http://localhost:8080/ontonet/session
</PRE>

<P>
The result will be an OWL description of the session. Client application must take note of the session IRI to refer to it in further REST calls. It is important that client applications close the session with a DELETE call after the whole task is performed.

<P>

<H4><A NAME="SECTION000114230000000000000">
PUT</A>
</H4> can add ontologies to the session. Ontologies can be added by pointing to a dereferencable IRI or by attaching the file stream to the request.

<P>
Supported parameters are: 
<BR>
<P>
<TABLE CELLPADDING=3 BORDER="1" WIDTH="100%">
<TR><TH ALIGN="LEFT"><B>Parameter</B></TH>
<TH ALIGN="LEFT"><B>Mandatory</B></TH>
<TH ALIGN="LEFT" VALIGN="TOP" WIDTH=283><B>Description</B></TH>
</TR>
<TR><TH ALIGN="LEFT"><I>scope</I></TH>
<TD ALIGN="LEFT">Y</TD>
<TD ALIGN="LEFT" VALIGN="TOP" WIDTH=283>the scope IRI on which the session is opened.</TD>
</TR>
<TR><TH ALIGN="LEFT"><I>session</I></TH>
<TD ALIGN="LEFT">Y</TD>
<TD ALIGN="LEFT" VALIGN="TOP" WIDTH=283>The IRI of the session</TD>
</TR>
<TR><TH ALIGN="LEFT"><I>location</I></TH>
<TD ALIGN="LEFT">N</TD>
<TD ALIGN="LEFT" VALIGN="TOP" WIDTH=283>The IRI of the ontology to import within the session</TD>
</TR>
</TABLE>

<P>
The following call will add the ontology 'Situation' to the session, loading from its public location:
<PRE>
 $ curl -X PUT -G \
-d location="http://ontologydesignpatterns.org/owl/cp/situation.owl" \
-d session="http://session-iri"
-d scope="http://localhost:8080/ontonet/ontology/User"
	http://localhost:8080/ontonet/session
</PRE>

<P>
In alternative, a file stream can be attached to the request:
<PRE>
 $ curl -X PUT -G \
-d session="http://session-iri" \
-d scope="http://localhost:8080/ontonet/ontology/User" \
-T /User/ontologies/myOntology.owl
	http://localhost:8080/ontonet/session
</PRE>

<P>
Possible responses:
<DL>
<DT><STRONG>200</STRONG></DT>
<DD>The ontology has been added.
 
</DD>
<DT><STRONG>404</STRONG></DT>
<DD>Some resource does not exists or cannot be loaded
 
</DD>
<DT><STRONG>500</STRONG></DT>
<DD>Some error occurred
</DD>
</DL>

<P>

<H4><A NAME="SECTION000114240000000000000">
DELETE</A>
</H4> can be used to remove ontologies from the session or to delete the session:

<P>
Supported parameters are: 
<BR>
<P>
<TABLE CELLPADDING=3 BORDER="1" WIDTH="100%">
<TR><TH ALIGN="LEFT"><B>Parameter</B></TH>
<TH ALIGN="LEFT"><B>Mandatory</B></TH>
<TH ALIGN="LEFT" VALIGN="TOP" WIDTH=283><B>Description</B></TH>
</TR>
<TR><TH ALIGN="LEFT"><I>scope</I></TH>
<TD ALIGN="LEFT">Y</TD>
<TD ALIGN="LEFT" VALIGN="TOP" WIDTH=283>the scope IRI on which the session is opened.</TD>
</TR>
<TR><TH ALIGN="LEFT"><I>session</I></TH>
<TD ALIGN="LEFT">Y</TD>
<TD ALIGN="LEFT" VALIGN="TOP" WIDTH=283>The IRI of the session</TD>
</TR>
<TR><TH ALIGN="LEFT"><I>delete</I></TH>
<TD ALIGN="LEFT">N</TD>
<TD ALIGN="LEFT" VALIGN="TOP" WIDTH=283>The IRI of the ontology to remove from the session. <B>If this parameter is not provided the session is closed by the system and cannot be retrieved.</B></TD>
</TR>
</TABLE>

<P>
This requests remove the ontology 'Situation' from the session:
<PRE>
 $ curl -X DELETE -G \
  -d scope="http://localhost:8080/ontonet/ontology/User" \
  -d session="http://session-iri" \
  -d delete="http://ontologydesignpatterns.org/owl/cp/situation.owl" \
http://localhost:8080/ontonet/session
</PRE>

<P>
This one, removes the whole session:
<PRE>
 $ curl -X DELETE -G \
  -d scope="http://localhost:8080/ontonet/ontology/User" \
  -d session="http://session-iri" \
http://localhost:8080/ontonet/session
</PRE>

<P>
Possible responses:
<DL>
<DT><STRONG>200</STRONG></DT>
<DD>The deletion has been executed.
 
</DD>
<DT><STRONG>500</STRONG></DT>
<DD>Some error occurred 
</DD>
</DL>

<P>

<H3><A NAME="SECTION000114300000000000000">
/rules/rule</A>
</H3>
This service is targeted at rule management.

<P>
Supported HTTP methods are: <B>GET</B>: to retrieve details about a rule; <B>POST</B>: to create a rule; <B>DELETE</B>: to delete a rule.

<P>

<H4><A NAME="SECTION000114310000000000000">
GET</A>
</H4> requests will obtain all the rules.
The call can be followed by the IRI of a rule to obtain the single rule details.

<P>
Get the list of all the available rules.
<PRE>
$ curl -X GET -H "Accept: application/rdf+xml" \ 
	http://localhost:8080/rule/all
</PRE>

<P>
Get the recipe definition with all embedded rules:
<PRE>
$ curl -X GET -H "Accept: application/rdf+xml" \
     http://localhost:8080/rule/http://my-rule-iri
</PRE>

<P>
Possible responses:
<DL>
<DT><STRONG>200</STRONG></DT>
<DD>The rule is retrieved
 
</DD>
<DT><STRONG>404</STRONG></DT>
<DD>The rule does not exists in KReS
 
</DD>
<DT><STRONG>500</STRONG></DT>
<DD>Some error occurred 
</DD>
</DL>

<P>

<H4><A NAME="SECTION000114320000000000000">
POST</A>
</H4> requests can add a rule to a recipe or simply add a rule. If the recipe is omitted then the rule is simple added to the ontology, instead if the rule is already inside the ontology just to give the rule IRI and the recipe IRI. When all parameters are used then the rule is added both recipe and ontology.

<P>
Supported parameters are: 
<BR>
<P>
<TABLE CELLPADDING=3 BORDER="1" WIDTH="100%">
<TR><TH ALIGN="LEFT"><B>Parameter</B></TH>
<TH ALIGN="LEFT"><B>Mandatory</B></TH>
<TH ALIGN="LEFT" VALIGN="TOP" WIDTH=283><B>Description</B></TH>
</TR>
<TR><TH ALIGN="LEFT"><I>rule</I></TH>
<TD ALIGN="LEFT">Y</TD>
<TD ALIGN="LEFT" VALIGN="TOP" WIDTH=283>the rule name (or full IRI) to create. If a simple string is provided, the default base IRI is applied.</TD>
</TR>
<TR><TH ALIGN="LEFT"><I>recipe</I></TH>
<TD ALIGN="LEFT">N</TD>
<TD ALIGN="LEFT" VALIGN="TOP" WIDTH=283>the recipe name (or full IRI) where to add the rule.</TD>
</TR>
<TR><TH ALIGN="LEFT"><I>description</I></TH>
<TD ALIGN="LEFT">Y</TD>
<TD ALIGN="LEFT" VALIGN="TOP" WIDTH=283>A description of the rule.</TD>
</TR>
<TR><TH ALIGN="LEFT"><I>kres-syntax</I></TH>
<TD ALIGN="LEFT">Y</TD>
<TD ALIGN="LEFT" VALIGN="TOP" WIDTH=283>The rule serialized as KReS rule syntax.</TD>
</TR>
</TABLE>

<P>
Add a rule:
<PRE>
$ curl -X POST \ 
 -F rule = http://my-rule-iri
 -F recipe = http://my-recipe-iri
 -F description
 -F kres-syntax
	http://localhost:8080/rule
</PRE>

<P>
Note: If the rule is already inside the ontology and all parameters are set the process will try to add the rule to the ontology but it gives an error because the IRI of the rule is already inside the ontology.

<P>
Possible responses:
<DL>
<DT><STRONG>200</STRONG></DT>
<DD>The rule has been added.
 
</DD>
<DT><STRONG>204</STRONG></DT>
<DD>The rule has not been added.
 
</DD>
<DT><STRONG>400</STRONG></DT>
<DD>The rule and recipe are not specified.
 
</DD>
<DT><STRONG>404</STRONG></DT>
<DD>Recipe or rule not found.
 
</DD>
<DT><STRONG>409</STRONG></DT>
<DD>A conflict occurred.
 
</DD>
<DT><STRONG>500</STRONG></DT>
<DD>Some error occurred.
</DD>
</DL>

<P>

<H4><A NAME="SECTION000114330000000000000">
DELETE</A>
</H4> method can be used to delete a rule from the recipe.

<P>
This is the example with cURL (Note: If the recipe is omitted the rule is deleted from the ontology.):
<PRE>
$ curl -X -G \ 
-d recipe=http://recipe-iri
-d rule=htto://my.rule-iri
 http://localhost:8080/rule
 (Note: this delates the rule from the recipe)

$ curl -X -G \ 
-d rule=htto://my.rule-iri
 http://localhost:8080/rule
 (Note: this delates the rule from the ontology)
</PRE>

<P>
Possible responses:
<DL>
<DT><STRONG>200</STRONG></DT>
<DD>The rule has been deleted
 
</DD>
<DT><STRONG>204</STRONG></DT>
<DD>The rule has not been deleted
 
</DD>
<DT><STRONG>404</STRONG></DT>
<DD>Recipe or rule not found.
 
</DD>
<DT><STRONG>409</STRONG></DT>
<DD>A conflict occurred.
 
</DD>
<DT><STRONG>500</STRONG></DT>
<DD>Some error occurred.
</DD>
</DL>

<P>

<H3><A NAME="SECTION000114400000000000000">
/recipe</A>
</H3>
The <I>/recipe</I> service provides recipe access, creation and deletion. 

<P>
Supported HTTP methods: <B>GET</B>; to retrive the recipes; <B>POST</B> to create a new recipe; <B>DELETE</B>, to delete a recipe.

<P>

<H4><A NAME="SECTION000114410000000000000">
GET</A>
</H4> requests returns the description of a recipe. With <I>/recipe/all</I> it returns the list of all available recipes.
The call can be followed by the IRI of a recipe to obtain the details (and rules) loaded in.

<P>
Get the list of all the available recipes:
<PRE>
$ curl -X GET -H "Accept: application/rdf+xml" \ 
	http://localhost:8080/recipe/all
</PRE>

<P>
Get the recipe definition with all embedded rules:
<PRE>
$ curl -X GET -H "Accept: application/rdf+xml" \
     http://localhost:8080/recipe/http://recipe-iri
</PRE>

<P>
Possible responses:
<DL>
<DT><STRONG>200</STRONG></DT>
<DD>The recipe is retrieved
 
</DD>
<DT><STRONG>404</STRONG></DT>
<DD>The recipe does not exists
 
</DD>
<DT><STRONG>500</STRONG></DT>
<DD>Some error occurred 
</DD>
</DL>

<P>

<H4><A NAME="SECTION000114420000000000000">
POST</A>
</H4> requests can add a recipe. 

<P>
Supported parameters are: 
<BR>
<P>
<TABLE CELLPADDING=3 BORDER="1" WIDTH="100%">
<TR><TH ALIGN="LEFT"><B>Parameter</B></TH>
<TH ALIGN="LEFT"><B>Mandatory</B></TH>
<TH ALIGN="LEFT" VALIGN="TOP" WIDTH=283><B>Description</B></TH>
</TR>
<TR><TH ALIGN="LEFT"><I>recipe</I></TH>
<TD ALIGN="LEFT">Y</TD>
<TD ALIGN="LEFT" VALIGN="TOP" WIDTH=283>the recipe name (or full IRI) to create. If a simple string is provided, the default base IRI is applied.</TD>
</TR>
<TR><TH ALIGN="LEFT"><I>description</I></TH>
<TD ALIGN="LEFT">Y</TD>
<TD ALIGN="LEFT" VALIGN="TOP" WIDTH=283>A description of the recipe.</TD>
</TR>
</TABLE>

<P>
Add a recipe (Note: the recipe is added without any rule.):
<PRE>
$ curl -X POST \ 
 -F recipe = http://recipe-iri
 -F description
	http://localhost:8080/recipe
</PRE>

<P>
Possible responses:
<DL>
<DT><STRONG>200</STRONG></DT>
<DD>The recipe has been created.
 
</DD>
<DT><STRONG>409</STRONG></DT>
<DD>A conflict occurred. Probably the recipe already exists, so it cannot be created.
 
</DD>
<DT><STRONG>500</STRONG></DT>
<DD>Some error occurred 
</DD>
</DL>

<P>

<H4><A NAME="SECTION000114430000000000000">
DELETE</A>
</H4> method can be used to delete a recipe.

<P>
This is the example with cURL (Note: only the recipe is deleted but not the rules of the recipe.):
<PRE>
$ curl -X -G \ 
-d recipe="http://my-recipe-iri"
 http://localhost:8080/recipe
</PRE>

<P>
Possible responses:
<DL>
<DT><STRONG>200</STRONG></DT>
<DD>The recipe has been removed.
 
</DD>
<DT><STRONG>409</STRONG></DT>
<DD>A conflict occurred. Probably the recipe not exists, so it cannot be deleted.
 
</DD>
<DT><STRONG>500</STRONG></DT>
<DD>Some error occurred 
</DD>
</DL>

<P>

<H3><A NAME="SECTION000114500000000000000">
/reasoner/classify</A>
</H3>
This service returns a set of axioms which is the result of classification process from a given input (can be a file or the IRI of a graph in the knowledge store) with respect to a scope and recipe. Supported HTTP method is <B>POST</B>.

<P>
Supported parameters are: 
<BR>
<P>
<TABLE CELLPADDING=3 BORDER="1" WIDTH="100%">
<TR><TH ALIGN="LEFT"><B>Parameter</B></TH>
<TH ALIGN="LEFT"><B>Mandatory</B></TH>
<TH ALIGN="LEFT" VALIGN="TOP" WIDTH=283><B>Description</B></TH>
</TR>
<TR><TH ALIGN="LEFT"><I>scope</I></TH>
<TD ALIGN="LEFT">Y</TD>
<TD ALIGN="LEFT" VALIGN="TOP" WIDTH=283>the scope IRI used to classify the input.</TD>
</TR>
<TR><TH ALIGN="LEFT"><I>session</I></TH>
<TD ALIGN="LEFT">Y</TD>
<TD ALIGN="LEFT" VALIGN="TOP" WIDTH=283>the session IRI used to classify the input.</TD>
</TR>
<TR><TH ALIGN="LEFT"><I>recipe</I></TH>
<TD ALIGN="LEFT">N</TD>
<TD ALIGN="LEFT" VALIGN="TOP" WIDTH=283>the recipe IRI to apply</TD>
</TR>
<TR><TH ALIGN="LEFT"><I>file</I></TH>
<TD ALIGN="LEFT">N</TD>
<TD ALIGN="LEFT" VALIGN="TOP" WIDTH=283>A file to be given as input classified

<P>
<B>Cannot be used in conjunction with <I>input-graph</I></B>.</TD>
</TR>
<TR><TH ALIGN="LEFT"><I>input-graph</I></TH>
<TD ALIGN="LEFT">N</TD>
<TD ALIGN="LEFT" VALIGN="TOP" WIDTH=283>a reference to a graph IRI in the knowledge store 

<P>
<B>Cannot be used in conjunction with <I>file</I></B>.</TD>
</TR>
<TR><TH ALIGN="LEFT"><I>owllink-endpoint</I></TH>
<TD ALIGN="LEFT">N</TD>
<TD ALIGN="LEFT" VALIGN="TOP" WIDTH=283>The reasoner server end-point URL. 

<P>
If this parameter is not provided, the system will use the built-in reasoner.</TD>
</TR>
</TABLE>

<P>
The following request asks the service to classify the entities in the input file with relation to a certain scope (and recipe) using the built-in reasoner (HermiT): 
<PRE>
$ curl -X POST -H "Accept: application/rdf+xml" \
 -F scope="http://localhost:8080/ontonet/ontology/http://scope-iri/MyScope" \
 -F recipe="http://localhost:8080/recipe/http://recipe-iri/MyRecipe" \
 -F file=@/Users/enricodaga/foaf.rdf \
 	http://localhost:8080/reasoner/classify/
</PRE>

<P>
In the next case, the input file is classified with relation to a certain session using a reasoner via owl-link server end-point:
<PRE>
$ curl -X POST -H "Accept: application/rdf+xml" \
 -F session="http://localhost:8080/ontonet/ontology/User" \ 
 -F file=@/Users/enricodaga/foaf.rdf \	
 -F owllink-endpoint="http://reasoning.org/" \	
	http://localhost:8080/reasoner/classify/
</PRE>

<P>
The entities in the input graph are classified with relation to a certain scope using the built-in reasoner:
<PRE>
$ curl -X POST -H "Accept: application/rdf+xml" \
 -F scope="http://localhost:8080/ontonet/ontology/http://scope-iri/MyScope" \ 
 -F input-graph="http://cmsusers/" \		
 	http://localhost:8080/reasoner/classify/
</PRE>

<P>
In the last example the classification process includes the entities in the input graph with relation to a certain scope (and recipe) using a reasoner via owl-link server end-point:
<PRE>
$ curl -X POST -H "Accept: application/rdf+xml" \
 -F session="http://kres.iks-project.eu/session/1283861908122" \
 -F recipe="http://localhost:8080/recipe/http://recipe-iri/MyRecipe" \ 
 -F input-graph=http://wikinews/enhanced \
 -F owllink-endpoint="http://reasoning.org/" \
	http://localhost:8080/reasoner/classify/
</PRE>

<P>
Is due to note that only one kind of input (file or input-graph) at once is allowed, elsewhere the request will produce a 409 HTTP response code (Conflict). If the session is used then use of a scope is mandatory otherwise the request will produce a 400 HTTP response code (Bad request).

<P>
Possible responses:
<DL>
<DT><STRONG>200</STRONG></DT>
<DD>The ontology is retrieved, containing only new axioms result of the classification
 
</DD>
<DT><STRONG>400</STRONG></DT>
<DD>To run the session is needed the scope
 
</DD>
<DT><STRONG>404</STRONG></DT>
<DD>No data is retrieved
 
</DD>
<DT><STRONG>409</STRONG></DT>
<DD>Conflicts in parameters. Too much inputs.
 
</DD>
<DT><STRONG>500</STRONG></DT>
<DD>Some error occurred
</DD>
</DL>

<P>

<H3><A NAME="SECTION000114600000000000000">
/reasoner/check-consistency</A>
</H3>
This service provides consistency check. Supported HTTP methods are <B>GET</B>, <B>POST</B>.

<P>

<H4><A NAME="SECTION000114610000000000000">
GET</A>
</H4> requests will access a light-weight service, which will check the consistency of the dereferenciable resource IRI provided. 

<P>
For example, the following request:
<PRE>
$ curl -X [GET -H "Accept: text/turtle"] \ 
	http://localhost:8080/reasoner/classify/fileIri
</PRE>

<P>
Possible responses are:
<DL>
<DT><STRONG>200</STRONG></DT>
<DD>No data is retrieved, the graph IS consistent
 
</DD>
<DT><STRONG>204</STRONG></DT>
<DD>No data is retrieved, the graph IS NOT consistent
 
</DD>
<DT><STRONG>404</STRONG></DT>
<DD>Recipe/Scope/Ontology/InputFile doesn't exist
 
</DD>
<DT><STRONG>500</STRONG></DT>
<DD>Some error occurred
</DD>
</DL>

<P>

<H4><A NAME="SECTION000114620000000000000">
POST</A>
</H4> requests can ask for consistency check with relation to a scope, a session and a recipe, providing an input file or a input graph IRI.

<P>
Supported parameters for POST are:
<BR><TABLE CELLPADDING=3 BORDER="1" WIDTH="100%">
<TR><TH ALIGN="LEFT"><B>Parameter</B></TH>
<TH ALIGN="LEFT"><B>Mandatory</B></TH>
<TH ALIGN="LEFT" VALIGN="TOP" WIDTH=283><B>Description</B></TH>
</TR>
<TR><TH ALIGN="LEFT"><I>scope</I></TH>
<TD ALIGN="LEFT">Y</TD>
<TD ALIGN="LEFT" VALIGN="TOP" WIDTH=283>the scope IRI used to classify the input 

<P>
<B>Cannot be used in conjunction with <I>session</I></B>.</TD>
</TR>
<TR><TH ALIGN="LEFT"><I>session</I></TH>
<TD ALIGN="LEFT">Y</TD>
<TD ALIGN="LEFT" VALIGN="TOP" WIDTH=283>the session IRI used to classify the input 

<P>
<B>Cannot be used in conjunction with <I>scope</I></B>.</TD>
</TR>
<TR><TH ALIGN="LEFT"><I>recipe</I></TH>
<TD ALIGN="LEFT">N</TD>
<TD ALIGN="LEFT" VALIGN="TOP" WIDTH=283>the recipe IRI to apply</TD>
</TR>
<TR><TH ALIGN="LEFT"><I>file</I></TH>
<TD ALIGN="LEFT">N</TD>
<TD ALIGN="LEFT" VALIGN="TOP" WIDTH=283>A file to be given as input classified

<P>
<B>Cannot be used in conjunction with <I>input-graph</I></B>.</TD>
</TR>
<TR><TH ALIGN="LEFT"><I>input-graph</I></TH>
<TD ALIGN="LEFT">N</TD>
<TD ALIGN="LEFT" VALIGN="TOP" WIDTH=283>a reference to a graph IRI in the knowledge store 

<P>
<B>Cannot be used in conjunction with <I>file</I></B>.</TD>
</TR>
<TR><TH ALIGN="LEFT"><I>owllink-endpoint</I></TH>
<TD ALIGN="LEFT">N</TD>
<TD ALIGN="LEFT" VALIGN="TOP" WIDTH=283>The reasoner server end-point URL. 

<P>
If this parameter is not provided, the system will use the built-in reasoner (Hermit).</TD>
</TR>
</TABLE>

<P>
In the following example the service checks the consistency of the given RDF input with relation to a certain scope (and recipe) using the built-in reasoner (Hermit):
<PRE>
$ curl -X POST -H "Accept: application/rdf+xml" H "Content-type: text/turtle" \
 -F scope="http://localhost:8080/ontonet/ontology/http://scope-iri/MyScope" \
 -F recipe="http://localhost:8080/recipe/http://recipe-iri/MyRecipe" \
 -F file=@/Users/enricodaga/foaf.rdf \
     http://localhost:8080/reasoner/check-consistency/
</PRE>

<P>
The next checks the consistency of the given graph with relation to a certain scope (and recipe) using a reasoner via owl-link server end-point:
<PRE>
$ curl -X POST -H "Accept: application/rdf+" \
 -F scope="http://localhost:8080/ontonet/ontology/http://scope-iri/MyScope" \ 
 -F recipe="http://localhost:8080/recipe/http://recipe-iri/MyRecipe" \ 
 -F input-graph="http://cmsusers/" \
 -F owllink-endpoint="http://reasoning.org/" \
     http://localhost:8080/reasoner/check-consistency/
</PRE>

<P>
Possible responses are:
<DL>
<DT><STRONG>200</STRONG></DT>
<DD>No data is retrieved, the graph IS consistent
 
</DD>
<DT><STRONG>204</STRONG></DT>
<DD>No data is retrieved, the graph IS NOT consistent
 
</DD>
<DT><STRONG>404</STRONG></DT>
<DD>Scope either Ontology or recipe or RDF input not found
 
</DD>
<DT><STRONG>409</STRONG></DT>
<DD>Conflicts in parameters
 
</DD>
<DT><STRONG>500</STRONG></DT>
<DD>Some error occurred
</DD>
</DL>

<P>

<H3><A NAME="SECTION000114700000000000000">
/reasoner/enrich</A>
</H3>
This service runs the reasoner over the input data applying the given scope, a session and recipe and returns all the inferred knowledge.

<P>
Supported HTTP method is <B>POST</B>.
<BR>
Supported parameters are the following:
<BR><TABLE CELLPADDING=3 BORDER="1" WIDTH="100%">
<TR><TH ALIGN="LEFT"><B>Parameter</B></TH>
<TH ALIGN="LEFT"><B>Mandatory</B></TH>
<TH ALIGN="LEFT" VALIGN="TOP" WIDTH=283><B>Description</B></TH>
</TR>
<TR><TH ALIGN="LEFT"><I>scope</I></TH>
<TD ALIGN="LEFT">Y</TD>
<TD ALIGN="LEFT" VALIGN="TOP" WIDTH=283>the scope IRI used to classify the input.</TD>
</TR>
<TR><TH ALIGN="LEFT"><I>session</I></TH>
<TD ALIGN="LEFT">Y</TD>
<TD ALIGN="LEFT" VALIGN="TOP" WIDTH=283>the session IRI used to classify the input.</TD>
</TR>
<TR><TH ALIGN="LEFT"><I>recipe</I></TH>
<TD ALIGN="LEFT">N</TD>
<TD ALIGN="LEFT" VALIGN="TOP" WIDTH=283>the recipe IRI to apply</TD>
</TR>
<TR><TH ALIGN="LEFT"><I>file</I></TH>
<TD ALIGN="LEFT">N</TD>
<TD ALIGN="LEFT" VALIGN="TOP" WIDTH=283>A file to be given as input classified

<P>
<B>Cannot be used in conjunction with <I>input-graph</I></B>.</TD>
</TR>
<TR><TH ALIGN="LEFT"><I>input-graph</I></TH>
<TD ALIGN="LEFT">N</TD>
<TD ALIGN="LEFT" VALIGN="TOP" WIDTH=283>a reference to a graph IRI in the knowledge store 

<P>
<B>Cannot be used in conjunction with <I>file</I></B>.</TD>
</TR>
<TR><TH ALIGN="LEFT"><I>owllink-endpoint</I></TH>
<TD ALIGN="LEFT">N</TD>
<TD ALIGN="LEFT" VALIGN="TOP" WIDTH=283>The reasoner server end-point URL. 

<P>
If this parameter is not provided, the system will use the built-in reasoner (Hermit).</TD>
</TR>
</TABLE>

<P>
The following HTTP request explodes the given recipe with relation to a given input and session:
<PRE>
$ curl -X POST -H "Accept: application/rdf+xml" \
 -H "Content-type: application/rdf+xml" \
 -F session="http://kres.iks-project.eu/session/1283861908122" \
 -F recipe="http://localhost:8080/recipe/http://recipe/SameWorkplaceGeneration" \
 -F input=@/Users/enricodaga/foaf.rdf \
     http://localhost:8080/reasoner/enrich/
</PRE>

<P>
Explode the given recipe with relation to a given graph and scope:
<PRE>
$ curl -X POST -H "Accept: application/rdf+xml" \
 -F scope="http://localhost:8080/ontonet/ontology/MyScope" \
 -F recipe="http://localhost:8080/recipe/http://recipe-iri/MyRecipe" \ 
 -F input-graph=http://input-graph-iri \
 -F owllink-endpoint="http://reasoning.org/" \
     http://localhost:8080/reasoner/enrich/
</PRE>

<P>
Possible responses are:
<DL>
<DT><STRONG>200</STRONG></DT>
<DD>Returns a graph with the enrichments
 
</DD>
<DT><STRONG>204</STRONG></DT>
<DD>No enrichments have been produced from the given graph
 
</DD>
<DT><STRONG>404</STRONG></DT>
<DD>The recipe/ontology/scope/input doesn't exist in the network
 
</DD>
<DT><STRONG>409</STRONG></DT>
<DD>Conflicts in parameters
 
</DD>
<DT><STRONG>500</STRONG></DT>
<DD>Some error occurred
</DD>
</DL>

<P>

<H3><A NAME="SECTION000114800000000000000">
/refactor (/lazy and /consistent)</A>
</H3>
Supported HTTP methods: <B>GET</B>, made for getting the input from the knowledge store; <B>POST</B>, which implies an attached RDF file to be sent as input. The ontology network to be used can be pointed with the <I>scope</I> parameter only, to use a globally shared version of the network or providing also the <I>session</I> parameter, with the IRI of a user defined session obtained from the <I>/kres/session</I> service (described above).

<P>
For both HTTP methods, two modalities are provided: 

<OL>
<LI>the /refactor/lazy applies refactoring without considering consistency check;
</LI>
<LI>the /refactor/consistent applies refactoring considering consistency check;
</LI>
</OL>

<P>

<H4><A NAME="SECTION000114810000000000000">
GET</A>
</H4>

<P>
Supported parameters for GET requets: 
<BR><TABLE CELLPADDING=3 BORDER="1" WIDTH="100%">
<TR><TH ALIGN="LEFT"><B>Parameter</B></TH>
<TH ALIGN="LEFT"><B>Mandatory</B></TH>
<TH ALIGN="LEFT" VALIGN="TOP" WIDTH=283><B>Description</B></TH>
</TR>
<TR><TH ALIGN="LEFT"><I>recipe</I></TH>
<TD ALIGN="LEFT">Y</TD>
<TD ALIGN="LEFT" VALIGN="TOP" WIDTH=283>The IRI of the recipe to apply</TD>
</TR>
<TR><TH ALIGN="LEFT"><I>input-graph</I></TH>
<TD ALIGN="LEFT">Y</TD>
<TD ALIGN="LEFT" VALIGN="TOP" WIDTH=283>The IRI of the graph to be refactored</TD>
</TR>
<TR><TH ALIGN="LEFT"><I>output-graph</I></TH>
<TD ALIGN="LEFT">N</TD>
<TD ALIGN="LEFT" VALIGN="TOP" WIDTH=283>The IRI of the graph to fill with refactored data. If the output-graph param is present, the output is put in the graph; elsewhere is returned back.</TD>
</TR>
</TABLE>

<P>
The following request process the input and produce the refactored data as output according to a given recipe:

<P>
<PRE>
$ curl -X GET -H "Accept: text/turtle" \
 -G -d recipe="http://localhost:8080/recipe/myRecipe" \
 -d input-graph="http://torefactor/graph" \
 -d output-graph="http://torefactor/output-graph" \
     http://localhost:8080/refactor/lazy
</PRE>

<P>
In the above case output is put in the graph "http://torefactor/output-graph". If the graph does not exist it is created otherwise a reference to the existing graph is taken. No consistency checking is performed to test the consistency of the refactored data set. While if 
the request is the following:

<P>
<PRE>
$ curl -X GET -H "Accept: text/turtle" \
 -G -d recipe="http://localhost:8080/recipe/http://recipes/MyRecipe" \
 -d input-graph="http://torefactor/graph" \
 -d output-graph="http://refactored/output-graph" \
     http://localhost:8080/refactor/consistent
</PRE>

<P>
also a consistency checking is required on the data set generated by the refactoring.

<P>

<H4><A NAME="SECTION000114820000000000000">
POST</A>
</H4> requests perform refactoring over a file given as input.

<P>
Supported parameters for POST requets are: 
<BR><TABLE CELLPADDING=3 BORDER="1" WIDTH="100%">
<TR><TH ALIGN="LEFT"><B>Parameter</B></TH>
<TH ALIGN="LEFT"><B>Mandatory</B></TH>
<TH ALIGN="LEFT" VALIGN="TOP" WIDTH=283><B>Description</B></TH>
</TR>
<TR><TH ALIGN="LEFT"><I>recipe</I></TH>
<TD ALIGN="LEFT">Y</TD>
<TD ALIGN="LEFT" VALIGN="TOP" WIDTH=283>The IRI of the recipe to apply</TD>
</TR>
<TR><TH ALIGN="LEFT"><I>input</I></TH>
<TD ALIGN="LEFT">Y</TD>
<TD ALIGN="LEFT" VALIGN="TOP" WIDTH=283>The file containing the RDF data to be refactored</TD>
</TR>
</TABLE>

<P>
Next possible request is

<P>
<PRE>
$ curl -X POST -H "Accept: text/turtle" \
 -F recipe="http://localhost:8080/recipe/toMyOntology" \
 -F input=@/Users/enricodaga/foaf.rdf \
     http://localhost:8080/refactor/lazy
</PRE>
Here output returns back without consistency checking.

<P>
While

<P>
<PRE>
$ curl -X POST -H "Accept: text/turtle" \
 -F recipe="http://localhost:8080/recipe/toMyOntology" \
 -F input=@/Users/enricodaga/foaf.rdf \
     http://localhost:8080/refactor/consistent
</PRE>

<P>
returns back with consistency checking.

<P>
<DL>
<DT><STRONG>200</STRONG></DT>
<DD>Data has been refactored. Returns a RDF graph with the refactored data if -output-graph is not defined, else returns nothing.
 
</DD>
<DT><STRONG>204</STRONG></DT>
<DD>The defined recipe does not exists
 
</DD>
<DT><STRONG>404</STRONG></DT>
<DD>The recipe|input graph deos not exist (Some resource has not been found)
 
</DD>
<DT><STRONG>415</STRONG></DT>
<DD>Media type is not supported (only POST)
 
</DD>
<DT><STRONG>500</STRONG></DT>
<DD>Some error occurred
</DD>
</DL>

<P>

<H3><A NAME="SECTION000114900000000000000">
/reengineer/reengineer</A>
</H3>
For each input a dedicated service is provided:

<P>

<H4><A NAME="SECTION000114910000000000000">
Input is a document (Mime-type based)</A>
</H4>.

<P>
This service process the input and produce the reengineered data as output.

<P>
Supported HTTP method: <B>POST</B>

<P>
Supported parameters: 
<BR><TABLE CELLPADDING=3 BORDER="1" WIDTH="100%">
<TR><TH ALIGN="LEFT"><B>Parameter</B></TH>
<TH ALIGN="LEFT"><B>Mandatory</B></TH>
<TH ALIGN="LEFT" VALIGN="TOP" WIDTH=283><B>Description</B></TH>
</TR>
<TR><TH ALIGN="LEFT"><I>input-type</I></TH>
<TD ALIGN="LEFT">N</TD>
<TD ALIGN="LEFT" VALIGN="TOP" WIDTH=283>The mime type is used for detecting the reengineering engine to run. If the input-type is omitted, then the system tries to detect it.</TD>
</TR>
<TR><TH ALIGN="LEFT"><I>input</I></TH>
<TD ALIGN="LEFT">Y</TD>
<TD ALIGN="LEFT" VALIGN="TOP" WIDTH=283>The input file to reengineer</TD>
</TR>
<TR><TH ALIGN="LEFT"><I>recipe</I></TH>
<TD ALIGN="LEFT">N</TD>
<TD ALIGN="LEFT" VALIGN="TOP" WIDTH=283>If present, the requested recipe will be applied</TD>
</TR>
<TR><TH ALIGN="LEFT"><I>output-graph</I></TH>
<TD ALIGN="LEFT">N</TD>
<TD ALIGN="LEFT" VALIGN="TOP" WIDTH=283>If present, the result is fed in the given graph</TD>
</TR>
</TABLE>

<P>
Both the following examples are valid requests:

<P>
<PRE>
$ curl -X POST -H "Accept: text/turtle" \
 -F input-type="application/rdf+xml"
 -F input=@/Users/enricodaga/profile.xml \
     http://localhost:8080/reengineer/reengineer/

$ curl -X POST -H "Accept: text/turtle" \
 -F recipe="http://localhost:8080/recipe/xml2owl" \
 -F input=@/Users/enricodaga/profile.xml \
     http://localhost:8080/reengineer/reengineer/
</PRE>

<P>
Possible responses: 
<BR><DL>
<DT><STRONG>200</STRONG></DT>
<DD>Data has been reengineered. Returns a graph with the reengineered data if -output-graph is not defined, else returns nothing.
 
</DD>
<DT><STRONG>204</STRONG></DT>
<DD>The following things can happen:

<P>
- No recipes have been found for the input given 

<P>
- The defined recipe cannot be applied to such content
 
</DD>
<DT><STRONG>404</STRONG></DT>
<DD>The recipe|output graph deos not exist (Some resource has not been found)
 
</DD>
<DT><STRONG>500</STRONG></DT>
<DD>Some error occurred
</DD>
</DL>

<P>

<H4><A NAME="SECTION000114920000000000000">
Input is a DBMS connection</A>
</H4>. 

<P>
This service is available in the sub-path <B>/dbsource</B>. It connects to the database, extract the data and put it in the given graph.

<P>
Supported HTTP method: <B>POST</B>

<P>
Supported parameters: 
<BR><TABLE CELLPADDING=3 BORDER="1" WIDTH="100%">
<TR><TH ALIGN="LEFT"><B>Parameter</B></TH>
<TH ALIGN="LEFT"><B>Mandatory</B></TH>
<TH ALIGN="LEFT" VALIGN="TOP" WIDTH=283><B>Description</B></TH>
</TR>
<TR><TH ALIGN="LEFT"><I>db</I></TH>
<TD ALIGN="LEFT">Y</TD>
<TD ALIGN="LEFT" VALIGN="TOP" WIDTH=283>The name of the database</TD>
</TR>
<TR><TH ALIGN="LEFT"><I>namespace</I></TH>
<TD ALIGN="LEFT">Y</TD>
<TD ALIGN="LEFT" VALIGN="TOP" WIDTH=283>The base namespace for the reengineering</TD>
</TR>
<TR><TH ALIGN="LEFT"><I>jdbc</I></TH>
<TD ALIGN="LEFT">Y</TD>
<TD ALIGN="LEFT" VALIGN="TOP" WIDTH=283>JDBC driver to use for the connection</TD>
</TR>
<TR><TH ALIGN="LEFT"><I>protocol</I></TH>
<TD ALIGN="LEFT">Y</TD>
<TD ALIGN="LEFT" VALIGN="TOP" WIDTH=283>Protocol part of the DB connection</TD>
</TR>
<TR><TH ALIGN="LEFT"><I>host</I></TH>
<TD ALIGN="LEFT">Y</TD>
<TD ALIGN="LEFT" VALIGN="TOP" WIDTH=283>Host of the DBMS</TD>
</TR>
<TR><TH ALIGN="LEFT"><I>port</I></TH>
<TD ALIGN="LEFT">Y</TD>
<TD ALIGN="LEFT" VALIGN="TOP" WIDTH=283>Port</TD>
</TR>
<TR><TH ALIGN="LEFT"><I>username</I></TH>
<TD ALIGN="LEFT">Y</TD>
<TD ALIGN="LEFT" VALIGN="TOP" WIDTH=283>User of the DBMS</TD>
</TR>
<TR><TH ALIGN="LEFT"><I>password</I></TH>
<TD ALIGN="LEFT">Y</TD>
<TD ALIGN="LEFT" VALIGN="TOP" WIDTH=283>Password</TD>
</TR>
<TR><TH ALIGN="LEFT"><I>output-graph</I></TH>
<TD ALIGN="LEFT">N</TD>
<TD ALIGN="LEFT" VALIGN="TOP" WIDTH=283>The IRI of the graph to fed with the result</TD>
</TR>
<TR><TH ALIGN="LEFT"><I>recipe</I></TH>
<TD ALIGN="LEFT">N</TD>
<TD ALIGN="LEFT" VALIGN="TOP" WIDTH=283>The IRI of the recipe to apply</TD>
</TR>
</TABLE>

<P>
<PRE>
$ curl -X POST -H "Accept: text/plain" \
 -F db="customer" \
 -F namespace="http://reengineeringexample/entity/customer/" \
 -F jdbc="com.mysql.jdbc.Driver" \
 -F protocol="jdbc:mysql://" \
 -F host="127.0.0.1" \
 -F port="3306" \
 -F username="admin" \
 -F password="password" \
 -F output-graph="http://reengineeringexample/" \
 -F recipe="http://localhost:8080/recipe/mydbschema2owl" \
     http://localhost:8080/reengineer/reengineer/dbsource
</PRE>

<P>
Possible responses:
<DL>
<DT><STRONG>200</STRONG></DT>
<DD>Data has been reengineered. Returns a graph with the reengineered data if -output-graph is not defined, else returns nothing.
 
</DD>
<DT><STRONG>204</STRONG></DT>
<DD>The defined recipe cannot be applied to such content
 
</DD>
<DT><STRONG>404</STRONG></DT>
<DD>The recipe|output graph deos not exist (Some resource has not been found)
 
</DD>
<DT><STRONG>415</STRONG></DT>
<DD>Media type is not supported
 
</DD>
<DT><STRONG>500</STRONG></DT>
<DD>Some error occurred
</DD>
</DL>

<!-- end of file -->
</@common.page>
</#escape>
