<#import "/imports/common.ftl" as common>
<#import "/imports/integritycheckDescription.ftl" as integritycheckDescription>
<#import "/imports/outline.ftl" as outline>
<#import "/imports/steps/step1.ftl" as step1>
<#import "/imports/steps/step2.ftl" as step2>
<#import "/imports/steps/step3.ftl" as step3>
<#import "/imports/steps/step4.ftl" as step4>
<#escape x as x?html>
<@common.page title="Apache Stanbol Demos: Integrity Check" hasrestapi=false>
<@integritycheckDescription.view />
<@outline.view />
<@step1.view />
<@step2.view />
<@step3.view />
<@step4.view />
</@common.page>
</#escape>