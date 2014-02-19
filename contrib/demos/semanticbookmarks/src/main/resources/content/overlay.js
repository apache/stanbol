var semanticbookmarks = {
  onInit : function() {
    if (!this.initialized) {
      this.initialized = true;
      this.strings = document.getElementById("semanticbookmarks-strings");
      gBrowser.addEventListener("load", function(e) {
        semanticbookmarks.onLoad(e);
      }, true);
    }
  },
  onLoad : function(e) {
    // load jQuery on startup
    this.loadjQuery(semanticbookmarks);
  },
  onLike : function(e) {
    var firefox = Components.classes["@mozilla.org/appshell/window-mediator;1"].getService(Components.interfaces.nsIWindowMediator);
    var mainwindow = firefox.getMostRecentWindow("navigator:browser");
    this.addFact(mainwindow.content.document, "personlikeswebpage");
  },
  onDislike : function(e) {
    var firefox = Components.classes["@mozilla.org/appshell/window-mediator;1"].getService(Components.interfaces.nsIWindowMediator);
    var mainwindow = firefox.getMostRecentWindow("navigator:browser");
    this.addFact(mainwindow.content.document, "persondislikeswebpage");
  }, 
  onShowPages : function(e) {
    tools.openTab("chrome://semanticbookmarks/content/pages.html");
  },
  addFact : function(document, factId) {
    // init jQuery
    var jQuery = semanticbookmarks.jQuery;
    var $ = function(selector,context){
       return new  jQuery.fn.init(selector,context||window._content.document);
    };
    $.fn = $.prototype = jQuery.fn;
    semanticbookmarks.env=window._content.document;

    var rdf = "<?xml version='1.0' encoding='UTF-8' ?>";
    rdf += "<rdf:RDF xmlns:rdf='http://www.w3.org/1999/02/22-rdf-syntax-ns#' xmlns:sorg='http://schema.org/'>";
    rdf += "<rdf:Description rdf:about='" + document.documentURI + "' ";
    rdf += "rdf:type='http://schema.org/WebPage'>";
    rdf += "<sorg:url>" + document.documentURI + "</sorg:url>";
    rdf += "</rdf:Description>";
    rdf += "</rdf:RDF>";

    var prefManager = Components.classes["@mozilla.org/preferences-service;1"].getService(Components.interfaces.nsIPrefBranch);

    jQuery.ajax( {
      type : "POST",
      url : prefManager.getCharPref("extensions.semanticbookmarks.server") + "/entityhub/entity?update=true",
      contentType : "application/rdf+xml",
      data : rdf,
      cache : false,
      success : function(data, textStatus, jqXHR) {
        var fact = {
          "@profile" : factId,
          "person" : { "@iri" : prefManager.getCharPref("extensions.semanticbookmarks.user_uri") },
          "page"   : { "@iri" : document.documentURI }
        };

        jQuery.ajax( {
          type : "POST",
          url : prefManager.getCharPref("extensions.semanticbookmarks.server") + "/factstore/facts",
          contentType : "application/json",
          data : JSON.stringify(fact),
          cache : false,
          error : function(jqXHR, textStatus, errorThrown) {
            alert(("Error storing fact " + factId + "\n" + jqXHR.responseText));
          }
        });

      },
      error : function(jqXHR, textStatus, errorThrown) {
        alert("Error creating web page entity.\n" + jqXHR.responseText);
      }
    });
  },  
  loadjQuery : function(context) {
    var loader = Components.classes["@mozilla.org/moz/jssubscript-loader;1"].getService(Components.interfaces.mozIJSSubScriptLoader);
    loader.loadSubScript("chrome://semanticbookmarks/content/jquery.js",context);

    var jQuery = window.jQuery.noConflict(true);
        if( typeof(jQuery.fn._init) == 'undefined') { jQuery.fn._init = jQuery.fn.init; }
    semanticbookmarks.jQuery = jQuery;    
  }
};

var tools = {
  openTab : function(url) {
    var wm = Components.classes["@mozilla.org/appshell/window-mediator;1"]
                       .getService(Components.interfaces.nsIWindowMediator);
    var browserEnumerator = wm.getEnumerator("navigator:browser");

    var found = false;
    while (!found && browserEnumerator.hasMoreElements()) {
      var browserWin = browserEnumerator.getNext();
      var tabbrowser = browserWin.gBrowser;

      // Check each tab of this browser instance
      var numTabs = tabbrowser.browsers.length;
      for (var index = 0; index < numTabs; index++) {
        var currentBrowser = tabbrowser.getBrowserAtIndex(index);
        if (url == currentBrowser.currentURI.spec) {

          // The URL is already opened. Select this tab.
          tabbrowser.selectedTab = tabbrowser.tabContainer.childNodes[index];

          // Focus *this* browser-window
          browserWin.focus();
          tabbrowser.reloadTab(tabbrowser.selectedTab);
          found = true;
          break;
        }
      }
    }

    // Our URL isn't open. Open it now.
    if (!found) {
      var recentWindow = wm.getMostRecentWindow("navigator:browser");
      if (recentWindow) {

        var newTabBrowser = recentWindow.gBrowser.addTab(url);
        recentWindow.gBrowser.selectedTab = newTabBrowser;
      }
      else {
        // No browser windows are open, so open a new one.
        window.open(url);
      }
    }
  }
};

window.addEventListener("load", function(e) {
  semanticbookmarks.onInit();
}, false);

