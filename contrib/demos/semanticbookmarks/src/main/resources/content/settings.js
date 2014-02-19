var semanticbookmarks = {
  onSettingsAccept: function() {
     var prefManager = Components.classes["@mozilla.org/preferences-service;1"].getService(Components.interfaces.nsIPrefBranch);

     prefManager.setCharPref("extensions.semanticbookmarks.server", document.getElementById("settings_server").value);
     prefManager.setCharPref("extensions.semanticbookmarks.user_name", document.getElementById("settings_user_name").value);
     prefManager.setCharPref("extensions.semanticbookmarks.user_uri", document.getElementById("settings_user_uri").value);

     window.close();
  },
  onSettingsCancel: function() {
     window.close();
  },
  onSettingsLoad: function() {
    var prefManager = Components.classes["@mozilla.org/preferences-service;1"].getService(Components.interfaces.nsIPrefBranch);

    document.getElementById("settings_server").value = prefManager.getCharPref("extensions.semanticbookmarks.server");
    document.getElementById("settings_user_name").value = prefManager.getCharPref("extensions.semanticbookmarks.user_name");
    document.getElementById("settings_user_uri").value = prefManager.getCharPref("extensions.semanticbookmarks.user_uri");

    this.loadjQuery(semanticbookmarks);
  },
  loadjQuery : function(context) {
    var loader = Components.classes["@mozilla.org/moz/jssubscript-loader;1"].getService(Components.interfaces.mozIJSSubScriptLoader);
    loader.loadSubScript("chrome://semanticbookmarks/content/jquery.js",context);

    var jQuery = window.jQuery.noConflict(true);
        if( typeof(jQuery.fn._init) == 'undefined') { jQuery.fn._init = jQuery.fn.init; }
    semanticbookmarks.jQuery = jQuery;
  },  
  onCreateUser: function() {
    var prefManager = Components.classes["@mozilla.org/preferences-service;1"].getService(Components.interfaces.nsIPrefBranch);

    // init jQuery
    var jQuery = semanticbookmarks.jQuery;
    var $ = function(selector,context){
       return new  jQuery.fn.init(selector,context||window._content.document);
    };
    $.fn = $.prototype = jQuery.fn;
        
    var userName = document.getElementById("settings_user_name").value;
    var userURI = document.getElementById("settings_user_uri").value;

    var rdf = "<?xml version='1.0' encoding='UTF-8' ?>";
    rdf += "<rdf:RDF xmlns:rdf='http://www.w3.org/1999/02/22-rdf-syntax-ns#' xmlns:sorg='http://schema.org/'>";
    rdf += "<rdf:Description rdf:about='" + userURI + "' ";
    rdf += "rdf:type='http://schema.org/Person'>";
    rdf += "<sorg:name>" + userName + "</sorg:name>";
    rdf += "</rdf:Description>";
    rdf += "</rdf:RDF>";

    jQuery.ajax( {
      type : "POST",
      url : prefManager.getCharPref("extensions.semanticbookmarks.server") + "/entityhub/entity?update=true",
      contentType : "application/rdf+xml",
      data : rdf,
      cache : false,
      success : function(data, textStatus, jqXHR) {
        alert("Created user entity\n" + userURI);
      },
      error : function(jqXHR, textStatus, errorThrown) {
        alert(("Error creating user entity.\n" + jqXHR.responseText));
      }
    });
  },
  onInit: function() {
    this.initSchemata();
  },
  initSchemata: function() {
    var likeSchema = {
      "@context" : {
        "schema" : "http://schema.org/",
        "@types" : {
          "person" : "schema:Person",
          "page"   : "schema:WebPage"
        }
      }
    };
    semanticbookmarks.addSchema(likeSchema, "personlikeswebpage", this.initDislikeSchema);
  },
  initDislikeSchema: function() {
    var dislikeSchema = {
      "@context" : {
        "schema" : "http://schema.org/",
        "@types" : {
          "person" : "schema:Person",
          "page"   : "schema:WebPage"
        }
      }
    };
    semanticbookmarks.addSchema(dislikeSchema, "persondislikeswebpage", function() { alert("Init successful.") });
  },
  addSchema: function(schema, schemaId, onSuccess) {
        // init jQuery
    var jQuery = semanticbookmarks.jQuery;
    var $ = function(selector,context){
       return new  jQuery.fn.init(selector,context||window._content.document);
    };
    $.fn = $.prototype = jQuery.fn;

    var prefManager = Components.classes["@mozilla.org/preferences-service;1"].getService(Components.interfaces.nsIPrefBranch);

    jQuery.ajax( {
      type : "PUT",
      url : prefManager.getCharPref("extensions.semanticbookmarks.server") + "/factstore/facts/" + schemaId,
      contentType : "application/json",
      data : JSON.stringify(schema),
      cache : false,
      success : onSuccess,
      error : function(jqXHR, textStatus, errorThrown) {
        alert("Error on init.\n" + jqXHR.responseText);
      }
    });
  }
};
