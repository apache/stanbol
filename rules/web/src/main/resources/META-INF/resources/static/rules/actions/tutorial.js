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
/**
 * Author andrea.nuzzolese
 *
 **/

var globalRDF = new Array();

var globalDatabank = new Array();

var databankCounter = 0;

var inputRDF;

/*
* ExhibitDemo produce una serializzazione json del grafo RDF che può essere usata per visualizzare i risultati con Exhibit (http://www.simile-widgets.org/exhibit/).
*/
function ExhibitDemo(){

}

ExhibitDemo.prototype.serializeJSON = function(json){

	// serializza il json+rdf in un formato compatibile con Exhibit (un esempio di json per exhibit si trova qui http://www.simile-widgets.org/exhibit/examples/presidents/presidents.js).
	var jsonExhibit = "{ \"items\" : [";
	
	
	// Carica il json+rdf nel databank di rdfquery.	
	var databank = $.rdf.databank().load(json);
	
	// Prendi l'array di triple contenuto nel databank di rdfquery.
	var triples = databank.triples();
						
						
	/*
	 * Costruisci un array in modo che ad ogni individuo corrisponda un array di oggetti Statement che contengono coppie (proprietà, valore)
	 */ 
	var myArray = new Array();
		
	for(var j=0; j<triples.length; j++) {
			
		var index = triples[j].subject.toString().replace("<","").replace(">","");
		
		var predicate = triples[j].property.toString().replace("<","").replace(">","");
		
		var object = triples[j].object.toString().replace("<","").replace(">","");	
		
		var statements = myArray[index];
		
		if(statements == null){
			statements = new Array();
		} 
		
		statements[statements.length] = new Statement(getLocalname(predicate), getLiteral(object)); 
		
		myArray[index] = statements;  
	}
	
	
	var first = true;
	for(var index in myArray){
	
		var subject = getLocalname(index);
		var statements = myArray[index];
		
		
		
		
		var innerFirst = true;
		
		var item = "";
		
		var genre = "";
		
		var visitedProps = new Array();
		
		for(var i=0; i<statements.length; i++){
			var statement = statements[i];
			
			var prop = statement.getProperty();
			
			if(prop != "type"){
				
				if(prop == "genre"){
					if(genre.length>0){
						genre += ", ";
					} 
					genre += "\"" + statement.getObject() + "\"";
				}
				else if(!contains(visitedProps, prop)){
				
					visitedProps[prop] = prop;
				
					if(!innerFirst && !item.endsWith(",") && item.length>0){
						item += ",";
					}
					else{
						innerFirst = false;
					}
					
					
					
					item += "\"" + prop + "\" : \"" + statement.getObject() + "\"";
					
				}
			}
			
			
		}
		
		if(item.length > 0){
			
			if(!first){
				jsonExhibit += ",";
			}
			else{
				first = false;
			}
			
			if(genre.length > 0){
				item += ",  \"genre\" : [" + genre + "]";
			}
			jsonExhibit += "{" + item + ", \"type\" : \"Musician\" }";
		}
		
		
	}
	
	jsonExhibit += "]}";
	
	return jsonExhibit; 
}


// Oggetto Statement che contiene coppie (proprietà, valore)
function Statement(property, object){
	this.property = property;
	this.object = object;
	
	this.getProperty = function() {
		return this.property;
	}
	
	this.getObject = function() {
		return this.object;
	}
}


/**
 * Classe Tutorial.
 * effettua le chiamate all'Enhancer, all'EntityHub e al Refactor
 */
