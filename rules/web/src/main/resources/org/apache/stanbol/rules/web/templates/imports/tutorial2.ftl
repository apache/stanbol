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
<#macro view>

An atom is the smallest unit of the interpretation of a rule, e.g. the following predicate calculus formula
<pre>     Person(x) &rArr; hasFather(x, y)</pre>
has two atoms
<ul>
<li>Person(&middot;)</li>
<li>hasFather(&middot;, &middot;)</li>
</ul>

In Stanbol basic atoms are
<ul>
<li><a href="javascript:var interaction = new Interaction(); interaction.getTutorial(3);">Class assertion atom</a></li>
<li><a href="javascript:var interaction = new Interaction(); interaction.getTutorial(4);">Individual assertion atom</a></li>
<li><a href="javascript:var interaction = new Interaction(); interaction.getTutorial(5);">Data value assertion atom</a></li>
<li>Range assertion atom </li>
</ul>

The atoms may contain

<ul>
<li>constants: they consist of URI (we are in Web context) or Literal (values), e.g. e.g. &lt;http//dbpedia.org/resource/Bob_Marley&gt; is a constant, but "Bob Marley"^^xsd:string is a constant too</li>
<li>variables: any identifier preceded by ?, e.g. ?x is a variable, but also ?y is a variable</li>
</ul>

</#macro>