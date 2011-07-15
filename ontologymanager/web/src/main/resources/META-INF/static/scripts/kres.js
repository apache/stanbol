/*
* Licensed to the Apache Software Foundation (ASF) under one or more
* contributor license agreements.  See the NOTICE file distributed with
* this work for additional information regarding copyright ownership.
* The ASF licenses this file to You under the Apache License, Version 2.0
* (the "License"); you may not use this file except in compliance with
* the License.  You may obtain a copy of the License at
*
*     http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/
function showReengineer(dataSourceType){
	var dataSourceDiv = document.getElementById("data-source-form");
	if(dataSourceDiv != null){
		if(dataSourceType == 1){
			dataSourceDiv.innerHTML = "<form id=\"kres\" action=\"/kres/reengineer/db/schema\" method=\"POST\" enctype=\"application/x-www-form-urlencoded\" " +
					"accept=\"application/rdf+xml\"> " +
					"Base URI for schema<br/><input type=\"text\" class=\"url\" name=\"output-graph\" value=\"\" /><br/> " +
					"Physical DB Name<br/><input type=\"text\" class=\"url\" name=\"db\" value=\"\" /><br/> " +
					"JDBC Driver<br/><input type=\"text\" class=\"url\" name=\"jdbc\" value=\"\" /><br/> " +
					"Protocol<br/><input type=\"text\" class=\"url\" name=\"protocol\" value=\"\" /><br/> " +
					"Host<br/><input type=\"text\" class=\"url\" name=\"host\" value=\"\" /><br/> " +
					"Port<br/><input type=\"text\" class=\"url\" name=\"port\" value=\"\" /><br/> " +
					"Username<br/><input type=\"text\" class=\"url\" name=\"username\" value=\"\" /><br/> " +
					"Password<br/><input type=\"text\" class=\"url\" name=\"password\" value=\"\" /><br/> " +
					"<br/><br/> " +
					"<input type=\"submit\" class=\"submit\" value=\"RDB Reengineering\"/>" +
					"</form>";
		}
		else if(dataSourceType == 2){
			dataSourceDiv.innerHTML = "<form id=\"kres\" action=\"/kres/reengineer\" method=\"POST\" " +
					"enctype=\"multipart/form-data\" " +
					"accept=\"application/rdf+xml\"> " +
					"Output graph URI<br/><input type=\"text\" class=\"url\" name=\"output-graph\" value=\"\" /><br/> " +
					"Input type<br/><select name=\"input-type\"><option value=\"xml\">XML</select><br/> " +
					"Input<br/><input type=\"file\" class=\"url\" name=\"input\" /><br/><br/> " +
					"<input type=\"submit\" class=\"submit\" value=\"XML Reengineering\"/>" +
					"</form>";
		}
		else{
			dataSourceDiv.innerHTML = "";
		}
	}
	
	
}


function getXMLHttpRequest() {

	
	var XHR = null,

	userAgent = navigator.userAgent.toUpperCase();

	if(typeof(XMLHttpRequest) === "function" || typeof(XMLHttpRequest) === "object")
		XHR = new XMLHttpRequest();

	else if(window.ActiveXObject && userAgent.indexOf("MSIE 4") < 0) {

		if(userAgent.indexOf("MSIE 5") < 0)
			XHR = new ActiveXObject("Msxml2.XMLHTTP");
		else
			XHR = new ActiveXObject("Microsoft.XMLHTTP");
	}

	return XHR;
}


function expandMenu(requester) {
	var semionMenu = document.getElementById(requester);
	var display = semionMenu.style.display;
	if(display == '' || display == 'none' || display == null){
		semionMenu.style.display = 'block';
	}
	else{
		semionMenu.style.display = 'none';
	}
	
}

function listScopes(withInactive){
	var ajax = getXMLHttpRequest();
	if(ajax != null){
		if(withInactive){
			ajax.open("get", "/kres/ontology?with-inactive=true", true);
		}
		else{
			ajax.open("get", "/kres/ontology", true);
		}
		ajax.setRequestHeader("Accept", "application/rdf+json");
		
		var contentDIV = document.getElementById("content");
		
		ajax.onreadystatechange = function() {

		   	// se le operazioni sono state effettuate
		    if(ajax.readyState == 4) {
		    	if (ajax.status == 200) {
		    		if(contentDIV != null){
		    			var jsonObj = ajax.responseText;
		    			
		    			var databank = $.rdf.databank().load(JSON.parse(jsonObj));
		    			databank.prefix('onm', 'http://kres.iks-project.eu/ontology/onm/meta.owl#');
		    			databank.prefix('rdf', 'http://www.w3.org/1999/02/22-rdf-syntax-ns#');
		    			databank.prefix('owl', 'http://www.w3.org/2002/07/owl#');
		    			
		    			var rdf = $.rdf({databank:databank});
		    			
		    			
		    			var scopes = rdf.prefix('onm', 'http://kres.iks-project.eu/ontology/onm/meta.owl#')
		    							.where('?scope a onm:Scope')
		    							.select();
		    			
		    			
		    			var text = "<h3 class=\"menuLeft\">Ontology Network scopes</h3>" +
		    						"<a class=\"menuRight\" href=\"javascript:addScope(true)\"><img alt=\"Add a scope\" src=\"/kres/static/images/add.gif\"</a>" +
		    						"<div><ul id=\"kresScopes\" class=\"kressList\">";
		    			
		    			text += "<br><br>";
		    			for(var scope in scopes){
		    				var scopeID = scopes[scope].scope.toString().replace('<', '').replace('>', '');
		    				text += "<li>"+scopeID +
		    						"<a href=\"javascript:showScopeConfigurationOptions('"+scopeID+"')\"><img class=\"configure\" alt=\"Configure\" src=\"/kres/static/images/configure.gif\"></a>" +
		    						"<div id=\""+scopeID+"\" class=\"scopeDIV\">" +
		    							"<a href=\"javascript:deleteScope("+scopeID+")\"><img src=\"/kres/static/images/delete.gif\" alt=\"delete scope\"></a>" +
		    						"</div>";
		    			}
		    			text += "</ul></div>";
		    			if(withInactive){
		    				text += "<input class=\"contentAlign\" type=\"checkbox\" onClick=\"javascript:listScopes(false)\" CHECKED>Show also disabled scopes"
		    			}
		    			else{
		    				text += "<input class=\"contentAlign\" type=\"checkbox\" onClick=\"javascript:listScopes(true)\">Show also disabled scopes"
		    			}
		    			
		    			contentDIV.innerHTML = text;
		    		}
		        }
		        
		    }
	    }
		
		contentDIV.innerHTML = "<img src=\"/kres/static/images/loading.gif\">";
		
		ajax.send(null);
	}
}


function showScopeConfigurationOptions(scopeID) {
	var scopeDIV = document.getElementById(scopeID);
	if(scopeDIV != null){
		var d = scopeDIV.style.display;
		if(d==null || d=='none' || d==''){
			scopeDIV.style.display = 'block';
		}
		else{
			scopeDIV.style.display = 'none';
		}
	}
}

function addScope(displayModule) {
	if(displayModule){
		var content2 = "<div id=\"popupbox\">" +
						"<form name=\"login\" action=\"\" method=\"post\">" +
						"Scope ID:" +
						"<center><input id=\"scopeid\" name=\"scopeid\" type=\"text\" size=\"34\" /></center>" +
						"Core registry:" +
						"<center><input id=\"corereg\" name=\"corereg\" type=\"text\" size=\"34\" /></center>" +
						"Core ontology:" +
						"<center><input id=\"coreont\" name=\"coreont\" type=\"text\" size=\"34\" /></center>" +
						"Custom registry:" +
						"<center><input id=\"customreg\" name=\"customreg\" type=\"text\" size=\"34\" /></center>" +
						"Custom ontology:" +
						"<center><input id=\"customont\" name=\"customont\" type=\"text\" size=\"34\" /></center>" +
						"Activate scope:" +
						"<center><input id=\"activate\" name=\"activate\" type=\"checkbox\" /></center>" +
						"<center><input type=\"button\" name=\"submit\" value=\"add\" onclick=\"javascript:addScope()\"/></center></form><br />";

		TINY.box.show(content2,0,0,0,1);
	}
	else{
		var scopeID = document.getElementById("scopeid").value;
		var corereg = document.getElementById("corereg").value;
		var coreont = document.getElementById("coreont").value;
		var customreg = document.getElementById("customreg").value;
		var customont = document.getElementById("customont").value;
		var activate = document.getElementById("activate");
		
		var ajax = getXMLHttpRequest();
		if(ajax != null){
			ajax.onreadystatechange = function() {

			   	if(ajax.readyState == 4) {
			    	if (ajax.status == 200) {
			    		TINY.box.hide();
			    		var kresScopes = document.getElementById("kresScopes");
			    		if(kresScopes != null){
			    			var txt = document.createTextNode(scopeid);
			    			
			    			var newScope = document.createElement("li");
			    			newScope.appendChild(txt);
			    		}
			    	}
			   	}
			}
			
			var parameters = "";
			
			if(corereg != ''){
				parameters += "corereg="+corereg;
			}
			if(coreont != ''){
				if(parameters != ''){
					parameters += "&";
				}
				parameters += "coreont="+coreont;
			}
			if(customreg != ''){
				if(parameters != ''){
					parameters += "&";
				}
				parameters += "customreg="+customreg;
			}
			if(customont != ''){
				if(parameters != ''){
					parameters += "&";
				}
				parameters += "customont="+customont;
			}
			
			if(parameters != ''){
				parameters += "&";
			}
			if(activate.checked){
				parameters += "activate=true";
			}
			else{
				parameters += "activate=false";
			}
			ajax.open("put", "/kres/ontology/"+scopeID+"?"+parameters, true);
			//alert(parameters);
			ajax.send(null);
		}
	}
}

function discoveryLinks(){
	var confIn = document.getElementById("confIn")
	
	if(confIn != null){
		var ajax = getXMLHttpRequest();
		
		if(ajax != null){
			
			ajax.open("post", "/kres/link-discovery", true);
			
			var parameter = "configuration="+confIn.value;
			
			ajax.onreadystatechange = function() {
				if(ajax.readyState == 4) {
			    	if (ajax.status == 200) {
			    	}
				}
			}
		}
		
		ajax.send(parameter);
	}
}




function getGraphs(id, ns){
	var content = "";
	
	var ajax = getXMLHttpRequest();
	if(ajax != null){
		ajax.open("get", "/kres/graphs/resume", false);
		ajax.setRequestHeader("Accept", "application/rdf+json");
		ajax.send(null);
		
		content += "Select a graph from the store: <br>";
		content += "<select id=\""+id+"\" class=\"refactor\" name=\""+id+"\">";
		
		var jsonObj = ajax.responseText;
		
		var databank = $.rdf.databank().load(JSON.parse(jsonObj));
		
		var rdf = $.rdf({databank:databank});
		
		
		var graphs = rdf.prefix('kres', ns)
						.where('kres:Storage kres:hasGraph ?graph')
						.select();
		
		for(var graph in graphs){
			var graphURI = graphs[graph].graph.toString();
			var g = graphURI.replace("<", "").replace(">", "");
			
			content += "<option value='"+ g +"'>"+ g;
		}
		
		content += "</select>";
	}
	
	return content;
}


function getRecipies(){
	var content = "";
	
	var ajax = getXMLHttpRequest();
	if(ajax != null){
		ajax.open("get", "/kres/recipe/all", false);
		ajax.setRequestHeader("Accept", "application/rdf+json");
		ajax.send(null);
		
		content += "<br>Select a recipe from the rule store: <br>";
		content += "<select id=\"recipe\" class=\"refactor\" name=\"recipe\">";
		
		var jsonObj = ajax.responseText;
		
		var databank = $.rdf.databank().load(JSON.parse(jsonObj));
		
		var rdf = $.rdf({databank:databank});
		
		
		var recipes = rdf.prefix('rmi', 'http://kres.iks-project.eu/ontology/meta/rmi.owl#')
						.where('?recipe a rmi:Recipe')
						.select();
		
		for(var recipe in recipes){
			var recipeURI = recipes[recipe].recipe.toString();
			var r = recipeURI.replace("<", "").replace(">", "");
			content += "<option value='"+ r +"'>"+ r;
		}
		
		content += "</select>";
	}
	
	return content;
}


/*
 * Refactoring
 */

