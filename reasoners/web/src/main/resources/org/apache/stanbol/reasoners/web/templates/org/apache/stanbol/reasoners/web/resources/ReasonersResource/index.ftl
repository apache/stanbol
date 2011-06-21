<#import "/imports/common.ftl" as common>
<#import "/imports/reasonersDescription.ftl" as reasonersDescription>
<#escape x as x?html>
<@common.page title="Apache Stanbol Reasoners" hasrestapi=false> 

<@reasonersDescription.view /> 

</@common.page>
</#escape>