function Tutorial(){

	this.enhance = function(text, format){

			//dati da mandare in post
			var data = {
				// content è il parametro da passare a Stanbol
		       	content: text,
		       	// url è il parametro che identifica la locazione di Stanbol
		       	url: "http://localhost:8080/engines",
			};
		     
			 $.ajaxSetup({ 
			 		'beforeSend': function(xhr) {
			 			xhr.setRequestHeader("Accept", "application/rdf+json")} 
			 		}
			 	);
		    
			$.ajax({
		       type: "POST",
		       url: "../proxy/proxy.php",
		       data: data,
		       success: function(result) {
		    	   //alert(result);
		       	 	var jsonOBJ = jQuery.parseJSON(result);
		       	 	var databank = $.rdf.databank().load(jsonOBJ);
		       	 	    
			 	 	var rdf = $.rdf({databank:databank});
		            
		            //seleziona con rdf query solo i fise:entity-reference, ossia solo le URI delle entità trovate.
					var references = rdf.prefix('fise', 'http://fise.iks-project.eu/ontology/')
		                                 .where('?subject fise:entity-reference ?reference')
		                                 .select();
		            var arrRefs = new Array();
		                
		            for(var subject in references){
		                var referenceURI = references[subject].reference.toString().replace("<", "").replace(">", "");
		                arrRefs.push(referenceURI);
		            }
		                
		            arrRefs = unique(arrRefs);
		                
		            /*
		             * Con questo ciclo for inserisco le uri delle entità trovate.
		             * Le uri vengono inserite all'interno del blocco div con ID entities.
		             * La variabile contente contiene la lista delle entità. 
		             */
		            var content = "";
		            for(var uriRef in arrRefs){
		                var uri = arrRefs[uriRef];
		                content += "<input type=\"checkbox\" id=\""+uri+"\">" + uri + "</input><br>";
		            }
		           
		            
		            $('#entities').append(content);
		            
					$('#enginesOuput').show();
					
		       },
		       error: function(result) {
		         alert("Error");
		       },
		       dataType: "html"
		     });
		};
	
	this.queryEntityHub = function(){

		var successfulRequests = 0;
		
		var entities = $('.indentLigth');
		
		
		/*
		 * Add the recipe into the textarea identified by the ID recipecode.
		 * Show the recipe block. 
		 */
		var recipeTMP = recipe.replaceAll("<", "&lt;").replaceAll(">", "&gt;");
		$('#recipecode').html(recipeTMP);
		$('#refactoringBlock').show();
		
		
		for(var i=0; i<entities.length; i++){
			var entity = entities[i].id;
			
			
			$.ajaxSetup({ 
		 		'beforeSend': function(xhr) {
		 			xhr.setRequestHeader("Accept", "application/rdf+json")} 
		 		}
		 	);
		 	
		 	var data = {
		 		id: entity.replace("-fetch", ""),
		 		url: "http://localhost:8080/entityhub/sites/entity"
		 	};
		 	
			$.ajax({
		       	type: "GET",
		       	
		       	// l'id è il parametro da inoltrare a Stanbol, url è il parametro che identifica la location di Stanbol (per il Proxy) 
		       	url: "../proxy/proxy.php",
		       	
		       	data: data,
		       	success: function(result) {
					
		       		if(!result.startsWith("Entity")){
		       		
		       			if(result.indexOf("gYear") != -1){
		       				result = result.replaceAll("gYear", "string");
		       				result = result.replaceAll("http://dbpedia.org/datatype/second", "string");
		       			}
		       			
		       			//alert(result);
		       			var jsonGraph = $.parseJSON(result);
			       		
						
						
						var graphID = databankCounter;
						
						databankCounter += 1;
						globalDatabank[graphID] = $.rdf.databank().load(jsonGraph);
						
						var rdf = $.rdf({databank:globalDatabank[graphID]});
						
						
						
						
						var entities = rdf.prefix('rdf', 'http://www.w3.org/1999/02/22-rdf-syntax-ns#')
		  									.where('?s ?p ?o')
		  									.select();
		  					
						var ent = entities[0].s.toString().replace("<", "").replace(">", "");
						
						
						ent = ent.replace(".meta", "");
						
						alert("ent " + ent);
						
						
						entities = rdf.prefix('rdf', 'http://www.w3.org/1999/02/22-rdf-syntax-ns#')
							.where('<'+ent+'> ?p ?o')
							.select();
						
						/*
						 * var elem = document.getElementById(ent + "-fetch");
						 * var button = "<input type=\"button\" value=\"view\" onClick=\"javascript:document.getElementById('"+ent+"-summary').style.display = 'block';\">";
						 * button += "<input type=\"button\" value=\"hide\" onClick=\"javascript:document.getElementById('"+ent+"-summary').style.display = 'none';\"> Relevant entitytypes and properties";
						 * button += "<div id=\"" + ent + "-summary\" class=\"indent\" style=\"display:none; margin-bottom:15px; width:90%; height: auto !important; min-height: 2em; overflow: hidden; border-style: dotted; border-width: 1px; padding:5px;\"><div id=\"" + ent + "-ref-types\" class=\"fetchedEntity\" style=\"width:50%; float:left;\"><b>Types</b><br/></div><div id=\"" + ent + "-ref-props\" class=\"fetchedEntity\" style=\"width:50%; float:right;\"><b>Properties</b><br/></div></div></div>";
						 * elem.innerHTML = elem.innerHTML + " " +  button; 
						 */
						
						
						var content = "<li>"+ent;
						content += "<input type=\"button\" value=\"view\" onClick=\"javascript:document.getElementById('"+ent+"-summary').style.display = 'block';\">";
						content += "<input type=\"button\" value=\"hide\" onClick=\"javascript:document.getElementById('"+ent+"-summary').style.display = 'none';\"> Relevant entitytypes and properties";
						content += "<div id=\"" + ent + "-summary\" class=\"indent\" style=\"display:none; margin-bottom:15px; width:90%; height: auto !important; min-height: 2em; overflow: hidden; border-style: dotted; border-width: 1px; padding:5px;\"><div id=\"" + ent + "-ref-types\" class=\"fetchedEntity\" style=\"width:50%; float:left;\"><b>Types</b><br/></div><div id=\"" + ent + "-ref-props\" class=\"fetchedEntity\" style=\"width:50%; float:right;\"><b>Properties</b><br/></div></div></div>";
						
						$('#graphs').append(content);
						
						
						var propertyArray = new Array();
						var typeArray = new Array();
						
						var typeArrayCounter = 0;
						var propertyArrayCounter = 0;
						for(var j=0; j<entities.length; j++){
							
							var property = entities[j].p.toString().replace("<", "").replace(">", "").replace(".meta", "");
							
							if(property == "http://www.w3.org/1999/02/22-rdf-syntax-ns#type"){
								var value = entities[j].o.toString().replace("<", "").replace(">", "").replace(".meta", "");
								
								if(!contains(typeArray, value)){
									var types = document.getElementById(ent + "-ref-types");
									types.innerHTML = types.innerHTML + value + "<br>";
									typeArray[typeArrayCounter] = value;
									typeArrayCounter += 1;
								}
							}
							else if(!contains(propertyArray, property)){
								var types = document.getElementById(ent + "-ref-props");
								types.innerHTML = types.innerHTML + property + "<br>";
								propertyArray[typeArrayCounter] = property;
								propertyArrayCounter += 1;
							}
						}
		       		}
				
					
				},
		       	error: function(result) {
					alert("Error");
		       	},
		     });
		}
		/*
		var children = $('.fetchedEntity').children();
		for(var i=0; i<children.length; i++){
		
			var entity = $("#entities input:nth-child("+(i+1)+")").attr('id');
			//var entity = children[i].attr('id');
			
			
		    
		     
		}
		*/
	};
	
	
	this.getSameAs = function(){
		
		
		var children = $('#entities').children();
		
		for(var i=0; i<children.length; i++){
		
			
		
			var entity = $("#entities input:nth-child("+(i+1)+")").attr('id');
			
			if(entity != null){
				
				
				
				/*
				 * Mostra il blocco div con ID sameas.
				 * 
				 */
				$('#sameas').show();
				
				var json = {
							"selected": ["http:\/\/www.w3.org\/1999\/02\/22-rdf-syntax-ns#type"], 
							"offset": "0", 
							"limit": "3", 
							"constraints": [ {"type": "reference", 
												"field": "http:\/\/www.w3.org\/2002\/07\/owl#sameAs", 
												"value": "" + entity + ""
													}] 
							};
				
				
				
				var jsontext = JSON.stringify(json);
				
				
				var data = {
			 		query: encodeURI(jsontext),
			 		binary: "true",
			 		url: "http://localhost:8080/entityhub/sites/query"
			 	};
			     
				$.ajax({
			       	type: "POST",
			       	
			       	// l'id è il parametro da inoltrare a Stanbol, url è il parametro che identifica la location di Stanbol (per il Proxy) 
			       	url: "../proxy/proxy.php",
			       	
			       	data: data,
			       	//contentType: "application/json",
			       	success: function(result) {
						
			       		
			       		$('#sameasH3').show();
			       		$('#showcodeoffetch').show();
			       		
			       		
			       		result = result.replaceAll("http:\\/\\/www.w3.org\\/1999\\/02\\/22-rdf-syntax-ns#type", "rdftype")
			       		
						var jsonOBJ = $.parseJSON(result);
						
			       		var entityID = jsonOBJ.query.constraints[0].value; 
			       		
						var results = jsonOBJ.results;
						
						var reference;
						
						var content = "SameAs Entities found for " + entityID + "<br>";
						
						content += "<div id=\"" + entityID + "-fetch\" class=\"indentLigth\">" + entityID + "</div>";
						for(var i=0; i<results.length; i++){
							var type = results[i].rdftype;
							
							var id = results[i].id
							
							
							content += "<div id=\"" + id + "-fetch\" class=\"indentLigth\">" + id + "</div>";
						}
						
						
						/*
						 * Aggiungo le sameAs entity in testa al blocco div con ID sameas. 
						 */
						
						$('#sameas').html(content+$('#sameas').html());
						
						
					},
			       	error: function(result) {
						alert("Error");
			       	},
			     });
				
				
				
			}
			
			
		}
		
	};
	
	
	this.refactor = function(){
	
		
		var graph = document.getElementById('input-rdf-graph');
		var graphFile = graph.files[0];
		//var graphString = graphFile.getAsText("")
		
		var reader = new FileReader();
		reader.onload = function(e) { 
			
			
			var graphString = e.target.result; 
			
		
			/*
			 * Preparo il messaggio da mandare in post multipart/form-data.
			 * E' come un messaggio di posta. Si compone di più parti divise da un boundary.
			 *
			 */
			var boundary = "---------------------------7da24f2e50046";
			
			var rules = '--' + boundary + '\r\n'
		         + 'Content-Disposition: form-data; name="recipe";' + '\r\n\r\n'
		         + recipe + '\r\n';
			
			//rdf = Utf8.encode(rdf);
		    
		    /*
		    rdf = Utf8.encode(rdf),
			
			rdf = rdf.replaceAll(" xmlns:xml=\"http://www.w3.org/XML/1998/namespace\"", "");
			rdf = rdf.replaceAll(">\"", ">");
			rdf = rdf.replaceAll("\"<", "<");
			*/
		    
			rules += '--' + boundary + '\r\n'
		         // Parameter name is "file" and local filename is "temp.txt"
		         + 'Content-Disposition: form-data; name="input";'
		         + 'filename="temp.txt"' + '\r\n'
		         + 'Content-type: application/rdf+xml' + '\r\n\r\n'
		         //+ rdf + '\r\n'
		         + graphString + '\r\n'
		         + '--' + boundary;
		         
		    
			
		
			 $.ajaxSetup({ 
			 	'beforeSend': function(xhr) {
			 		xhr.setRequestHeader("Accept", "text/owl-manchester")}
			 	}
			 );
			 
			     
			$.ajax({
		       	type: "POST",
		       	url: "http://localhost:8080/refactor/apply",
		       	data: rules,
		       	contentType: 'multipart/form-data;boundary=---------------------------7da24f2e50046',
		       	async: false,
		       	success: function(result) {
					
		       		/*
		       		 * Prepare the box with the refactored graph
		       		 */
		       		
		       		var id = "refactoredGraph-0";
		       		
		       		result = result.replaceAll("<", "&lt;").replaceAll(">", "&gt;");
		       		
		       		var content = 	"<li> OUTPUT "  +
		       						"<input type=\"button\" value=\"view\" onClick=\"javascript:$('#" + id + "').show();\">" +
		       						"<input type=\"button\" value=\"hide\" onClick=\"javascript:$('#" + id + "').hide();\"></li>" +
		       						"<div id=\"refactoredGraph-0\" style=\"display:none;\"><pre>" + result + "</pre></div>"
		       		
		       		$('#refactoringoutput').show();
		       		
		       		
		       		
		       		/*
		       		 * Append the box with the refactored graph to the block with the ID refactoringoutput. 
		       		 */
		       		
		       		$('#refactoringoutput').append(content);
		       		
		 			
				},
		       	error: function(result) {
					alert("Error");
		       	}
		     });
		
		};  
		var graphString = reader.readAsText(graphFile, "UTF-8");
		
			
	};
		
		
	
}