function runRefactoringStore(graph, recipe){
	alert("refactoring store");
}

function listRecipes(){
	var recipeListDIV = document.getElementById("recipeList");
	if(recipeListDIV != null){
		
		
		
			
		var ajax = getXMLHttpRequest();
		if(ajax != null){
			ajax.open("get", "/kres/recipe/all", true);
			ajax.setRequestHeader("Accept", "application/rdf+json");
			
			ajax.onreadystatechange = function() {
				if(ajax.readyState == 4) {
			    	if (ajax.status == 200) {
			    		
			    		
		    			var content = "<ul class=\"kressList\">";
		    			
		    			var jsonObj = ajax.responseText;
		    			
		    			var databank = $.rdf.databank().load(JSON.parse(jsonObj));
		    			
		    			var rdf = $.rdf({databank:databank});
		    			
		    			
		    			var recipes = rdf.prefix('rmi', 'http://kres.iks-project.eu/ontology/meta/rmi.owl#')
										 .where('?recipe a rmi:Recipe')
										 .select();
		
		    			content += "<br><br>";
		    			for(var recipe in recipes){
		    				var recipeURI = recipes[recipe].recipe.toString();
		    				var r = recipeURI.replace("<", "").replace(">", "");
		    				content += "<li>"+ r;
		    				content += "<a href=\"javascript:showScopeConfigurationOptions('"+r+"')\"><img class=\"configure\" alt=\"Configure\" src=\"/kres/static/images/configure.gif\"></a>";
		    				content += "<div id=\""+r+"\" class=\"scopeDIV\">" +
		    								"<a href=\"javascript:deleteRecipe("+r+")\" alt=\"delete scope\">" +
		    										"<img src=\"/kres/static/images/delete.gif\" alt=\"delete scope\"></a>" +
		    								"<a href=\"javascript:var rule = new Rule(); rule.displayAddBox('"+r+"');\" alt=\"add rule to scope\">" +
		    										"<img src=\"/kres/static/images/addRule.gif\" alt=\"add rule to scope\"></a>" +
		    								"<a id=\"listRulesA\"href=\"javascript:var rule = new Rule(); rule.listRulesOfRecipe('"+r+"');\" alt=\"list rules of recipe"+r+"\">" +
		    										"<img src=\"/kres/static/images/rules.gif\" alt=\"list rules of recipe"+r+"\"></a>" +
		    							"</div>";
		    			}
		    			
		    			
		    			content += "</ul>";
		    			
		    			
		    			recipeListDIV.innerHTML = content;
		    		
			    	}
			   	}
			}
			
			ajax.send(null);
		}
		
		recipeListDIV.style.display = 'block';
		
		var addRecipeElement = document.getElementById("addRecipe");
		if(addRecipeElement != null){
			addRecipeElement.style.display = 'block';
		}
		
		var action = document.getElementById("action");
		if(action != null){
			action.href = "javascript:hideRecipes()";
			action.innerHTML = "hide";
		}
	}
}

