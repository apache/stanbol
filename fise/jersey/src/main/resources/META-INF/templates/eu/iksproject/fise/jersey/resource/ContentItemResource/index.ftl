<#import "/imports/common.ftl" as common>
<#import "/imports/contentitem.ftl" as contentitem>
<#escape x as x?html>
<@common.page title="Content Item: ${it.localId} (${it.contentItem.mimeType})" hasrestapi=false> 

<h3>Public URI of this resource</h3>
<pre>${it.contentItem.id}</pre>

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