function unique(arr) {
	var r = new Array();
	o:for(var i = 0, n = arr.length; i < n; i++)
	{
		for(var x = 0, y = r.length; x < y; x++)
		{
			if(r[x]==arr[i])
			{
				continue o;
			}
		}
		r[r.length] = arr[i];
	}
	return r;
};


function getNamespace(resource) {

	var slashIndex = resource.lastIndexOf("/");
	var hashIndex = resource.lastIndexOf("#");
	
	var namespace;
	if(slashIndex>hashIndex){
		namespace = resource.substring(0, slashIndex+1);
	}
	else{
		namespace = resource.substring(0, hashIndex+1);
	}
	
	return namespace;
}


function getLiteral(resource){
	if(resource.startsWith("\"\"")){
		resource = resource.substring(2, resource.length);
		if(resource.lastIndexOf("\"\"") > 0){
			resource = resource.substring(0, resource.lastIndexOf("\"\""));
		}
		
	}
	else if (resource.startsWith("\"")){
		resource = resource.substring(1, resource.length);
		if(resource.lastIndexOf("\"") > 0){
			resource = resource.substring(0, resource.lastIndexOf("\""));
		}
	}
	return resource;
}

function getLocalname(resource) {

	var slashIndex = resource.lastIndexOf("/");
	var hashIndex = resource.lastIndexOf("#");
	
	var namespace;
	if(slashIndex>hashIndex){
		namespace = resource.substring(slashIndex+1, slashIndex.length);
	}
	else if(slashIndex<hashIndex){
		namespace = resource.substring(hashIndex+1, hashIndex.length);
	}
	else{
	
		if(resource.startsWith("\"")){
			resource = resource.substring(2, resource.length);
			resource = resource.substring(0, resource.lastIndexOf("\"\""));
		}
		namespace = resource;
	}
	
	return namespace;
}