function hideRecipes(){
	var recipeListDIV = document.getElementById("recipeList");
	if(recipeListDIV != null){
		recipeListDIV.style.display = 'none';
	}
	
	var addRecipeElement = document.getElementById("addRecipe");
	if(addRecipeElement != null){
		addRecipeElement.style.display = 'none';
	}
	
	var action = document.getElementById("action");
	if(action != null){
		action.href = "javascript:listRecipes()";
		action.innerHTML = "view";
	}
}

function Recipe(){
	return this;
}

Recipe.prototype.addRecipe = function(){
	
	var recipeIDEl = document.getElementById("recipeid");
	var descriptionEl =	document.getElementById("description");
	
	var ajax = getXMLHttpRequest();
	if(ajax != null){
		ajax.open("post", "/kres/recipe", true);
		ajax.setRequestHeader("Content-type", "application/x-www-form-urlencoded")
		ajax.setRequestHeader("Accept", "application/rdf+json");
		
		ajax.onreadystatechange = function() {
			if(ajax.readyState == 4) {
		    	if (ajax.status == 200) {
		    		TINY.box.hide();
		    		listRecipes();
		    	}
			}
		}
	}
	
	if(recipeIDEl != null && descriptionEl != null){
		var recipeID = recipeIDEl.value;
		
		var description = descriptionEl.value;
		
		var parameter = "recipe="+recipeID+"&description="+description;
		
		ajax.send(parameter);
	}
	
	
}

