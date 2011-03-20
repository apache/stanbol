<#import "/imports/common.ftl" as common>
<#escape x as x?html>
<@common.page title="KReS Semiom Refactorer">

<form id="kres" action="/kres/refactorer" method="POST"
 enctype="application/x-www-form-urlencoded"
 accept="application/sparql-results+xml, application/rdf+xml">
Select a recipe:
<select name="recipeSelect">
<option value="">
</select>
var = <http://kres.iksproject.eu/rules#> .
</textarea>

Select a graph

<#if it.graphs?size == 0>
	<p><em>There is no stored MGraph.</em></p>
<#else>
	<ul>
	<#list it.graphs as graph>
		<li><input type="radio" name="datasetURI" value="${graph}"/>${graph}
	</#list>
	</ul>
	<br>
</#if>

<p><input type="submit" class="submit" value="refactoring"/></p>

</form>

</@common.page>
</#escape>
