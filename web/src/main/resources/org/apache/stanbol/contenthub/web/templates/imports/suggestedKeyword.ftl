<#macro suggestedKeyword set key>
	<#assign limit = 4>
		Related ${key}
		<#assign relatedKey = "related"+key />
		<ul id="${relatedKey}list" class="spadded">
			<#assign x = 0/>
			<#list set as related>
				<#assign related = related?replace('_',' ')>
				<#if x == limit><#break/></#if>
				<li><a href=javascript:getResults(null,null,"${related}","explore")>${related}</a></li>
				<#assign x = x + 1>
			</#list>
		</ul>
		<#if set?size &gt; limit>
			<a id="${relatedKey}" href="">more</a><br>
		</#if>
		<br>
		
	<script language="javascript">
		function moreLessButtonHandler() {
		   $("#${relatedKey}", this).click(function(e) {
		     // disable regular form click
		     e.preventDefault();
		     if(document.getElementById("${relatedKey}").innerHTML == "more")
		     {
		     	var a="<#list set as related><#assign related = related?replace('_',' ')><li><a href=javascript:getResults(null,null,'${related}','explore')>${related?replace('_', ' ')}</a></li></#list>";
		     	document.getElementById("${relatedKey}list").innerHTML=a;
		     	$(this).attr({ 'innerHTML': 'less' });
			 }
			 else
			 {
			 	var a="<#assign x=0><#list set as related><#assign related = related?replace('_',' ')><#if x == limit><#break/></#if><li><a href=javascript:getResults(null,null,'${related}','explore')>${related?replace('_', ' ')}</a></li><#assign x=x+1 /></#list>";
			 	document.getElementById("${relatedKey}list").innerHTML=a;
			 	$(this).attr({ 'innerHTML': 'more' });		 	
			 }    
		     });
		 }		 
		 
		 $(document).ready(moreLessButtonHandler);
	</script>
</#macro>