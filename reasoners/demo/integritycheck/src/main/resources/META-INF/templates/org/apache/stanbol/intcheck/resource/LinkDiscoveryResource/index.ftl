<#import "/imports/common.ftl" as common>
<#escape x as x?html>
<@common.page title="Semion: Link Discovery extension">
  <p>The Link Discovery component is based on the <a href="www4.wiwiss.fu-berlin.de/bizer/silk">Silk Framework</a></p>

<div id="silk" class="contentTag">
Provide a configuration file: <input id="confIn" type="file" name="configuration"><br><br>
<input type="submit" name="submit" value="discovery links" onClick="javascript:discoveryLinks()">
</div>

</@common.page>
</#escape>