Recipe.prototype.displayAddBox = function(){
	var content2 = "<div id=\"popupbox\">" +
	"<form name=\"login\" action=\"\" method=\"post\">" +
	"Recipe ID:" +
	"<center><input id=\"recipeid\" name=\"recipeid\" type=\"text\" size=\"34\" /></center>" +
	"Recipe description:" +
	"<center><input id=\"description\" name=\"description\" type=\"text\" size=\"34\" /></center>" +
	"<center><input type=\"button\" name=\"submit\" value=\"add\" onclick=\"javascript:var recipe = new Recipe(); recipe.addRecipe()\"/></center></form><br />";

	TINY.box.show(content2,0,0,0,1);
}

function loadGraph(element, json) {
	var ht = new $jit.Hypertree({  
		  //id of the visualization container  
		  injectInto: element,  
		  //canvas width and height  
		  width: 50,  
		  height: 50,  
		  //Change node and edge styles such as  
		  //color, width and dimensions.  
		  Node: {  
		      dim: 9,  
		      color: "#f00"  
		  },  
		  Edge: {  
		      lineWidth: 2,  
		      color: "#088"  
		  },  
		  onBeforeCompute: function(node){  
		      Log.write("centering");  
		  },  
		  //Attach event handlers and add text to the  
		  //labels. This method is only triggered on label  
		  //creation  
		  onCreateLabel: function(domElement, node){  
		      domElement.innerHTML = node.name;  
		      $jit.util.addEvent(domElement, 'click', function () {  
		          ht.onClick(node.id);  
		      });  
		  },  
		  //Change node styles when labels are placed  
		  //or moved.  
		  onPlaceLabel: function(domElement, node){  
		      var style = domElement.style;  
		      style.display = '';  
		      style.cursor = 'pointer';  
		      if (node._depth <= 1) {  
		          style.fontSize = "0.8em";  
		          style.color = "#ddd";  
		  
		      } else if(node._depth == 2){  
		          style.fontSize = "0.7em";  
		          style.color = "#555";  
		  
		      } else {  
		          style.display = 'none';  
		      }  
		  
		      var left = parseInt(style.left);  
		      var w = domElement.offsetWidth;  
		      style.left = (left - w / 2) + 'px';  
		  },  
		    
		  onAfterCompute: function(){  
		      Log.write("done");  
		        
		      //Build the right column relations list.  
		      //This is done by collecting the information (stored in the data property)   
		      //for all the nodes adjacent to the centered node.  
		      var node = ht.graph.getClosestNodeToOrigin("current");  
		      var html = "<h4>" + node.name + "</h4><b>Connections:</b>";  
		      html += "<ul>";  
		      node.eachAdjacency(function(adj){  
		          var child = adj.nodeTo;  
		          if (child.data) {  
		              var rel = (child.data.band == node.name) ? child.data.relation : node.data.relation;  
		              html += "<li>" + child.name + " " + "<div class=\"relation\">(relation: " + rel + ")</div></li>";  
		          }  
		      });  
		      html += "</ul>";  
		      $jit.id(element+"-details").innerHTML = html;  
		  }  
		});  
		//load JSON data.  
		ht.loadJSON(json);  
		//compute positions and plot.  
		ht.refresh();
}