String.prototype.replaceAll = function(stringToFind,stringToReplace){
	var temp = this;
	var index = temp.indexOf(stringToFind);
    while(index != -1){
        temp = temp.replace(stringToFind,stringToReplace);
        index = temp.indexOf(stringToFind);
    }
    return temp;
}

String.prototype.startsWith = function(str){
    return (this.indexOf(str) === 0);
}

String.prototype.endsWith = function(str){
    return this.indexOf(str, this.length - str.length) !== -1;
}

function contains(a, obj){
  for(var i in a) {
    if(a[i] === obj){
      return true;
    }
  }
  return false;
}

var Utf8 = {
 
	// public method for url encoding
	encode : function (string) {
		string = string.replace(/\r\n/g,"\n");
		var utftext = "";
 
		for (var n = 0; n < string.length; n++) {
 
			var c = string.charCodeAt(n);
 
			if (c < 128) {
				utftext += String.fromCharCode(c);
			}
			else if((c > 127) && (c < 2048)) {
				utftext += String.fromCharCode((c >> 6) | 192);
				utftext += String.fromCharCode((c & 63) | 128);
			}
			else {
				utftext += String.fromCharCode((c >> 12) | 224);
				utftext += String.fromCharCode(((c >> 6) & 63) | 128);
				utftext += String.fromCharCode((c & 63) | 128);
			}
 
		}
 
		return utftext;
	},
 
	// public method for url decoding
	decode : function (utftext) {
		var string = "";
		var i = 0;
		var c = c1 = c2 = 0;
 
		while ( i < utftext.length ) {
 
			c = utftext.charCodeAt(i);
 
			if (c < 128) {
				string += String.fromCharCode(c);
				i++;
			}
			else if((c > 191) && (c < 224)) {
				c2 = utftext.charCodeAt(i+1);
				string += String.fromCharCode(((c & 31) << 6) | (c2 & 63));
				i += 2;
			}
			else {
				c2 = utftext.charCodeAt(i+1);
				c3 = utftext.charCodeAt(i+2);
				string += String.fromCharCode(((c & 15) << 12) | ((c2 & 63) << 6) | (c3 & 63));
				i += 3;
			}
 
		}
 
		return string;
	}
 
}

