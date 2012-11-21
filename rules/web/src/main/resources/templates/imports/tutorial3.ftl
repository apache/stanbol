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

A class assertion atom is identified by the operator

<pre>     <b>is</b>(<i>classPredicate</i>, <i>argument</i>)</pre>

where
<ul>
<li><i>classPredicate</i> is a URI that identified a class</li>
<li><i>argument</i> is the resource that has to be proved as typed with the classPredicate. It can be both a constant (a URI) or a variable</li>
</ul>

For example

<pre>     <span class="red">is(http://xmlns.com/foaf/0.1/Person, ?x)</span> is evaluated to be
                    <b>true</b> if the concrete value associated to ?x is typed as http://xmlns.com/foaf/0.1/Person 	
                    <b>false</b> otherwise</pre>


</#macro>