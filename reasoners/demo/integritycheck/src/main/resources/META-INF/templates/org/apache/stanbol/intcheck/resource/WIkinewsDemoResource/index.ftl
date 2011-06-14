<#import "/imports/common.ftl" as common>
<#escape x as x?html>
<@common.page title="Integrity Check for Remote Data Fusion">

<div class="contentTag">

<p>
This demo shows an application for performing integrity check on remote data (e.g. from LOD) before storing such data on a local knowledge base to the aim of enriching it.
Performances are being improved, the system is continuously evolving.
</p>
<p>
How to use the demo:
<ol class="indent">
<li/>Paste a text in the box and click "start".<br>
<li/>The list you got back is a list of resources related to text and gathered from DBPedia and GeoNames LOD datasets
<li/>Click on "create scope" and wait that all entities are checked. It is fetching all resources and their associated semantic graph. 
It doesn't matter if some of them are red-checked, it means the server might be down.
<li/>Click "add to the ON". It means all the fetched datasets are loaded in an ontonet session where the validation will be computed.
<li/>Click on "add a new rule for the integrity check" and define your calidation rule. Use the pre-defined rule as template or go to 
<a href="http://stlab.istc.cnr.it/stlab/KReS/KReSRuleLanguage">RuleSyntax page</a> if you want more details on the syntax
<li/>Check that your rule is correct then click on "check integrity".
A reasoner is run, it will allow also to gather all sameAs entities.
<li/>The resources you got back are those ones that passed the integrity check based on your validity rule.
</ol>
</p>
<textarea id="textInput" name="textInput" cols=70 rows=20>Sony Corporation is the electronics business unit and the parent company of the Sony Group, which is engaged in business through its eight operating segments. Consumer Products & Devices (CPD), Networked Products & Services (NPS), B2B & Disc Manufacturing (B2B & Disc), Pictures, Music, Financial Services, Sony Ericsson and All Other.These make Sony one of the most comprehensive entertainment companies in the world. Sony's principal business operations include Sony Corporation (Sony Electronics in the U.S.), Sony Pictures Entertainment, Sony Computer Entertainment, Sony Music Entertainment, Sony Ericsson, and Sony Financial. As a semiconductor maker, Sony is among the Worldwide Top 20 Semiconductor Sales Leaders.</textarea>
<br><br>
<input type="button" id="submit" value="start" onClick="javascript:var demo=new Demo(); demo.enhance();">
<input type="button" value="reset" onclick="window.location.href='/intcheck/demo';"/>
<div id="fiseResult" class="hide"></div>
</div>

</@common.page>
</#escape>
