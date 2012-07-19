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
<@common.page title="Contenthub" hasrestapi=false>
<div class="panel" id="webview">
<a href="${it.publicBaseUri}contenthub/store">Store</a> | <a href="${it.publicBaseUri}contenthub/index">Index</a><b>/ldpath</b>

  <h3>Recently Submitted LD Path Semantic Indexes</h3>
  <div id="storeContents" class="storeContents">
    <div id="submittedPrograms">
      <table>
        <tr>
          <th>Name</th>
          <th>Description</th>
          <th>Index Content</th>
          <th>Batch Size</th>
          <th>Store Check Period</th>
          <th>Solr Check Time</th>
          <th>Program</th>
        </tr>
        <#list it.ldPrograms?keys as pid>
          <#assign properties = it.ldPrograms[pid]>
          <tr>
            <#list properties as property>
              <td>${property}</td>
            </#list>
          </tr>
        </#list>
      </table>
    </div>
  </div>

  <br/>
  <h3>Submit a new LD Path Semantic Index</h3>
  <fieldset>
    <legend>Give required information about LD Path Semantic Index</legend>
    <p>
      <b>Name<font color="red">*</font>: </b>
      <font size=1>(The name identifying the index)</font>
    </p>
    <p><input type="text" id="nameText" /></p>
    
    <p>
      <b>Description: </b>
      <font size=1>(Description of the index)</font>
    </p>
    <p><input type="text" id="descriptionText" /></p>

    <p>
      <b>Index Content: </b>
      <font size=1>(Set to TRUE to enable indexing content)</font>
    </p>
    <p><input type="checkbox" id="indexContentCheckBox" checked="true" /> Index Content</p>
        
    <p>
      <b>Batch Size: </b>
      <font size=1>(Maximum number of changes to be returned)</font>
    </p>
    <p><input type="text" id="batchsizeText" value="10" /></p>
    
    <p>
      <b>Store Check Period: </b>
      <font size=1>(Time to check changes in the Contenthub  Store in second units)</font>
    </p>
    <p><input type="text" id="storecheckperiodText" value="20" /></p>
    
    <p>
      <b>Solr Server Check Time: </b>
      <font size=1>(Maximum time in seconds to wait for the availability of the Solr Server)</font>
    </p>
    <p><input type="text" id="solrchecktimeText" value="5" /></p>
    
    <p>
      <b>Service Ranking: </b>
      <font size=1>(To be able to use other SemanticIndex implementations rather than this, Service Ranking property of other implementations should be set higher than of this one)</font>
    </p>
    <p><input type="text" id="rankingText" value="0" /></p>
    
    <p>
      <b>LDPath Program<font color="red">*</font>: </b>
      <font size=1>(LDPath program that will be used as a source to create the semantic index. Index fields and Solr specific configurations regarding those index fields are given in this parameter.)</font>
    </p>
    <p><textarea rows="15" id="programTextArea" name="content"></textarea></p>
    <p><input type="submit" id="ldProgramSubmit" value="Submit" onClick="javascript:submitProgram();" /></p>
  </fieldset>
  
  <div class="waitingDiv">
    <p>Stanbol is creating your index...</p>
    <p><img alt="Waiting..." src="http://localhost:8080/static/home/images/ajax-loader.gif"></p>
  </div>
</div>  
  <script language="javascript">
      function submitProgram() {
          var name = $.trim($("#nameText").val());
          var description = $.trim($("#descriptionText").val());
          var indexContent = $("#indexContentCheckBox").is(':checked');
          var batchsize = $.trim($("#batchsizeText").val());
          var storecheckperiod = $.trim($("#storecheckperiodText").val());
          var solrchecktime = $.trim($("#solrchecktimeText").val());
          var ranking = $.trim($("#rankingText").val());
          var program = $.trim($("#programTextArea").val());
      
          if(name == "" || program == "") {
              alert("You should enter an LD Program Name and LD Path Program");
              return false;
          }
          $(".waitingDiv").show();
          
          $.ajax({
              url: "${it.publicBaseUri}contenthub/index/ldpath",
              type: "POST",
              data: { name: name, description: description, indexContent: indexContent, batchsize: batchsize, storecheckperiod: storecheckperiod, solrchecktime: solrchecktime, ranking: ranking, program: program },
              success: function() {
                  $(".waitingDiv").hide();
                  $("#submittedPrograms").load("${it.publicBaseUri}contenthub/index/ldpath #submittedPrograms>table");
                  $("#nameText").attr("value", "");
                  $("#descriptionText").attr("value", "");
                  $("#indexContentCheckBox").attr("checked", true);
                  $("#batchsizeText").attr("value", "10");
                  $("#storecheckperiodText").attr("value", "20");
                  $("#solrchecktimeText").attr("value", "5");
                  $("#rankingText").attr("value", "0");
                  $("#programTextArea").attr("value", "");
              },
              error: function(jqXHR, textStatus, errorThrown) {
                  $(".waitingDiv").hide();
                  alert(jqXHR.status + " " + errorThrown);
              }
          });
      }
  </script>
  
</@common.page>
</#escape>