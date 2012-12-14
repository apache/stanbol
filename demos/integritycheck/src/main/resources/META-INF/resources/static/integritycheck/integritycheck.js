/**
 * This script includes code for the Stanbol Demo: Integrity Check
 * It uses jQuery as basic javascript library and rdfQuery for processing 
 * RDF output and jQuery/Cookie extensions for cookie management.
 * 
 * 
 * @author enridaga, anuzzolese
 */
$(document).ready(function(){
	// We must add this datatype to jqueryrdf
	jQuery.typedValue.types["http://www.w3.org/2001/XMLSchema#long"] = jQuery.typedValue.types["http://www.w3.org/2001/XMLSchema#float"];
	// Base application URL, (we want to know this several times in the process)
	var baseA = document.createElement("a");
	baseA.href="/";
	var baseURL = baseA.protocol+"//"+baseA.host;
	
	// Utility
	contains=function(sbj,obj) {
	    for (var i = 0; i < sbj.length; i++) {
	        if (sbj[i] === obj) {
	            return true;
	        }
	    }
	    return false;
	};
	distinct = function(sbj) {
		var res = [];
	    for (var i = 0; i < sbj.length; i++) {
	        if (!contains(res,sbj[i])) {
	            res.push(sbj[i]);
	        }
	    }
	    return res;
	};
	/*********************************************
	 * This function implements Step 1 of the Demo
	 *********************************************/
	var enhance=function(event){
		event.preventDefault();
		// We disable the UI item
		$(this).attr('disabled','disabled');
		
		// We prepare the data
		var data = {
				content: $("#textInput").val(),
				ajax: false,
				format:  "application/rdf+json"
		        };
		$.ajax({
		       type: "POST",
		       url: "/engines",
		       data: data,
		       cache: false,
		       beforeSend: function(xhr){
		    	   xhr.setRequestHeader("Accept", "application/rdf+json");
		       },
		       success: function(result) {
			        var databank = $.rdf.databank().load(result);
			        var rdf = $.rdf({databank:databank});
			        var references = rdf.prefix('fise', 'http://fise.iks-project.eu/ontology/')
									 .where('?subject fise:entity-reference ?reference')
									 .select();
			        // get unique values
			        var urls = new Array();
			        for(var subject in references){
		 				var referenceURI = references[subject].reference.toString().replace("<", "").replace(">", "");
		 				urls.push(referenceURI);
			        }
			        urls = distinct(urls);
			        var resultsUL = $("#step-1-results");
			        resultsUL.empty();
			        resultsUL.show();
			        for(var u in urls){
		 				resultsUL.append("<li>"+urls[u]+"</li>");
			        }
			        $("#step-1-after .message").html("These are the entities found by the <b>/engines</b> service:");
			        $("#step-1-after").show();
			        // If everything worked fine, we show next step!
			        $("#step-2").show();
		       },
		       error: function(result) {
			       $("#step-1-after .message").html(result.statusText+"<br/><br/>You can <a href=\"/integritycheck\">try again</a>.");
			       $("#step-1-after").show();
		       }
		});
	}
	
	/*************************************************
	 * This function implements Step 2 of the demo.
	 * This step is dedicated to the /ontonet service
	 *************************************************/
	collect = function(event){
		event.preventDefault();
		$(this).attr('disabled','disabled');
		
		/**
		 * a) First we check if the /ontonet Scope dedicated to this demo application do exist, elsewhere we create it.
		 */
		var scopeName = "integritycheck";
		var scopeID = "/ontonet/ontology/"+scopeName;

		setupScope = function(scopeID){
			$.ajax({
			       type: "PUT",
			       url: scopeID+"?coreont=" + baseURL + "/static/integritycheck/foaf.rdf" + "&activate=true",
			       cache: false,
			       async: true,
			       success: function(result) {
				        $("#step-2-after .message").html("Scope <b>"+scopeID+"</b> have been created.");
				        $("#step-2-after").show();
				        setupSession(baseURL,scopeID);
			       },
			       error: function(result) {
			    	   var msg="";
			    	   var go = false;
			    	   if(result.status == '409'){
			    		   msg = "Scope <b>"+scopeID+"</b> is ready."; 
			    		   go = true;
			    	   }else{
			    		   msg = "["+result.status+": "+result.statusText+"] There can be a problem to connect to a remote server. Please try again."
			    	   }
			    	   $("#step-2-after .message").html(msg);
				       $("#step-2-after").show();
				       if(go){
				    	   setupSession(baseURL,scopeID);
				       }
			       }
			});			
		}
		setupSession = function(baseURL,scopeID){
			// We set the scope ID
 		    $.cookie("ontonet-scope-id", baseURL + scopeID);
			// If a) is success
	        $("#step-2-after .message").append("<br/>Creating /ontonet session...");
			/**
			 * b) Then we create a /ontonet Session, which we use to load data to process in this single demo instance.
			 */
			$.ajax({
			       type: "POST",
			       url: "/ontonet/session",
			       data: {
			    	   scope: baseURL + scopeID
			       },
			       beforeSend: function(xhr){
			    	   xhr.setRequestHeader("Accept", "application/rdf+json");
			       },
			       cache: false,
			       async: false,
			       success: function(result) {
			    	   var datab = $.rdf.databank();
			    	   datab.load(result);
			    	   var rdf = $.rdf({databank:datab});
			    	   var sessionMetadata = rdf.prefix('meta', 'http://kres.iks-project.eu/ontology/onm/meta.owl#')
		    	   			.where('?session meta:hasID ?id')
		    	   			.select();
			    	   for(var subject in sessionMetadata){
			    		   sessionID = sessionMetadata[subject].id.value;
			    	   }
			    	   if(sessionID) {
			    		   //console.log("Session ID is: ",sessionID.toString().replace('<', '').replace('>', ''));
			    		   // We set the cookie, we will launch a DELETE to remove the session on unload!
			    		   $.cookie("ontonet-session-id", sessionID.toString().replace('<', '').replace('>', ''));
			    		   // We get back the scopeID from the cookie
			    		   var scopeID = $.cookie("ontonet-scope-id");
			    		   var sessionID = $.cookie("ontonet-session-id");
			    		   $("#step-2-after .message").append("<br/>Session <b>"+sessionID+"</b> have been created.");
			    		   //console.log("Base url is: ",baseURL);
			    		   //console.log("Scope ID is: ",scopeID);
			    		   /**
			    			 * c) Now we load the demo ontology in the session
			    			 */
			    		   // Loading the demo ontology in the session
			    		   var demoOntologyUrl = baseURL+"/static/integritycheck/demo.owl";
			    		   $("#step-2-after .message").append("<br/>Now, we load the <a href=\""+baseURL+"/static/integritycheck/demo.owl\">demo ontology</a> in the <b>/ontonet/session</b>...");
			    		   $.ajax({
		    					type: "POST",
		    				       url: "/ontonet/session",
		    					   contentType: "application/x-www-form-urlencoded",
		    				       dataType: "json",
		    				       data:{
		    				    	   scope: scopeID,
		    				    	   session: sessionID,
		    				    	   location: demoOntologyUrl
		    				       },
		    				       cache: false,
		    				       timeout: 5000,
		    				       async: true,
		    				       success: function(result) {
		    				    	   $("#step-2-after .message").append("Done.");
		    				    	   $("#step-3").show();
		    				       },
		    				       error: function(result) {
		    				    	   try{
		    				    		   $("#step-2-after .message").append("....FAILED ["+result.status+": "+result.statusText+"]");		       			    				    		   
		    				    	   }catch(e){
		    				    		   // Timed out ...
		    				    		   $("#step-2-after .message").append("....Timeout ");
		    				    	   }	
		    				       }
		    				});
			    	   }else throw Exception("Error parsing service output");
			       },
			       error: function(result) {
			    	   var msg = "<br/>["+result.status+": "+result.statusText+"] There can be a problem opening the session on /ontonet."
			    	   $("#step-2-after .message").append(msg);			       
			       }
			});
		}
        
		setupScope(scopeID);
		
	};
	/*************************************************
	 * This function implements Step 3 of the demo.
	 * This step is dedicated to the /recipe and /rule  services
	 *************************************************/
	setupRule = function(event){
		event.preventDefault();
		// We disable the UI item
		$(this).attr('disabled','disabled');
		
		var sessionID = $.cookie("ontonet-session-id");
		var scopeID = $.cookie("ontonet-scope-id");
		
		// Generate an identifier for the recipe and rule
		// We smartly reuse the sessionID for that
		var recipeID = sessionID.replace("/session","/recipe");
		var ruleID   = sessionID.replace("/session","/rule");
		// Sign both in the cookie, we must remove our recipe and rule
		$.cookie("recipe-id", recipeID);
		$.cookie("rule-id", ruleID);
		
		// Input rule from the user
		// We need to unescape entities here
		var inputRule = $("#step-3-input").val().replace(/&gt;/g,">").replace(/&lt;/g,"<");
		
		$("#step-3-after .message").append("First we need to create recipe: <b>"+recipeID+"</b>....");
		$("#step-3-after").show();
		// a) Create a recipe
		$.ajax({
			type: "POST",
			url: "/recipe",
			dataType: "json",
		    data:{
		    	recipe: recipeID,
		    	description: "Integiry check demo recipe."
		    },
		    cache: false,
		    async: false,
		    beforeSend: function(xhr){
				xhr.setRequestHeader("Accept", "application/rdf+json");
		    },
			contentType: "application/x-www-form-urlencoded",
		    success: function(result) {
		       $("#step-3-after .message").append("...DONE");
		    },
		    error: function(result) {
		       $("#step-3-after .message").append("...FAILED ["+result.status+": "+result.statusText+"]. <a href=\"/\">You can try again</a>.");			       
		    }
		});
		// b) Add the rule to the recipe
		$("#step-3-after .message").append("<br/>Now we add our rule to the recipe...");
		$.ajax({
			type: "POST",
			url: "/rule",
			dataType: "text", 
		    data:{
		    	recipe: recipeID,
		    	rule: ruleID,
		    	'kres-syntax': inputRule,
		    	description: "Integiry check demo rule."
		    },
		    cache: false,
		    async: false,
		    beforeSend: function(xhr){
				xhr.setRequestHeader("Accept", "application/rdf+xml") // This is a strange behaviour... we need to set this to have 'success' event launched when response is 200: OK!
		    },
			contentType: "application/x-www-form-urlencoded",
		    success: function(result) {
		       $("#step-3-after .message").append("...DONE");
		       $("#step-4").show();
		    },
		    error: function(result) {
		       $("#step-3-after .message").append("...FAILED ["+result.status+": "+result.statusText+"]. <a href=\"/\">You can try again</a>.");			       
		    }
		});
	}
	/*************************************************
	 * Step 4: Check the integrity.
	 * Ths step launch the /reasoners/classify service
	 * over the given session and recipe, on each entity
	 * we ask the service to get the entity from the entityhub
	 *************************************************/
	checkIntegrity = function(event){
		event.preventDefault();
		// We disable UI item
		$(this).attr('disabled','disabled');
		
		var number = $("#step-1-results li").size();
		
		$("#step-4-after .message").append("Invoking <b>/reasoners</b> on " + number + " found entities (this can take some time...).<br/>Entities are fetched from the entityhub.");
		$("#step-4-after").show();
		
		var sessionID = $.cookie("ontonet-session-id");
		var scopeID = $.cookie("ontonet-scope-id");
		var recipeID = $.cookie("recipe-id");
		var start = new Date();
		var entityHubService = "http://localhost:8080/entityhub/sites/entity?create=true";
		$("#step-1-results li").each(function(){
			var entityUri = $(this).html();
			var reasonerCall = {
				entity: entityUri,
				type: "GET",
				url: "/reasoners/owl2/classify",
				dataType: "xml",
			    data:{
			    	session: sessionID,
			    	recipe: recipeID,
			    	scope: scopeID,
			    	url: entityHubService+"&id=" + entityUri
			    },
			    cache: false,
			    async: true,
			    beforeSend: function(xhr){
					xhr.setRequestHeader("Accept", "application/rdf+xml");
			    },
			    timeout: 30000,
				contentType: "application/x-www-form-urlencoded",
			    success: function(result) {
			    	number = number-1;
			    	$("#step-4-results").append("<li id=\""+this.entity+"\">"+this.entity+"</li>");
			    	
			    	var databank = $.rdf.databank().load(result);
			    	var lit = $('#step-4-results li[id="' + this.entity + '"]');
			        
					var rdf = $.rdf({databank:databank});
					var validContents = null;
					validContents = rdf.prefix('demo', 'http://www.example.org/integritycheck/').where('?content a demo:ValidContent').select();
					var content="";	
					for(var validContent in validContents){
						var ent = validContents[validContent].content.toString().replace('<', '').replace('>', '');
						if(ent==this.entity){
							content=ent;
							break;
						}
					}
					if(content != "" ){
						lit.append(" &gt; <font color=green>Valid</font> ");
					}else{
						lit.append(" &gt; <font color=red>Not valid</font> ");	
					}
					if(number == 0){
						var end = new Date();
			    		$("#step-4-after").append("Finished in " + (end - start) + " ms");
			    	}
			    },
			    error: function(result) {
			    	number = number-1;
			    	$("#step-4-results").append("<li id=\""+this.entity+"\">"+this.entity+"</li>");
			    	var lit = $('#step-4-results li[id="' + this.entity + '"]');
			    	try{
			    		if(result.status==204){
			    			msg = " &gt; <font color=grey>Inconsistent :(</font>";
			    		}else{
			    			msg = " &gt; <font color=red>FAILED ["+result.status+"].</font>";
			    		}
			    	}catch(e){
		    		   // Timed out ...
		    		   msg = " &gt; Timeout";
			    	}
			    	lit.append(msg);
			    	if(number == 0){
						var end = new Date();
			    		$("#step-4-after").append("Finished in " + (end - start) + " ms");
			    	}
			    }
			};
			$.ajax(reasonerCall);
		});
	}
	/**
	 * Finally we map the UI objects to events
	 */
	$("#step-1-start").click(enhance);
	$("#step-2-start").click(collect);
	$("#step-3-start").click(setupRule);
	$("#step-4-start").click(checkIntegrity);
});

/**
 * Delete the /ontonet session on page re-loading/closing
 */
$(document).ready(function(){
	// This function deletes the ontonet session
	function deleteOntonetSession(deleteID){
		var deleteID = $.cookie("ontonet-session-id");
		var scopeID = $.cookie("ontonet-scope-id");
		if(deleteID){
			$.ajax({
			    type: "DELETE",
			    async: false,
			    url:  "/ontonet/session?scope="+escape(scopeID)+"&session="+escape(deleteID)
			});
			$.cookie("ontonet-scope-id","");
			$.cookie("ontonet-session-id","");
		}
		// We delete the recipe
		var deleteRecipeID = $.cookie("recipe-id");
		if(deleteRecipeID){
			$.ajax({
			    type: "DELETE",
			    async: false,
			    url:  "/recipe?recipe="+escape(deleteRecipeID)
			});
			$.cookie("recipe-id","");
		}
	}
	// We are loading the page right now
	deleteOntonetSession();
	// Do the same when page is unloading
	$(window).unload( deleteOntonetSession );

	// Some browsers remember the status of UI objects,
	// for example form elements and buttons.
	// This means that we could find the demo buttons off
	// when reloading the page to try the demo again.
	$('button.start').removeAttr("disabled");
});