function load_xml_content_string(xmlData) {
	if (window.ActiveXObject) {
		//for IE
		xmlDoc=new ActiveXObject("Microsoft.XMLDOM");
		xmlDoc.async="false";
		xmlDoc.loadXML(xmlData);
		return xmlDoc;
	} else if (document.implementation && document.implementation.createDocument) {
		//for Mozila
		parser=new DOMParser();
		xmlDoc=parser.parseFromString(xmlData,"text/xml");
		return xmlDoc;
	}
}

function saveRecipe(){
	var recipeTMP=$('#recipecode').val(); 
	recipeTMP = recipeTMP.replaceAll("&lt;", "<").replaceAll("&gt;", ">"); 
	recipe = recipeTMP; 
	alert('The recipe as been saved');
}

var recipe = "dbpedia = <http://dbpedia.org/ontology/> . \n" +
			"dbpprop = <http://dbpedia.org/property/> . \n" +
			"google = <http://rdf.data-vocabulary.org/#> . \n" +
			"foaf = <http://xmlns.com/foaf/0.1/> . \n" +
			"rdf = <http://www.w3.org/1999/02/22-rdf-syntax-ns#> . \n" +
			"skos = <http://www.w3.org/2004/02/skos/core#> . \n" +
			"gn = <http://www.geonames.org/ontology#> .\n" +
			"\n" +
			"peopleTypeRule [ is(dbpedia:Person, ?x) -> is(google:Person, ?x) ] . \n" +
			"peopleNameRule [ is(dbpedia:Person, ?x) . values(foaf:name, ?x, ?y) -> values(google:name, ?x, ?y) ] . \n" +
			"peopleNickRule [ is(dbpedia:Person, ?x) . values(foaf:nick, ?x, ?y) -> values(google:nickname, ?x, ?y) ] . \n" +
			"peoplePhotoRule [ is(dbpedia:Person, ?x) . has(dbpedia:thumbnail, ?x, ?y) -> has(google:photo, ?x, ?y) ] . \n" +
			"peopleProfessionRule [ is(dbpedia:Person, ?x) . has(dbpedia:profession, ?x, ?y) -> has(google:title, ?x, ?y) ] . \n" + 
			"peopleOccupationRule [ is(dbpedia:Person, ?x) . has(dbpedia:occupation, ?x, ?y) -> has(google:title, ?x, ?y) ] . \n" + 
			"peopleRoleRule [ is(dbpedia:Person, ?x) . values(dbpedia:role, ?x, ?y) -> values(google:role, ?x, ?y) ] . \n" + 
			"peopleHomepageRule [ is(dbpedia:Person, ?x) . has(foaf:homepage, ?x, ?y) -> has(google:url, ?x, ?y) ] . \n" + 
			"peopleAffiliationRule [ is(dbpedia:Person, ?x) . has(dbpedia:employer, ?x, ?y) -> has(google:affiliation, ?x, ?y) ] . \n" + 
			"peopleKnowsRule [ is(dbpedia:Person, ?x) . has(foaf:knows, ?x, ?y) -> has(google:friend, ?x, ?y) ] . \n" + 
			"peopleAddressRule [ is(dbpedia:Person, ?x) . values(dbpedia:address, ?x, ?y) -> values(google:address, ?x, ?y) ]";

