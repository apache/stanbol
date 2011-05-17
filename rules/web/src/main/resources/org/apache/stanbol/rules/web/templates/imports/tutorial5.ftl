<#macro view>

An datavalued assertion atom is useful to evaluate or assert facts between individuals and values and is identified by the operator

<pre>     <b>values</b>(<i>propertyPredicate</i>, <i>individualArgument</i>, <i>valueArgument</i>)</pre>

where

<ul>
<li><i>propertyPredicate</i> is the object property that has to be evaluated. It can be a constant (URI) or a variable (?x)</li>
<li><i>individualArgument</i>is the subject of the statement. It can be either constants (i.e. URI) or variables (e.g. ?x)</li>
<li><i>valueArgument</i>is the object of the statement. It can be either constants (i.e. a literal) or variables (e.g. ?x)
</li>
</ul>

</#macro>