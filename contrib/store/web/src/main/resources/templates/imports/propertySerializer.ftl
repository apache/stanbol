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