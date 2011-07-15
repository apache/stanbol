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
<@common.page title="About KReS...">

<p>The Knowledge Representation and Reasoning System (or short KReS) is a standalone set of software components 
targeted at the realization of the vast majority of functionalities and requirements belonging to the IKS knowledge 
management tier, as per IKS Deliverable D3.2 . The rationale behind it is to provide a controlled environment for 
CMS developers and administrators to execute KR, rule management and reasoning tasks for managing their content. 
Here, the notion of "controlled environment" indicates the ability to provide each user with all and only the knowledge 
and semantic services required within their business setting, despite the fact that KReS as a whole is always able to 
deliver additional services on demand.</p>

<p>The knowledge representation and reasoning capabilities of KReS span from loading, querying, storing and applying 
OWL ontologies and SWRL rule sets to executing rules and applying simple and composite Description Logic reasoning, 
along with reengineering and alignment of Linked Data and other models (such as DOM trees and relational databases) 
with the content stored by a CMS.</p>

<p>The initial KReS alpha provides both Java and RESTful APIs for the most basic functionalities that allow for some 
knowledge representation and reasoning capabilities along with an early, strictly closed-compartment-like implementation 
of the controlled environment management of ontologies and rules.</p>

</@common.page>
</#escape>
