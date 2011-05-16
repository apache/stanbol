<#macro view>


The rule pattern (modus ponens) used in Stanbol is the following
<pre>     <span class="red">if</span> conditioon <span class="red">then</span> consequent</pre>

For example the statement "every person has a father", i.e. &forall;x.&exist;y. Person(x) &rArr; hasFather(x, y), becomes
<pre>     <span class="red">if</span> X is a person <span class="red">then</span> X has a father</pre>
Or "the brother of the father is the uncle", i.e. &forall;xyz. hasFather(x,y) &and; hasBrother(y,z) &rArr; hasUncle(x,z), becomes
<pre>     <span class="red">if</span> Y is the father of X and Z the brother of Y <span class="red">then</span> Z is the uncle of X</pre>

</#macro>