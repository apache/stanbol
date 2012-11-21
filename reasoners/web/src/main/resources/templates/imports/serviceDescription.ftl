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
<#import "/imports/taskDescription.ftl" as taskDescription>

<#macro view fullpath path name description>
	<p>This endpoint expose the ${name} reasoner.</p>
	<p>List of subresources:</p>
	<ul>
	<!-- TODO: generate the task list dinamically -->
	<@taskDescription.li name="Classify" path="${fullpath}/classify" description="This task infer all <tt>rdf:type</tt> statements."/>
	<@taskDescription.li name="Check" path="${fullpath}/check" description="This task checks whether the schema is correctly used."/>
	<@taskDescription.li name="Enrich" path="${fullpath}/enrich" description="This task materializes all inferences."/>
	</ul>
</#macro>

<#macro rest fullpath path name description>
	<p>This is the description of the REST api exposed by this service. <br/>Each subresource correspond to a different task:</p>
	<ul>
	<!-- TODO: generate the task list dinamically -->
	<@taskDescription.li name="Classify" path="${fullpath}/classify" description="This task infer all <tt>rdf:type</tt> statements."/>
	<@taskDescription.li name="Check" path="${fullpath}/check" description="This task checks whether the schema is correctly used."/>
	<@taskDescription.li name="Enrich" path="${fullpath}/enrich" description="This task materializes all inferences."/>
	</ul>
	<@taskDescription.view name="Classify" path="${fullpath}/classify" description="This task infer all <tt>rdf:type</tt> statements."/>
	<@taskDescription.view name="Check" path="${fullpath}/check" description="This task checks whether the schema is correctly used."/>
	<@taskDescription.view name="Enrich" path="${fullpath}/enrich" description="This task materializes all inferences."/>
</#macro>
<#macro li fullpath path name description>
	<li>${name}: @<a href="${fullpath}">${fullpath}</a> This endpoint expose the ${name} reasoner.</li>
</#macro>