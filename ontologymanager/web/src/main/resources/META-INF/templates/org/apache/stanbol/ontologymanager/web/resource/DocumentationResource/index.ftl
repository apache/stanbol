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
<#import "/imports/common.ftl" as common>
<#escape x as x?html>
<@common.page title="KReS Documentation">

<div class="contentTag">

<p>Javadoc documentation is available <a href="${it.javadoc}" alt="Javadoc">here</a></p>
<p>The delivarable about the KReS Alpha is available <a href="${it.alphaReleaseDocumentation}" alt="Javadoc">here</a></p>
<p>The delivarable about the requirements that drove the implementation of the KReS Alpha is available <a href="${it.requirementsReleaseDocumentation}" alt="Javadoc">here</a></p>
<p>The SVN repository can be browsed <a href="${it.alphaReleaseCode}" alt="Javadoc">here</a>.<br>
To checkout the code from the SVN repository you can copy and past in your terminal the following code:<br>
<div class="indent"><code>svn co ${it.alphaReleaseCode}</code></div>
</p>

</div>



</@common.page>
</#escape>
