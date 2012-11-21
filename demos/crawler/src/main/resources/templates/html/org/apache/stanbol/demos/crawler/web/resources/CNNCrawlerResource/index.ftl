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
<@common.page title="CNN News Importer" hasrestapi=false>
	<form method="POST" accept="text/html" accept-charset="utf-8">
		<fieldset>
			<legend>Select an index</legend>
			<div id="indexDiv"><#--this div will be populated by ajax--></div>
		</fieldset>
		<fieldset>
			<legend>Enter a topic, max number of articles and summary/full news selection</legend>
			<p>
				Topic: <input id="topicIn" name="topic" type="text"/><br/>
			</p>
			<p>
				Max Number of Articles: <input  id="maxIn" name="max" type="text" value="5"/><br/>
			</p>
			<p>
				Full Articles: <input id="fullIn"  name="full" type="checkbox" checked="checked"/><br/>
			</p>
				<input id="submitIn" type="submit" value="Import News"><br/>
			</p>
			<img id="busyIcon" class="invisible centerImage" src="${it.staticRootUrl}/contenthub/images/ajax-loader.gif"/>
		</fieldset>
	</form>
	
	<!-- if you change this div, look out success function -->
	<div id="results">
		<#if it.templateData?exists>
			<fieldset>
				<legend>Articles found for topic: ${it.templateData.topic}</legend>
				<#if it.templateData.uris?exists && it.templateData.uris?size != 0>
					<ul>
					<#assign x=it.templateData.uris?size>
					<#list 0..x-1 as i>
						<#assign uri = it.templateData.uris[i]>
						<#assign title = it.templateData.titles[i]/>
						<li><a href="${it.publicBaseUri}contenthub/${it.indexName}/store/content/${uri}">
							<#if title??>${title}<#else>${uri}</#if>
						</a></li>
					</#list>  
					</ul>
				<#else>
					<p>No articles found for this topic<p>
				</#if>
			</fieldset>
		</#if>
	</div>
	
	<script language="javascript">
	
		function init() {
			$("#submitIn", this).click(function(e) {
				// disable regular form click
				e.preventDefault();
			     
				$("#busyIcon").removeClass("invisible");
		
				$.ajax({
					type: "POST",
					async: true,
					data: {topic: $("#topicIn").val(), max: $("#maxIn").val(), full: $("#fullIn").attr('checked')},
					dataType: "html",
					cache: false,
					success: function(result) {
						var startIndex = result.indexOf('<div id="results">');
						var endIndex = result.indexOf('</div>',startIndex)+6;
						$("#results").replaceWith(result.substring(startIndex,endIndex));
						$("#busyIcon").addClass("invisible");
					},
					error: function(result) {
						alert(result.status + " " + result.statusText);
					}
				});
			});
		   
			$.get("${it.publicBaseUri}contenthub/ldpath", function(indexes) {
				innerStr = "<select id='indexSelect' onChange='javascript:redirectIndex();'>" + "<option value='contenthub'>contenthub</option>"
				for(var index in indexes) {
					innerStr += "<option value=" + index + ">" + index + "</option>";
				}
				innerStr += "</select>";
				$("#indexDiv").html(innerStr);
				$("#indexSelect").val("${it.indexName}");
			});
		}
		$(document).ready(init);
		 
		function redirectIndex(){
			var index = $("#indexSelect").val();
			window.location.replace("${it.publicBaseUri}crawler/cnn/" + index);
		}
	</script>
</@common.page>
</#escape>