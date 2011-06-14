<#import "/imports/common.ftl" as common>
<#escape x as x?html>
<@common.page title="Active Sessions">
  

<div id="session" class="contentTag">
<ul class="indent">
<#list it.sessionsAsStringArray as session>
<li>${session}
</#list>
<ul>
</div>

</@common.page>
</#escape>
