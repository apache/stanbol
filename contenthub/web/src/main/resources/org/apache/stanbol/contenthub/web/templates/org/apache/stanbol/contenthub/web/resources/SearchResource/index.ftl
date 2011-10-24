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
<@common.page title="Search" hasrestapi=false>
	<div id="search">
		<form method="POST" accept="text/html" accept-charset="utf-8">
			<fieldset>
				<legend>Keyword Based Search</legend>
				<p>
					Keywords: <input id="keywordIn" name="topic" type="text"/><br/>
				</p>
				<p>
					<!-- Ontology selection combobox-->
					<#if it.templateData.ontologies?exists && it.templateData.ontologies?size != 0>
					Graph: <select  id="graphIn" name="max" type="text" value="5">
						<#list it.templateData.ontologies as ont>
							<option value="${ont}">${ont}</option>
						</#list>
						</select>
					<#else>
						<p><i>No graphs to search.<i><p> 
					</#if>
				</p>
					<!-- Engine selection checkboxes-->
					<fieldset>
						<legend>Active Engines</legend>
						<#if it.templateData.engines?exists && it.templateData.engines?size != 0>
							<ul>
							<#list it.templateData.engines as engine>
								<li>
								<input class="searchengine checkbox" type="checkbox" checked="checked" name="${engine.className}" value="${engine.objectName}">${engine.className}</input>
								<input type="button" class="configure" title="Configure Engine" onclick="window.location.href='/system/console/configMgr/${engine.className}'"></button>
								</li>
							</#list>
							</ul>
						<#else>
							<p><i>There are no engines<i><p>
						</#if>
					</fieldset>
					<p>
						<input id="submitIn" type="submit" value="Search"></input>
					</p>
					<img id="busyIcon" class="invisible centerImage" src="${it.staticRootUrl}/contenthub/images/ajax-loader.gif"/>
			</fieldset>
		</form>
	</div>	
	
	<div id="resultContainer" class="invisible">
		<div>
			<!-- To be populated with ajax without xml :)-->
		</div>
	</div>
	<!-- FIXME put a textarea so jQuery-ui tabs does not expand through footer -->
	<textarea type="text" disabled="true" style="border-color: #fff; background: white; height:100px; max-height:100px; width:100%; max-width:100%"></textarea>
	
	<script language="javascript">
	
		function init() {
		
			$("#submitIn", this).click(function(e) {
				// disable regular form click
				e.preventDefault();
	     
				//accumulate all selected engines in an array
				var engines_selected = [];
				$(".searchengine ").each(function(){
				
					if($(this).attr("checked")){
						engines_selected.push($(this).val());
					}	
				});
	
				var graph_selected = $("#graphIn option:selected").val();
		     
				//make text area invisible
				$("#searchResult").fadeOut(100);
				$("#resultContainer").fadeOut(100);
				//show busy icon
				$("#busyIcon").removeClass("invisible");
		     
				$.ajax({
					type : "POST",
					async: true,
					data: {keywords: $("#keywordIn").val(), graph: graph_selected, engines: engines_selected},
					dataType: "html",
					cache: false,
					success: function(result) {
						// since post does not create any resource, there is no possibility to redirect
						$("#busyIcon").addClass("invisible");
						$("#search").addClass("invisible");
		       	 
						$("#resultContainer > div").replaceWith(result.substr(result.indexOf("</div>")));
						$(".keywords").accordion({collapsible: true, autoHeight: false });
						$(".keywords").removeClass("ui-widget");
						$(".resources > div").tabs({fx: { height: 'toggle', opacity: 'toggle' } });
						$("#resultContainer").fadeIn("slow");
		       	 
						//collapsible content
						$(".collapseItem").click(function(e){
							e.preventDefault();
							$(this).next(".collapseContent").slideToggle(500);
						}); 
					},
					error: function(result) {
						$("#busyIcon").addClass("invisible");
						alert(result.status + ' ' + result.statusText);
					}
				});
			});
	   
			//accordion
			$(".keywords").accordion({collapsible: true});
		}
		
		$(document).ready(init);
	 
		function hideDiv(){
			$('#facets').hide("slow");
		}
		function showDiv(){
			$('#facets').show("slow");
		}
		

		function getResults(jsonCons,facetName,facetValue){

			var JSONObject = JSON.parse(jsonCons);
			
			if(JSONObject[facetName] != null)
			{
				if(JSONObject[facetName].indexOf(facetValue) == -1) {
					JSONObject[facetName].push(facetValue);
				}
			} else {
				JSONObject[facetName] = new Array();
				JSONObject[facetName].push(facetValue);
			}
		
			//accumulate all selected engines in an array
			var engines_selected = [];
			$(".searchengine ").each(function(){
				if($(this).attr("checked")){
					engines_selected.push($(this).val());
				}	
			});
		     
			var graph_selected = $("#graphIn option:selected").val();
	
			$.ajax({
				type : "POST",
				async: true,
				data: {keywords: $("#keywordIn").val(), graph: graph_selected, engines: engines_selected, constraints: JSON.stringify(JSONObject)},
				dataType: "html",
				cache: false,
				success: function(result) {
					$("#resultContainer > div").replaceWith(result.substr(result.indexOf("</div>")));
					$(".keywords").accordion({collapsible: true, autoHeight: false });
					$(".keywords").removeClass("ui-widget");
					$(".resources > div").tabs({fx: { height: 'toggle', opacity: 'toggle' } });
					$("#resultContainer").fadeIn("slow");
	       	 
					//collapsible content
					$(".collapseItem").click(function(e){
						e.preventDefault();
						$(this).next(".collapseContent").slideToggle(500);
					}); 
					
	   				setChosenFacet(JSONObject);
				},
				error: function(result) {
					$("#busyIcon").addClass("invisible");
					alert(result.status + ' ' + result.statusText);
				}
			});
		}
	
		function setChosenFacet(JSONObject)	{
			var resultString = "";
			var chosenCons = document.getElementById('chosenFacetsHidden').innerHTML;
				
			if(JSONObject != null) {
				for(var p in JSONObject) {
					if(JSONObject.hasOwnProperty(p)) {
						for(var value in p) {
							if(p.hasOwnProperty(value) && typeof(JSONObject[p][value]) != "undefined") {
								var escapedFacetName = encodeURI(p.toString());
								var escapedFacetValue = encodeURI(JSONObject[p][value]);
								var lastindex = p.toString().lastIndexOf("_");
								var href = "<a href=javascript:deleteCons("; 
								href += 	encodeURI(chosenCons) + ",\"";
								href +=		escapedFacetName + "\",\"" + escapedFacetValue + "\") title='Remove'>";
								href +=     "<img src='${it.staticRootUrl}/contenthub/images/delete_icon_16.png'></a>";
								href +=		p.toString().substring(0, lastindex) + " : " + JSONObject[p][value] + "<br/>";
								resultString += href;
							}
						}
					}
				}
			}
			var a = document.getElementById('chosenFacets');
			a.outerHTML = resultString;
			a.innerHTML = resultString;
		}
	
		function deleteCons(jsonCons,facetName,facetValue) {

			var JSONObject = JSON.parse(jsonCons);
			var  values = JSONObject[facetName];
			
			var length=0;
			var index;
			for(var value in values) {			
				if(typeof(JSONObject[facetName][value]) != "undefined") {
					length++;
					if(JSONObject[facetName][value] == facetValue) {
						index = value;
					}
				}
			}
			
			
			if(length == 1) {
				delete JSONObject[facetName];
			} else {
				<#-- TODO: change -->
				delete JSONObject[facetName][index];
			}
			
			//accumulate all selected engines in an array
			var engines_selected = [];
			$(".searchengine ").each(function(){
				if($(this).attr("checked")){
					engines_selected.push($(this).val());
				}	
			});
		     
			var graph_selected = $("#graphIn option:selected").val();
	
			$.ajax({
				type : "POST",
				async: true,
				data: {keywords: $("#keywordIn").val(), graph: graph_selected, engines: engines_selected, constraints: JSON.stringify(JSONObject)},
				dataType: "html",
				cache: false,
				success: function(result) {
				 
					$("#resultContainer > div").replaceWith(result.substr(result.indexOf("</div>")));
					$(".keywords").accordion({collapsible: true, autoHeight: false });
					$(".keywords").removeClass("ui-widget");
					$(".resources > div").tabs({fx: { height: 'toggle', opacity: 'toggle' } });
					$("#resultContainer").fadeIn("slow");
	       	 
					//collapsible content
					$(".collapseItem").click(function(e){
						e.preventDefault();
						$(this).next(".collapseContent").slideToggle(500);
					}); 
					
					setChosenFacet(JSONObject);
				},
				error: function(result) {
					$("#busyIcon").addClass("invisible");
					alert(result.status + ' ' + result.statusText);
				}
			});
		}
	
	</script>
</@common.page>
</#escape>