/*
var recipe = rule = "dbpont = <http://dbpedia.org/ontology/> . \n" +
					"foaf = <http://xmlns.com/foaf/0.1/> . \n" +
					"rdfs = <http://www.w3.org/2000/01/rdf-schema#> . \n" +
					"myNS = <http://www.cs.unibo.it/demo/> . \n" +
					"yago = <http://dbpedia.org/class/yago/> . \n" +
					
					"musician[is(dbpont:MusicalArtist, ?entity) -> is(myNS:Musician, ?entity)] . \n" +  
					
					// name
					"name[is(dbpont:MusicalArtist, ?entity) . values(foaf:name, ?entity, ?name) -> values(rdfs:label, ?entity, ?name)] . \n" +
					
					// date and place of birh and death
					"birthDate[is(dbpont:MusicalArtist, ?entity) . has(dbpont:birthDate, ?entity, ?name) -> values(myNS:birth, ?entity, ?name)] . \n" +
					"birthDate[is(dbpont:MusicalArtist, ?entity) . has(dbpont:deathDate, ?entity, ?name) -> values(myNS:death, ?entity, ?name)] . \n" +
					
					"birthPlace[is(dbpont:MusicalArtist, ?entity) . has(dbpont:birthPlace, ?entity, ?name) -> values(myNS:birthPlace, ?entity, ?name)] . \n" +
					"deathPlace[is(dbpont:MusicalArtist, ?entity) . has(dbpont:deathPlace, ?entity, ?name) -> values(myNS:deathPlace, ?entity, ?name)] . \n" +
					
					// picture
					"dbpontImg[is(dbpont:MusicalArtist, ?entity) . has(dbpont:thumbnail, ?entity, ?img) -> values(myNS:imageURL, ?entity, ?img)] . \n" +
					
					// genre
					"genre[is(dbpont:MusicalArtist, ?entity) . has(dbpont:genre, ?entity, ?genre) -> values(myNS:genre, ?entity, ?genre)]";

*/