<#import "/imports/common.ftl" as common>
<#escape x as x?html>
<@common.page title="Reasoning">

<div class="contentTag">
<input type="text" id="ontologyIRI"/>
<input type="button" id="consistency" value="consistency check"/>

</div>
<script type="text/javascript">
$(document).ready(function(){
	$('#consistency').click(function(event){
		event.preventDefault();
		var u= $('#ontologyIRI').val();
		$.get("/reasoner/check-consistency/"+u,function(data, txt, xhr){
			alert("Result: "+(xhr.status==200? "consistent":"not consistent"));
		});
	});
});
</script>
</@common.page>
</#escape>
