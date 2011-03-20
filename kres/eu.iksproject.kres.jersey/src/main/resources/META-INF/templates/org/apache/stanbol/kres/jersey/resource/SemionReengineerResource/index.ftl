<#import "/imports/common.ftl" as common>
<#escape x as x?html>
<@common.page title="KReS Semion Reengineer">

  <p>The Semion Reengineer helps users to transform structured non-RDF data sources into RDF ones.
  Currently supported data sources are:</p>
  <ul class="kressList">
  <li> Relational Databases
  <li> Document
  </ul>
  <p>Next developing will support:</p>
  <ul class="kressList">
  <li> RSS
  <li> iCalendar
  </ul>
	
<p>Tranform a data source</p>
<select name="data-source-type" onchange="showReengineer(this.selectedIndex)">
<option value="empty">
<option value="rdb">Relational DB
<option value="xml">XML
</select><br/>

<div id="data-source-form">
</div>

</@common.page>
</#escape>
