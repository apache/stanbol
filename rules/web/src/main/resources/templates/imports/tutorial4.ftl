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

An individual assertion atom is useful to evaluate or assert facts between individuals and is identified by the operator

<pre>     <b>has</b>(<i>propertyPredicate</i>, <i>individualArgument1</i>, <i>individualArgument2</i>)</pre>

where

<ul>
<li><i>propertyPredicate</i> is the object property that has to be evaluated. It can be a constant (URI) or a variable (?x)</li>
<li><i>individualArgument1</i> and <i>individualArgument2</i> are the two arguments of the property. They can be either constants (URI) or variables (?x)
</li>
</ul>

</#macro>