function Rule(){
	return this;
}

Rule.prototype.addRule = function(){
	var recipeIDEl = document.getElementById("recipeid");
	var ruleIDEl = document.getElementById("ruleid");
	var ruleEl = document.getElementById("rule");
	var descriptionEl =	document.getElementById("description");
	
	var ajax = getXMLHttpRequest();
	if(ajax != null){
		ajax.open("post", "/kres/rule", true);
		ajax.setRequestHeader("Content-type", "application/x-www-form-urlencoded")
		
		ajax.onreadystatechange = function() {
			if(ajax.readyState == 4) {
		    	if (ajax.status == 200) {
		    		TINY.box.hide();
		    		listRecipes();
		    	}
			}
		}
	}
	
	if(recipeIDEl != null && ruleIDEl != null && ruleEl != null && descriptionEl != null){
		var recipeID = recipeIDEl.value;
		var ruleID = ruleIDEl.value;
		var rule = ruleEl.value;
		var description = descriptionEl.value;
		
		var parameter = "recipe="+recipeID+"&rule="+ruleID+"&kres-syntax="+rule+"&description="+description;
		
		ajax.send(parameter);
	}
}

Rule.prototype.displayAddBox = function(recipe){
	var content2 = "<div id=\"popupbox\">" +
	"<form name=\"login\" action=\"\" method=\"post\">" +
	"Recipe ID" +
	"<center><input id=\"recipeid\" name=\"recipeid\" type=\"text\" value=\""+recipe+"\" size=\"34\" READONLY/></center>" +
	"Rule ID:"+
	"<center><input id=\"ruleid\" name=\"ruleid\" type=\"text\" size=\"34\"/></center>" +
	"Rule:"+
	"<center><textarea id=\"rule\" name=\"rule\" cols=25 row=36/></textarea></center><br><br>" +
	"Description:"+
	"<center><input id=\"description\" name=\"description\" type=\"text\" size=\"34\" /></center>" +
	"<center><input type=\"button\" name=\"submit\" value=\"add\" onclick=\"javascript:var rule = new Rule(); rule.addRule()\"/></center></form><br />";

	TINY.box.show(content2,0,0,0,1);
}

