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