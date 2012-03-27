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
<@common.page title="Search" hasrestapi=false>
  <div id="search">
  <#-- this feildset was normally in a form, ajax is used to do the post, dont need to use fom -->
    <fieldset>
      <legend>Keyword Based Search</legend>
      
      <p>
        Select an index: <div id="indexDiv"><#--this div will be populated by ajax--></div>
      </p>
      <p>
        Keywords: <input id="keywordIn" class="autoCompleteText" <#-- onkeyup="javascript:completePattern();" --> name="topic" type="text" onkeydown="if (event.keyCode == 13) document.getElementById('submitIn').click()"/><br/>
      </p>
      <p>
        <!-- Ontology selection combobox-->
        <#if it.ontologies?exists && it.ontologies?size != 0>
          Graph: 
          <select  id="graphIn" name="max" type="text" value="5">
            <option value="choose_ontology">Choose an ontology</option>
            <#list it.ontologies as ont>
              <option value="${ont}">${ont}</option>
            </#list>
          </select>
        <#else>
          <p><i>No graphs to search.<i><p> 
        </#if>
      </p>
      <p>
        <input id="submitIn" type="button" value="Search" onclick="getResults(null,null,'first');"></input>
      </p>
      <img id="busyIcon" class="invisible centerImage" src="${it.staticRootUrl}/contenthub/images/ajax-loader.gif"/>
    </fieldset>
  </div>  
  
  <div id="resultContainer" class="invisible">
    <div class="invisible" id="previousSuggestionButton"></div>
    <div>
      <!-- To be populated with ajax without xml :)-->
    </div>
  </div>
  
  <!-- to be populated by the list of suggested keywords to be able to get back in search -->
  <input type="hidden" id="suggestedKeywordList" value=""/>  
  <!-- FIXME put a textarea so jQuery-ui tabs does not expand through footer -->
  <textarea type="text" disabled="true" style="border-color: #fff; background: white; height:100px; max-height:100px; width:100%; max-width:100%"></textarea>
  
  <script language="javascript">
  
      function init() {
        
          //accordion
          $(".keywords").accordion({collapsible: true});
          //if a GET gets a parameters, then does the search with that parameter
          var keywords = "${it.queryTerm}";
          if(keywords != null && keywords.length != 0) {
              $("#keywordIn").val(keywords);
              getResults(null,null,'first');
          }
          
          $.get("${it.publicBaseUri}contenthub/ldpath", function(indexes) {
              innerStr = "<select id='indexSelect' onChange='javascript:redirectIndex();'>" + "<option value='contenthub'>contenthub</option>"
              for(var index in indexes) {
                  innerStr += "<option value=" + index + ">" + index + "</option>";
              }
              innerStr += "</select>";
              $("#indexDiv").html(innerStr);
              $("#indexSelect").val("${it.indexName}");
          });
      }
    
      $(document).ready(init);
   
      function hideDiv() {
          $('#facets').hide("slow");
      }
      
      function showDiv() {
          $('#facets').show("slow");
      }
    
      function redirectIndex() {
          var index = $("#indexSelect").val();
          window.location.replace("${it.publicBaseUri}contenthub/" + index + "/search/featured");
      }
    
      function completePattern() {
          var pattern = $("#keywordIn").val();
          $.get("${it.publicBaseUri}contenthub/${it.indexName}/search/related/autocomplete?pattern="+pattern, function(data) {
              var jsonSource = JSON.parse(data);
              $(".autoCompleteText").autocomplete({
                  source: jsonSource['completedKeywords']
              });         
          });
      }

      function getResults(facetName,facetValue,operation,voffset,vpageSize) {
        
          //clears the content of div because it'll be filled by explorer posts
          var keywordToSearch = $("#keywordIn").val();
          
          jsonCons = $("#chosenFacetsHidden").attr("value");
          if(typeof(jsonCons) == "undefined") {
              jsonCons = "{}";
          }
        
          jsonSug = $("#suggestedKeywordList").attr("value");
          if(jsonSug != "undefined" && jsonSug == "") {
              var suggestedList = JSON.parse('{"keywords" : []}');
              suggestedList['keywords'].push($("#keywordIn").val());
              $("#suggestedKeywordList").val(JSON.stringify(suggestedList));
          }
       
          var JSONObject = JSON.parse(jsonCons);
          if(typeof(facetName) != "undefined" || typeof(facetValue != "undefined")) {
              
              if(operation == "addFacet") {
                  if(JSONObject[facetName] != null) {
                      if(JSONObject[facetName].indexOf(facetValue) == -1) {
                          JSONObject[facetName].push(facetValue);
                      }
                  } else {
                      JSONObject[facetName] = new Array();
                      JSONObject[facetName].push(facetValue);
                  }
              }
        
              else if(operation == "deleteFacet") {
          
                  facetName = decodeURIComponent(facetName);
                  facetValue = decodeURIComponent(facetValue);
                  var  values = JSONObject[facetName];  
                  var length=0;
                  var index;
                  for(var value in values) {      
                      if(typeof(JSONObject[facetName][value]) != "undefined") {
                          length++;
                          if(JSONObject[facetName][value] == facetValue) {
                              index = value;
                          }
                      }
                  }     
          
                  if(length == 1) {
                      delete JSONObject[facetName];
                  } else {
                      delete JSONObject[facetName][index];
                  }
              }
          }
          if(operation == "explore") {
            
              $("#keywordIn").val(facetValue);
              var suggestedList = JSON.parse($("#suggestedKeywordList").val());
            
              var previousString = "";
              for(i = 0; i  < suggestedList['keywords'].length ; i++) {
                  if(i != 0) {
                      previousString += " > ";
                  }
                  previousString += "<a href=javascript:getResults(null,'" + suggestedList['keywords'][i] + "','previousSuggestion')> " + suggestedList['keywords'][i] + "</a>";
              }
              $("#previousSuggestionButton").html(previousString);
              //adds the last entered word to list and saves it in a hidden division
              suggestedList['keywords'].push(facetValue);
            
              //decides when to show back division
              if(suggestedList['keywords'].length > 1) {
                  $("#previousSuggestionButton").removeClass('invisible');
              }
              else{
                  $("#previousSuggestionButton").addClass('invisible');
              }
              $("#suggestedKeywordList").val(JSON.stringify(suggestedList));
          }
      
          //if back button is pressed, previous suggestion is searched again and suggestionList is fixed
          else if(operation == "previousSuggestion") {
              var suggestedList = JSON.parse($("#suggestedKeywordList").val());
              var length = suggestedList['keywords'].length;
              var index = suggestedList['keywords'].indexOf(facetValue);
              
              suggestedList['keywords'] = suggestedList['keywords'].slice(0,index);       
              
              $("#suggestedKeywordList").val(JSON.stringify(suggestedList));
              getResults(null,facetValue,"explore");
          }
          else if(operation == "date") {
          
              var JSONObject = JSON.parse(jsonCons);
              var facetValue = "[" + document.getElementById("dateFrom").value + "T00:00:00Z TO " + 
                          document.getElementById("dateTo").value + "T23:59:59Z]";
              JSONObject[facetName] = new Array();
              JSONObject[facetName].push(facetValue);
          }
          else if(operation == "range") {
              var facetHtmlName = getHtmlName(facetName);
              var JSONObject = JSON.parse(jsonCons);
              var facetValue = "[" + document.getElementById(facetHtmlName+"TextMin").value + " TO " + 
                                     document.getElementById(facetHtmlName+"TextMax").value + "]";
                        
              JSONObject[facetName] = new Array();
              JSONObject[facetName].push(facetValue);
          }
      
          //make text area invisible
          $("#searchResult").fadeOut(100);
          $("#resultContainer").fadeOut(100);
          //show busy icon
          $("#busyIcon").removeClass("invisible");

          var graph_selected = "";
          var graphInCombo = document.getElementById('graphIn');
          if(graphInCombo != null) {
              var selectedIndex = graphInCombo.selectedIndex;
              if(selectedIndex != 0) {
                  graph_selected = $("#graphIn option:selected").val();
              }
          }
      
          $.ajax({
              url : "${it.publicBaseUri}contenthub/${it.indexName}/search/featured",
              type : "GET",
              async: true,
              data: {queryTerm: $("#keywordIn").val(), graph: graph_selected, constraints: JSON.stringify(JSONObject), offset: voffset, limit:vpageSize},
              dataType: "html",
              cache: false,
              success: function(result) {
                  $("#busyIcon").addClass("invisible");
                  $("#search").addClass("invisible");
                    
                  $("#resultContainer > div:nth-child(2)").html(result);
                  $(".keywords").accordion({collapsible: true, autoHeight: false });
                  $(".keywords").removeClass("ui-widget");
                  $(".resources > div").tabs({fx: { height: 'toggle', opacity: 'toggle' } });
                  $("#resultContainer").fadeIn("slow");
                   
                  //collapsible content
                  $(".collapseItem").click(function(e) {
                      e.preventDefault();
                      $(this).next(".collapseContent").slideToggle(500);
                  }); 
                  
                  setChosenFacet(JSONObject);
              },
              error: function(result) {
                  $("#busyIcon").addClass("invisible");
                  alert(result.status + ' ' + result.statusText);
              }
          });
      }
      
      function setChosenFacet(JSONObject) {
          var resultString = "";
          var chosenCons = $("#chosenFacetsHidden").attr("value");
              
          if(JSONObject != null) {
              for(var p in JSONObject) {
                  if(JSONObject.hasOwnProperty(p)) {
                      for(var value in p) {
                          if(p.hasOwnProperty(value) && typeof(JSONObject[p][value]) != "undefined") {
                              var escapedFacetName = escape(encodeURIComponent(p.toString()));
                              var escapedFacetValue = escape(encodeURIComponent(JSONObject[p][value]));
                              chosenCons = escape(chosenCons);
                              var facetHtmlName = getHtmlName(p.toString());
                              var href = "<a href=javascript:getResults(\""; 
                              href +=   escapedFacetName + "\",\"" + escapedFacetValue + "\",\"deleteFacet\") title='Remove'>";
                              href +=     "<img src='${it.staticRootUrl}/contenthub/images/delete_icon_16.png'></a>";
                              href +=   facetHtmlName + " : " + 
                                    ((isReserved(p)) ? JSONObject[p][value].substring(1,11)+" to "+JSONObject[p][value].substring(25,35) : 
                                    JSONObject[p][value]) + "<br/>";
                              resultString += href;
                          }
                      }
                  }
              }
          }
          
          var a = document.getElementById('chosenFacets');
          if(a != null) {
              a.innerHTML = resultString;
          }
      }
        
      function isReserved(str) {
          return str.indexOf("stanbolreserved_") == 0; 
      }
    
      function getHtmlName(name) {
          if(isReserved(name)) {
              return name.substring(name.indexOf("_")+1);
          }
          lastUnderscore = name.lastIndexOf("_");
          if (lastUnderscore >= 0) {
              underScoreExtension = name.substring(lastUnderscore);
              if (underScoreExtension == "_t" || underScoreExtension == "_l" || underScoreExtension == "_d" || underScoreExtension == "_dt") {
                  return name.substring(0, lastUnderscore);
              }
          }
          return name;
      }
      
  </script>
</@common.page>
</#escape>