Rule.prototype.listRulesOfRecipe = function(recipe){
	
	var div = document.getElementById("rulesOfrecipe"+recipe);
	if(div == null){
	
		var ajax = getXMLHttpRequest();
		if(ajax != null){
			recipe = recipe.replace("#", "%23");
			ajax.open("get", "/kres/rule/of-recipe/"+recipe, true);
			ajax.setRequestHeader("Accept", "application/rdf+json");
			ajax.onreadystatechange = function() {
				if(ajax.readyState == 4) {
			    	if (ajax.status == 200) {
			    		var jsonObj = ajax.responseText;
			    		
			    		alert(jsonObj);
			    		var databank = $.rdf.databank().load(JSON.parse(jsonObj));
		    			
		    			var rdf = $.rdf({databank:databank});
		    			
		    			recipe = recipe.replace("%23", "#");
		    			var rules = rdf.prefix('rmi', 'http://kres.iks-project.eu/ontology/meta/rmi.owl#')
										 .where('<' + recipe + '> rmi:hasRule ?rule')
										 .select();
		    			
		    			
		    			var content = "<div id=\"rulesOfrecipe"+recipe+"\"><br><br>";
		    			
		    			for(var rule in rules){
		    			
		    				var ruleURI = rules[rule].rule.toString();
		    				var r = ruleURI.replace("<", "").replace(">", "");
		    				content += "<li>"+ r;
		    				content += "<a href=\"javascript:showScopeConfigurationOptions('"+r+"')\"><img class=\"configure\" alt=\"Configure\" src=\"/kres/static/images/configure.gif\"></a>";
		    				content += "<div id=\""+r+"\" class=\"scopeDIV\">" +
		    								"<a href=\"javascript:deleteRecipe("+r+")\" alt=\"delete scope\">" +
		    										"<img src=\"/kres/static/images/delete.gif\" alt=\"delete scope\"></a>" +
		    								"<a href=\"javascript:var rule = new Rule(); rule.displayAddBox('"+r+"');\" alt=\"add rule to scope\">" +
		    										"<img src=\"/kres/static/images/addRule.gif\" alt=\"add rule to scope\"></a>" +
		    								"<a href=\"javascript:var rule = new Rule(); rule.listRulesOfRecipe('"+r+"');\" alt=\"list rules of recipe"+r+"\">" +
		    										"<img src=\"/kres/static/images/rules.gif\" alt=\"list rules of recipe"+r+"\"></a>" +
		    							"</div>";
		    			}
		    			
		    			
		    			content += "</ul>";
		    			
		    			content += "<div id='\"visualization"+recipe+"\'></div>";
		    			content += "<div id='\"visualization"+recipe+"-details\'></div>";
		    			
		    			content += "</div>";
		    			
		    			
		    			var ruleListDIV = document.getElementById(recipe);
		    			if(ruleListDIV != null){
		    				ruleListDIV.innerHTML = ruleListDIV.innerHTML + content;
		    			}
		    			
		    			loadGraph("visualization"+recipe, jsonObj);
			    	}
				}
			}
			
			ajax.send(null);
		}
	}
	else{
		div.style.display = 'block';
	}
	var listRulesA = document.getElementById("listRulesA");
	if(listRulesA != null){
		listRulesA.href = "javascript: var rule = new Rule(); rule.hideRulesOfRecipe('"+recipe+"');";
	}
}

