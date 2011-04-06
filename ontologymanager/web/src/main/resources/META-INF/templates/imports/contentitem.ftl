<#import "/imports/entities.ftl" as entities>
<#macro view>

<div class="entitylistings">
<#if it.personOccurrences?size != 0 || it.organizationOccurrences?size != 0 ||  it.placeOccurrences?size != 0>
<h3>Extracted entities</h3>
</#if>

<div class="entitylisting">
<#if it.personOccurrences?size != 0>
<h3>People</h3>
<@entities.listing entities=it.personOccurrences
  iconsrc="/static/images/user_48.png" /> 
</#if>
</div>

<div class="entitylisting">
<#if it.organizationOccurrences?size != 0>
<h3>Organizations</h3>
<@entities.listing entities=it.organizationOccurrences
  iconsrc="/static/images/organization_48.png" /> 
</#if>
</div>

<div class="entitylisting">
<#if it.placeOccurrences?size != 0>
<h3>Places</h3>
<@entities.listing entities=it.placeOccurrences
  iconsrc="/static/images/compass_48.png" /> 
</#if>
</div>
</div>
<div style="clear: both"></div>

<script>
$("th").click(function () {
  $(this).parents("table").toggleClass("collapsed");
});    
</script>
</#macro>