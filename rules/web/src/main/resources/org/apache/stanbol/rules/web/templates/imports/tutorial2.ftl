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