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
<#import "/imports/contentitem.ftl" as contentitem>
<#escape x as x?html>
<@common.page title="Content Item: ${it.localId} (${it.contentItem.mimeType})" hasrestapi=false> 

<h3>Public URI of this resource</h3>
<pre>${it.contentItem.uri.unicodeString}</pre>

<h3>Content preview</h3>
<div class="contentItemPreview">
<#if it.textContent?exists>
<pre>${it.textContent}</pre>
</#if>
<#if it.imageSrc?exists>
<img class="preview" src="${it.imageSrc}" alt="Image content of ${it.localId}" />
</#if>
</div>

<ul class="downloadLinks">
<#if it.downloadHref?exists>
<li><a href="${it.downloadHref}" class="download">Download the original Content Item</a></li>
</#if>
<#if it.metadataHref?exists>
<li><a href="${it.metadataHref}" class="downloadRDF">Download the related RDF enhancements</a></li>
</#if>
</ul>

<@contentitem.view />

</@common.page>
</#escape>
