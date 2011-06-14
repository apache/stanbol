<#import "/imports/common.ftl" as common>
<#escape x as x?html>
<@common.page title="KReS Semiom Refactorer">

<p>What graph do you want to refactor</p>

<div class="contentTag">
<input type="radio" name="from" onClick="javascript:var refactorer=new Refactorer(); refactorer.showRefactoring(0, '${it.namespace}')"> Stateful refactoring<br>
<input type="radio" name="from" onClick="javascript:var refactorer=new Refactorer(); refactorer.showRefactoring(1, '${it.namespace}')"> Stateless refactoring<br>
</div>

<div id="refactoring" class="contentTagNoShow"></div> 

</@common.page>
</#escape>
