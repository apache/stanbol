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
		</fieldset>
	</form>
	
	<#if it.templateData?exists>
		<fieldset>
			<legend>Articles found for topic: ${it.templateData.topic}</legend>
			<#if it.templateData.uris?exists && it.templateData.uris?size != 0>
				<ul>
				<#list it.templateData.uris as uri>
					<li><a href="${uri}">${uri}</a></li>
				</#list>
				</ul>
			<#else>
				<p>No articles found for this topic<p>
			</#if>
		</fieldset>
	</#if>
	
	
	<script language="javascript">
	
	function registersSparqlHandler() {
	   $("#submitIn", this).click(function(e) {
	     // disable regular form click
	     e.preventDefault();
	     
	     
	     $.ajax({
	       type: "POST",
	       async: false,
	       data: {topic: $("#topicIn").val(), max: $("#maxIn").val(), full: $("#fullIn").attr('checked')},
	       dataType: "html",
	       cache: false,
	       success: function(result) {
	       	 // since post does not create any resource, there is no possibility to redirect
	         document.clear();
	         document.write(result);
	       },
	       error: function(result) {
	         alert(result.status + " " + result.statusText);
	       }
	     });
	   });
	 }
	 $(document).ready(registersSparqlHandler);
	
	</script>
</@common.page>
</#escape>