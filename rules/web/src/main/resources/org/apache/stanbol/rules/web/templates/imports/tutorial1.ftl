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

in Stanbol a rule is defined using the following syntax

<pre>     <i>ruleName</i>[<span class="red">body</span> -> <span class="red">head</span>]</pre>

where
<ul>
<li>the ruleName identifies the rule</li>
<li>the body is a set of <b>atoms</b> that must be satisfied by evaluating the rule</li>
<li>the head or consequent is a set of <b>atoms</b> that must be true if the condition is evaluated to be true</li>
<li>both body and head consist of a list of conjunctive atoms 
<ul>
<li>head = atom1 . atom2 . ... . atomM</li>
<li>body = atom1 . atom2 . ... . atomN</li>
</ul>
</li>
<li>the conjunction &and; in Stanbol Rules is expressed with the symbol " . "</li>

</ul>

</#macro>