Rule.prototype.hideRulesOfRecipe = function(recipe){
	var div = document.getElementById("rulesOfrecipe"+recipe);
	if(div != null){
		div.style.display = 'none';
	}
	
	var listRulesA = document.getElementById("listRulesA");
	if(listRulesA != null){
		listRulesA.href = "javascript: var rule = new Rule(); rule.listRulesOfRecipe('"+recipe+"');";
	}
}

function Refactorer(){
	return this;
}

Refactorer.prototype.runRefactoringStoreLazy = function() {
	var recipeEl = document.getElementById("recipe");
	var inputGraphEl = document.getElementById("input-graph");
	var outputGraphEl = document.getElementById("output-graph");
	
	if(recipeEl!=null && inputGraphEl!=null && outputGraphEl!=null){
		var ajax = getXMLHttpRequest();
		if(ajax != null){
			
			var recipe = recipeEl.value.replace("#", "%23");
			var inputGraph = inputGraphEl.value.replace("#", "%23");
			var outputGraph = outputGraphEl.value.replace("#", "%23");
			alert(outputGraph +" - "+inputGraph+" - "+recipe);
			ajax.open("get", "/kres/refactorer/lazy?recipe="+recipe+"&input-graph="+inputGraph+"&output-graph="+outputGraph, true);
			ajax.setRequestHeader("Accept", "application/rdf+json");
			ajax.onreadystatechange = function() {
				if(ajax.readyState == 4) {
			    	if (ajax.status == 200) {
			    		alert("Refactoring completed");
			    	}
				}
			}
			ajax.send(null);
		}
	}
}

