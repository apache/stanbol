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
<#setting url_escaping_charset='ISO-8859-1'>
<#escape x as x?html>

<#import "/imports/common.ftl" as common>
<#import "/imports/keyword_result_tab.ftl" as keywordTab>
<#import "/imports/suggestedKeyword.ftl" as suggestedKeyword>
<#import "/imports/facetResultMacro.ftl" as facetResultMacro>
<#import "/imports/relatedKeywordMacro.ftl" as relatedKeywordMacro>

<#-- limit for the more less button -->
<#assign limit=4>
<@common.page title="Search" hasrestapi=false>

	<div id="resultContainer">
		<input type="hidden" id="keywordIn" value="${it.templateData.keywords}" />
		<#if it.templateData.engines?exists && it.templateData.engines?size != 0>
			<#list it.templateData.engines as engine>
				<input class="searchengine" type="hidden" name="${engine}" value="${engine}" />
			</#list>
		</#if>
		<div id="previousSuggestionButton" class="invisible"></div>
		<div id="result" class="result">
			<a href="${it.publicBaseUri}contenthub/search">Back to Search</a></br>
			<#assign con=it.templateData.context>
			<!--General Divs for layout  -->
			
			<div class="keywords">
				<#list con.queryKeyWords?sort_by("scoreString")?reverse as qk>
					<h3  class="keywordItem keywordClickable" id="kw_${qk.keyword?replace("*","_")?replace(" ", "_")?replace("'", "_")}">${qk.keyword}</h3>
					<div id="allSuggestions">
						<#if qk.relatedKeywords?exists && qk.relatedKeywords?size != 0>
							<#list qk.relatedKeywords?keys as mapKey>
								<#assign listOfKey = qk.relatedKeywords[mapKey]>
								<#if listOfKey?size &gt; 0>
									<@relatedKeywordMacro.relatedKeywordMacro relatedKeywordList = listOfKey source = mapKey/>
								</#if>
							</#list>
						</#if>
						<#-- this division includes the results coming from entityHub -->
						<div id="entityHubSuggestionSubDiv"></div>
						<#if (!qk.relatedKeywords?exists || qk.relatedKeywords?size == 0) && (!it.suggestions?exists || it.suggestions?size == 0)>
							<div id="noRelatedKeywordDivision">No related keyword</div>
						</#if>
					</div>
				</#list>
			</div>
			
			<div class="resources">
				<fieldset>
					<#list con.queryKeyWords?sort_by("score") as qk>
						<@keywordTab.keywordTab kw=qk/>
					</#list>
				</fieldset>
			</div>
			
			<div class="chosenfacets">
				<#if it.templateData.constraints != "{}">
					<fieldset>
						<div id="chosenFacets"></div>
						<div id="chosenFacetsHidden" class="invisible">'${it.templateData.constraints?js_string}'</div>			
					</fieldset>
				</#if>
			</div>
			<br/>
			<div class="facets" id="facets">
		
				<#if it.facets?exists && it.facets?size != 0>
					<fieldset>
						<br/>
						<#list it.facets as facet>
							<@facetResultMacro.facetResultMacro facetField=facet consLink=it.templateData.constraints/>
						</#list>
					</fieldset>
				</#if>
			</div>
		</div>
	</div>
	
	<!-- To be populated by the second ajax for the suggestions div -->
	<div id="tempEntityHubSuggestions" class="invisible"></div>
	
	<!-- to be populated by the list of suggested keywords to be able to get back in search -->
	<div id="suggestedKeywordList" class="invisible">{"keywords":[]}</div>	
	<!-- FIXME put a textarea so jQuery-ui tabs does not expand through footer -->
	<textarea type="text" disabled="true" style="border-color: #fff; background: white; height:100px; max-height:100px; width:100%; max-width:100%"></textarea>
	
	<script language="javascript">
	
		$(document).ready(init);
	
		function init() {
		
			$.ajax({
				url : "${it.publicBaseUri}contenthub/search/suggestion",
				type : "POST",
				async: true,
				data: {keyword: $("#keywordIn").val()},
				dataType: "html",
				cache: false,
				success: function(result) {
					if(!document.getElementById("allSuggestions")) {
						$("#tempEntityHubSuggestions").text(result);
					}
					else {
					// in this part, gets the result, and checks if it is empty, then dont remove the No Related Keyword Text
						$("#tempEntityHubSuggestions").text(result);
						$("#entityHubSuggestionSubDiv").html(result);
						var x = document.getElementById("entityHubSuggestions").innerHTML;
						if(x != "\n")
						$("#noRelatedKeywordDivision").remove();
					}		
				},
				error: function(result) {
					$("#busyIcon").addClass("invisible");
					alert(result.status + ' ' + result.statusText);
				}
			});
		
			$(".keywords").accordion({collapsible: true, autoHeight: false });
			$(".keywords").removeClass("ui-widget");
			$(".resources > div").tabs({fx: { height: 'toggle', opacity: 'toggle' } });
			
			//collapsible content
			$(".collapseItem").click(function(e){
				e.preventDefault();
				$(this).next(".collapseContent").slideToggle(500);
			}); 
			
			$("#entityHubSuggestionSubDiv").html($("#tempEntityHubSuggestions").text());			
		}
		
		function getResults(jsonCons,facetName,facetValue,operation){
			//clears the content of div because it'll be filled by explorer posts
			//$("#tempEntityHubSuggestions").empty();
			$("#allSuggestions").remove();
						
			var keywordToSearch = $("#keywordIn").val();
			
			if(typeof(jsonCons) == "undefined") {
				jsonCons = "{}";
				var suggestedList = JSON.parse('{"keywords" : []}');
				suggestedList['keywords'].push($("#keywordIn").val());
				$("#suggestedKeywordList").text(JSON.stringify(suggestedList));
			}
			var JSONObject = JSON.parse(jsonCons);
			if(typeof(facetName) != "undefined" || typeof(facetValue != "undefined")) {
							
				if(operation == "addFacet") {
					if(JSONObject[facetName] != null)
					{
						if(JSONObject[facetName].indexOf(facetValue) == -1) {
							JSONObject[facetName].push(facetValue);
						}
					} else {
						JSONObject[facetName] = new Array();
						JSONObject[facetName].push(facetValue);
					}
				}
				
				else if(operation == "deleteFacet") {
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
				}
				
			}
			if(operation == "explore")
			{
				
				$("#keywordIn").val(facetValue);
				var suggestedList = JSON.parse(document.getElementById("suggestedKeywordList").innerHTML);
				
				var previousString = "";
				for(i = 0; i  < suggestedList['keywords'].length ; i++)
				{
					if(i != 0)
					{
						previousString += " > ";
					}
					previousString += "<a href=javascript:getResults(null,null,'" + suggestedList['keywords'][i] + "','previousSuggestion')> " + suggestedList['keywords'][i] + "</a>";
				}
				$("#previousSuggestionButton").html(previousString);
				//adds the last entered word to list and saves it in a hidden division
				suggestedList['keywords'].push(facetValue);
				
				//decides when to show back division
				if(suggestedList['keywords'].length > 1)
				{
					$("#previousSuggestionButton").removeClass('invisible');
				}
				else
				{
					$("#previousSuggestionButton").addClass('invisible');
				}
				$("#suggestedKeywordList").text(JSON.stringify(suggestedList));
			}
			
			//if back button is pressed, previous suggestion is searched again and suggestionList is fixed
			else if(operation == "previousSuggestion")
			{
				var suggestedList = (JSON.parse(document.getElementById("suggestedKeywordList").innerHTML));
				var length = suggestedList['keywords'].length;
				var index = suggestedList['keywords'].indexOf(facetValue);
				
				suggestedList['keywords'] = suggestedList['keywords'].slice(0,index);				
				
				$("#suggestedKeywordList").text(JSON.stringify(suggestedList));
				getResults(null,null,facetValue,"explore");
			}
			else if(operation == "date"){
			
				var JSONObject = JSON.parse(jsonCons);
				var facetValue = "[" + document.getElementById("dateFrom").value + "T00:00:00Z TO " + 
										document.getElementById("dateTo").value + "T23:59:59Z]";
				JSONObject[facetName] = new Array();
				JSONObject[facetName].push(facetValue);
			}
			else if(operation == "range"){
			
				var JSONObject = JSON.parse(jsonCons);
				var facetValue = "[" + document.getElementById(facetName+"TextMin").value + " TO " + 
										document.getElementById(facetName+"TextMax").value + "]";
										
				JSONObject[facetName] = new Array();
				JSONObject[facetName].push(facetValue);
			}
			
			//accumulate all selected engines in an array
			var engines_selected = [];
			$(".searchengine").each(function(){
				engines_selected.push($(this).val());
			});
		    
		    //make text area invisible
			//	$("#searchResult").fadeOut(100);
			//	$("#resultContainer").fadeOut(100);
				//show busy icon
				$("#busyIcon").removeClass("invisible");
		    
			var graph_selected = "";
			var graphInCombo = document.getElementById('graphIn');
			if (graphInCombo != null) {
				var selectedIndex = graphInCombo.selectedIndex;
				if(selectedIndex != 0) {
					graph_selected = $("#graphIn option:selected").val();
				}
			}
			//means if there is need to recalculate the suggestions from external resource such as entityhub
			if(operation == "first" || operation == "explore" || operation == "previousSuggestion") {
				$.ajax({
					url : "${it.publicBaseUri}contenthub/search/suggestion",
					type : "POST",
					async: true,
					data: {keyword: $("#keywordIn").val()},
					dataType: "html",
					cache: false,
					success: function(result) {
						if(!document.getElementById("allSuggestions")) {
							$("#tempEntityHubSuggestions").text(result);
						}
						else {
						// in this part, gets the result, and checks if it is empty, then dont remove the No Related Keyword Text
							$("#tempEntityHubSuggestions").text(result);
							$("#entityHubSuggestionSubDiv").html(result);
							var x = document.getElementById("entityHubSuggestions").innerHTML;
							if(x != "\n")
							$("#noRelatedKeywordDivision").remove();
						}		
					},
					error: function(result) {
						$("#busyIcon").addClass("invisible");
						alert(result.status + ' ' + result.statusText);
					}
				});
			}
			$.ajax({
				url : "${it.publicBaseUri}contenthub/search",
				type : "POST",
				async: true,
				data: {keywords: $("#keywordIn").val(), graph: graph_selected, engines: engines_selected, constraints: JSON.stringify(JSONObject)},
				dataType: "html",
				cache: false,
				success: function(result) {
					$("#busyIcon").addClass("invisible");
					$("#search").addClass("invisible");
						
					$("#result").replaceWith(result.substr(result.indexOf("</div>")+6));
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
					
					$("#entityHubSuggestionSubDiv").html($("#tempEntityHubSuggestions").text());
				},
				error: function(result) {
					$("#busyIcon").addClass("invisible");
					alert(result.status + ' ' + result.statusText);
				}
			});
			
			
			
			
		}
	
		function setChosenFacet(JSONObject)	{
			var resultString = "";
			var chosenCons = $("#chosenFacetsHidden").attr("innerHTML");
							
			if(JSONObject != null) {
				for(var p in JSONObject) {
					if(JSONObject.hasOwnProperty(p)) {
						for(var value in p) {
							if(p.hasOwnProperty(value) && typeof(JSONObject[p][value]) != "undefined") {
								var escapedFacetName = encodeURI(p.toString());
								var escapedFacetValue = encodeURI(JSONObject[p][value]);
								var startindex = (isReserved(p)) ? p.toString().indexOf("_")+1 : 0;
								var lastindex = (isReserved(p)) ? p.length : p.toString().lastIndexOf("_");
								var href = "<a href=javascript:getResults("; 
								href += 	encodeURI(chosenCons) + ",\"";
								href +=		escapedFacetName + "\",\"" + escapedFacetValue + "\",\"deleteFacet\") title='Remove'>";
								href +=     "<img src='${it.staticRootUrl}/contenthub/images/delete_icon_16.png'></a>";
								href +=		p.toString().substring(startindex, lastindex) + " : " + 
											((isReserved(p)) ? JSONObject[p][value].substring(1,11)+" to "+JSONObject[p][value].substring(25,35) : 
											JSONObject[p][value]) + "<br/>";
								resultString += href;
							}
						}
					}
				}
			}
			var a = document.getElementById('chosenFacets');
			if(a != null) {
				a.innerHTML = resultString;
			}
		}
				
		function isReserved(str){
			return str.indexOf("stanbolreserved") == 0; 
		}
		
	</script>
</@common.page>
</#escape>
