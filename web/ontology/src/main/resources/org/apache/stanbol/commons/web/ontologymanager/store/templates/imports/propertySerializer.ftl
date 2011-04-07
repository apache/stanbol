<#macro property it>
    var property = this.PSTORE.propUtil.property;
	property.domainURIs = new Array();
	
	<#list it.domain.classMetaInformationOrBuiltInResource as resource>
		<#if resource?is_hash>
			property.domainURIs.push("${resource.URI}");
		<#else>
			property.domainURIs.push("${resource}");
		</#if>
	</#list>
	property.rangeURIs = new Array();
	<#list it.range.classMetaInformationOrBuiltInResource as resource>
		<#if resource?is_hash>
			property.rangeURIs.push("${resource.URI}");
		<#else>
			property.rangeURIs.push("${resource}");
		</#if>
	</#list>
	property.isFunctional = ${it.isIsFunctional()?string};
	
	<!-- Object Property Attributes -->
	<#if it.class.simpleName== 'ObjectPropertyContext'>
		property.isTransitive = ${it.isIsTransitive()?string};
		property.isSymmetric = ${it.isIsSymmetric()?string};
		property.isInverseFunctional = ${it.isIsInverseFunctional()?string};
	</#if>
	<!--Deep copy using jQuery-->
  	jQuery.extend(true, this.PSTORE.propUtil.originalProperty, property);
</#macro>