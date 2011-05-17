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