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
<@common.page title="LD Path" hasrestapi=true>

  <h3>Recently Submitted LD Programs</h3>
  <div id="storeContents" class="storeContents">
    <div id="submittedPrograms">
      <table>
        <tr>
          <th></th>
          <th>ProgramName</th>
          <th>LD Path Program</th>
        </tr>
        <#list it.ldPrograms as ldProgram>
          <tr>
            <td><img src="${it.staticRootUrl}/contenthub/images/delete_icon_16.png" onClick="javascript:deleteProgram('${ldProgram.name}')" title="Delete this program" /></td>
            <td>${ldProgram.name}</td>
            <td>${ldProgram.ldPathProgram}</td>
          </tr>
        </#list>
      </table>
    </div>
  </div>
  
  <br/>
  
  <h3>Submit a new LD Program</h3>
  <fieldset>
    <legend>Give an LD Program Name and LD Path Program</legend>
    <p>Program Name:</p>
    <p><input type="text" id="programNameText" /></p>
    <p>LD Path Program:</p>
    <p><textarea rows="15" id="ldPathProgramTextArea" name="content"></textarea></p>
    <p><input type="submit" id="ldProgramSubmit" value="Submit Program" onClick="javascript:submitProgram();" /></p>
  </fieldset>
  
  <script language="javascript">
      function submitProgram() {
          var programName = $.trim($("#programNameText").val());
          var ldPathProgram = $.trim($("#ldPathProgramTextArea").val());
          
          if(programName == "" || ldPathProgram == "") {
              alert("You should enter an LD Program Name and LD Path Program");
              return false;
          }
          
          $.ajax({
              url: "${it.publicBaseUri}contenthub/ldpath/program",
              type: "POST",
              data: { name: programName, program: ldPathProgram },
              success: function() {
                  $("#submittedPrograms").load("${it.publicBaseUri}contenthub/ldpath #submittedPrograms>table");
                  $("#programNameText").attr("value", "");
                  $("#ldPathProgramTextArea").attr("value", "");
              },
              error: function(jqXHR, textStatus, errorThrown) {
                  alert(jqXHR.status + " " + errorThrown);
              }
          });
      }
      
      function deleteProgram(programName) {
          $.ajax({
            url: "${it.publicBaseUri}contenthub/ldpath/program/"+programName,
            type: "DELETE",
            async: true,
            success: function() {
                $("#submittedPrograms").load("${it.publicBaseUri}contenthub/ldpath #submittedPrograms>table");
            },
            error: function(result) {
                alert(result.status);
            }
          });
      }
  </script>
  
</@common.page>
</#escape>