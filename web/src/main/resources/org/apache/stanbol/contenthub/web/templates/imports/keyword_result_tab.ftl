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

<#macro keywordTab kw>
	<#assign qk=kw/>
	<#assign baseID="kw_"+qk.keyword?replace("*","_")?replace(" ", "_")?replace("'", "_")?replace("~", "_")?replace(".", "_")+"_"/>
	<legend><h3>Results for ${qk.keyword}:</h3></legend>
	<div>
		<ul class="spadded">
		<#if qk.relatedDocumentResources?size == 0>
			Your search did not match any documents
		<#else>	
			<#list qk.relatedDocumentResources?sort_by("scoreString")?reverse as docRes>
				<div class="bordered-bottom">
					<li class="lined"><a href="${it.publicBaseUri}contenthub/page/${docRes.localId}">${docRes.documentTitle}</a></li>
					<a class="collapseItem lined" href="/">
						<img  src="/static/home/images/foldable_folded.png"/>
					</a>
					<div class="collapseContent">
						<textarea readonly="readonly">${docRes.relatedText}</textarea>
						<#if docRes.relatedContentRepositoryItem?has_content>
							<p>Generated from CMS document at:${docRes.relatedContentRepositoryItem}</p>
						</#if>
					</div>
				</div>	
			</#list>
		</#if>
		</ul>
	</div>
</#macro>