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


The rule pattern (modus ponens) used in Stanbol is the following
<pre>     <span class="red">if</span> conditioon <span class="red">then</span> consequent</pre>

For example the statement "every person has a father", i.e. &forall;x.&exist;y. Person(x) &rArr; hasFather(x, y), becomes
<pre>     <span class="red">if</span> X is a person <span class="red">then</span> X has a father</pre>
Or "the brother of the father is the uncle", i.e. &forall;xyz. hasFather(x,y) &and; hasBrother(y,z) &rArr; hasUncle(x,z), becomes
<pre>     <span class="red">if</span> Y is the father of X and Z the brother of Y <span class="red">then</span> Z is the uncle of X</pre>

</#macro>