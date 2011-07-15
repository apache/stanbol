<#--
  Licensed to the Apache Software Foundation (ASF) under one or more
  contributor license agreements.  See the NOTICE file distributed with
  this work for additional information regarding copyright ownership.
  The ASF licenses this file to You under the Apache License, Version 2.0
  (the "License"); you may not use this file except in compliance with
  the License.  You may obtain a copy of the License at

      http://www.apache.org/licenses/LICENSE-2.0

  Unless required by applicable law or agreed to in writing, software
  distributed under the License is distributed on an "AS IS" BASIS,
  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  See the License for the specific language governing permissions and
  limitations under the License.
-->
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