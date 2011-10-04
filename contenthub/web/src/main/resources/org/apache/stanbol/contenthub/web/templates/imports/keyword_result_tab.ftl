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
<div class="keywordTabs" id="${baseID}tabs">
	<legend>Results for ${qk.keyword}:</legend>
	<br/>
				<ul class="spadded">
					<li><a href="#${baseID}document_resources" onClick="javascript:showDiv();">Documents (${qk.relatedDocumentResources?size})</a></li>
					<li><a href="#${baseID}external_resources" onClick="javascript:hideDiv();">External Resources (${qk.relatedExternalResources?size})</a></li>
					<li><a href="#${baseID}class_resources" onClick="javascript:hideDiv();">Classes (${qk.relatedClassResources?size})</a></li>
					<li><a href="#${baseID}individual_resources" onClick="javascript:hideDiv();">Individuals (${qk.relatedIndividualResources?size})</a></li>
				</ul>
				<div id="${baseID}document_resources" >
					<ul class="spadded">
					<#list qk.relatedDocumentResources?sort_by("scoreString")?reverse as docRes>
						<div class="bordered-bottom">
							<li class="lined"><a href="${it.publicBaseUri}contenthub/page/${docRes.localId}">${docRes.localId}</a></li>
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
					</ul>
				</div>
				
				<div id="${baseID}external_resources">
					<ul class="spadded">
					<#list qk.relatedExternalResources?sort_by("scoreString")?reverse as extRes>
						<div class="bordered-bottom">
						<li>
							<a href="${extRes.dereferenceableURI}" target="_blank">${extRes.reference}</a>
							: ${extRes.scoreString}
							<a class="collapseItem" href="/">
								<img  src="/static/home/images/foldable_folded.png"/>
							</a>
							<div class="collapseContent">
								<fieldset>
								<legend>Types</legend>
								<ul class="spadded">
									<#list extRes.types as type>
										<li>${type}</li>
									</#list>
								<ul class="spadded">
								</fieldset>
								<fieldset>
								<legend>Mentioned In Documents</legend>
								<ul class="spadded">
									<#list extRes.relatedDocuments as doc>
										<li><a href="${doc.documentURI}">${doc.documentURI}</a></li>
									</#list>
								<ul class="spadded">
								</fieldset>
							</div>							
						</li>
						</div>
					</#list>
					</ul>
				</div>				
				
				<div id="${baseID}class_resources" >
					<ul class="spadded">
					<#list qk.relatedClassResources?sort_by("scoreString")?reverse as clsRes>
						<li><a href="${clsRes.dereferenceableURI}" target="_blank">${clsRes.classURI}</a>: ${clsRes.scoreString}</li>
					</#list>
					</ul>
				</div>
				
				<div id="${baseID}individual_resources" >
					<ul class="spadded">
					<#list qk.relatedIndividualResources?sort_by("scoreString")?reverse as indRes>
						<li><a href="${indRes.dereferenceableURI}" target="_blank">${indRes.individualURI}</a>: ${indRes.scoreString}</li>
					</#list>
					</ul>
				</div>
				
			</div>
</#macro>