Refactorer.prototype.runRefactoringFileLazy = function() {
	
	var formEl = document.getElementById("iForm");
	if(formEl != null){
		formEl.submit();
	}
	/*var recipeEl = document.getElementById("recipe");
	var inputGraphEl = document.getElementById("graph");
	
	if(recipeEl!=null && inputGraphEl!=null){
		var ajax = getXMLHttpRequest();
		if(ajax != null){
			
			var recipe = recipeEl.value;
			var inputGraph = inputGraphEl.value;
			
			alert("recipe: "+recipe+" - input: "+inputGraph);
			ajax.open("post", "/kres/refactorer/lazy", true);
			ajax.setRequestHeader("Accept", "application/rdf+json");
			ajax.onreadystatechange = function() {
				if(ajax.readyState == 4) {
			    	if (ajax.status == 200) {
			    		alert("Refactoring completed");
			    	}
				}
			}
			
			var parameters = "recipe='"+recipe+"'&input="+inputGraph
			ajax.send(parameters);
		}
	}*/
}

Refactorer.prototype.showRefactoring = function(type, ns){
	var contentTag = document.getElementById("refactoring");
	
	var content = "";
	if(contentTag != null){
		var submit = "";
		if(type == 0){
			content += getGraphs("input-graph", ns);
			content += "Output graph ID<br><input type=\"text\" id=\"output-graph\" name=\"output-graph\">";
			
			submit = "<input type=\"submit\" value=\"run refactoring\" onClick=\"javascript:var refactorer = new Refactorer(); refactorer.runRefactoringStoreLazy()\">";
		}
		else{
			content += "<form id=\"iForm\" action=\"/kres/refactorer/lazy\" method=\"post\" enctype=\"multipart/form-data\" onsubmit=\"alert(\"finito\")\">";
			content += "Select a graph from file: <input id=\"graph\" type=\"file\" name=\"input\"><br>";
			
			submit += "<input type=\"button\" value=\"run refactoring\" onclick=\"var refactorer=new Refactorer(); refactorer.runRefactoringFileLazy();\"></form>";
			
		}
		
		content += getRecipies();
		content += submit;
		
		contentTag.style.display = 'block';
		contentTag.innerHTML = content;
	}
}

function Storage(){
	return this;
}

Storage.prototype.loadGraph = function(graphID) {
	graphID = graphID.replace("<", "").replace(">", "").replace("#", "%23");
	var ajax = getXMLHttpRequest();
	if(ajax != null){
		
		
		ajax.open("get", "/kres/graphs/"+graphID, true);
		ajax.setRequestHeader("Accept", "application/rdf+xml");
		ajax.onreadystatechange = function() {
			if(ajax.readyState == 4) {
		    	if (ajax.status == 200) {
		    		var response = ajax.responseText;
		    		if(response != null){
		    			var div = document.getElementById("graphDIV");
		    			if(div != null){
		    				response = response.replace("<", "&lt;").replace(">", "&gt;");
		    				div.innerHTML = "<code class=\"code\">"+response+"</code>";
		    				div.style.display = 'block';
		    			}
		    		}
		    	}
			}
		}
		
		ajax.send(null);
	}
}

function Demo(){
	return this;
}

Demo.prototype.enhance = function(content) {
	var ajax = getXMLHttpRequest();
	if(ajax != null){
		ajax.open("post", "/engines", true);
		ajax.setRequestHeader("Content-type", "application/x-www-form-urlencoded")
		ajax.onreadystatechange = function() {
			if(ajax.readyState == 4) {
		    	if (ajax.status == 200) {
		    		var jsonObj = ajax.responseText;
	    			
	    			var databank = $.rdf.databank().load(JSON.parse(jsonObj));
	    			
	    			var rdf = $.rdf({databank:databank});
	    			
	    			
	    			var references = rdf.prefix('fise', 'http://fise.iks-project.eu/ontology/')
									 .where('?subject fise:entity-reference ?reference')
									 .select();
	    			var content = "";
	    			for(var reference in references){
	    				var referenceURI = references[reference].reference.toString();
	    				content += reference+"<br>";
	    			}
	    			
	    			var div = document.getElementById("fiseResult");
	    			if(div != null){
	    				div.innerHTML = content;
	    				div.style.display = 'block';
	    				
	    			}
		    	}
			}
		}
		
		var parameter = "content="+content+"&format=application/rdf+json";
		ajax.send(parameter);
	}
		
} 