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
<#macro listing entities>
<#list entities as entity>
<p>
<table class="collapsed">
<thead>
<tr>
  <th class="thumb">
    <img src="${entity.thumbnailSrc}"
      onerror="$(this).attr('src', '${entity.missingThumbnailSrc}');"
      alt="${entity.name}" />
  </th>
  <th class="name">
    <#if entity.uri?exists>
    <a href="${entity.uri}" title="${entity.summary}">${entity.name}</a>
    <#else>
    ${entity.name}
    </#if>
  </th>
</tr>
</thead>
<tbody>
<#if entity.suggestions?size != 0>
<tr class="subheader">
  <td colspan="2">Referenced entities</td>
</tr>
</#if>
<#list entity.suggestions as suggestion>
<tr>
  <td class="thumb"><img src="${suggestion.thumbnailSrc}"
    onerror="$(this).attr('src', '${suggestion.missingThumbnailSrc}');" alt="${suggestion.label}" /></td>
  <td><a href="${suggestion.uri}" title="${suggestion.summary}" class="external">${suggestion.label}</a></td>
</tr>
</#list>
<#if entity.mentions?size != 0>
<tr class="subheader">
  <td colspan="2">Mentions</td>
</tr>
</#if>
<#list entity.mentions as mention>
<tr>
  <td></td>
  <td>${mention}</td>
</tr>
</#list>
</tbody>
</table>
</p>
</#list>
</#macro>