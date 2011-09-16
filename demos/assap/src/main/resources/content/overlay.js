var assap = {
  onInit : function() {
    if (!this.initialized) {
      this.initialized = true;
      this.strings = document.getElementById("assap-strings");
      gBrowser.addEventListener("load", function(e) {
        assap.onLoad(e);
      }, true);
      var prefManager = Components.classes["@mozilla.org/preferences-service;1"]
          .getService(Components.interfaces.nsIPrefBranch);

      if (prefManager.getBoolPref("extensions.assap.startup") == false) {
        prefManager.setCharPref("extensions.assap.server", "http://localhost:8080/engines");
        prefManager.setBoolPref("extensions.assap.service_automatic", false);
        prefManager.setBoolPref("extensions.assap.money", true);
        prefManager.setBoolPref("extensions.assap.revenue", true);
        prefManager.setBoolPref("extensions.assap.time", true);
        prefManager.setBoolPref("extensions.assap.startup", true);
        prefManager.setCharPref("extensions.assap.email", "");
      }
    }
  },
  /* Auto analysis on startup */
  onLoad : function(e) {
    var document = e.originalTarget;
    if (document instanceof HTMLDocument) {
      var prefManager = Components.classes["@mozilla.org/preferences-service;1"]
          .getService(Components.interfaces.nsIPrefBranch);
      if (prefManager.getBoolPref("extensions.assap.service_automatic")) {
        this.analyseDocument(document);
      }
    }
  },
  /* Determines current window and starts analysis */
  onManualAnalyse : function(e) {
    var firefox = Components.classes["@mozilla.org/appshell/window-mediator;1"]
        .getService(Components.interfaces.nsIWindowMediator);
    var mainwindow = firefox.getMostRecentWindow("navigator:browser");
    this.analyseDocument(mainwindow.content.document);
  },
  /* Send document to Stanbol */
  analyseDocument : function(document) {
    var input_data = extractTextContent(document.body);
    if ((input_data != "") && (document.contentType == "text/html")) {
      var prefManager = Components.classes["@mozilla.org/preferences-service;1"]
          .getService(Components.interfaces.nsIPrefBranch);

      /* Call Stanbol Enhancement Engines */
      jQuery.ajax( {
        type : "POST",
        url : prefManager.getCharPref("extensions.assap.server"),
        dataType : "application/json",
        contentType : "text/plain",
        data : input_data,
        cache : false,
        success : function(data, textStatus, jqXHR) {
          window.openDialog('chrome://assap/content/results.xul', 'Annotation Results', 'centerscreen,chrome,dependent=yes,outerHeight=600,outerWidth=500', data);
        },
        error : function(jqXHR, textStatus, errorThrown) {
          alert(("Error loading semantic annotations.\n" + jqXHR.statusText + "\n" + jqXHR.responseText));
        }
      });

    }
  }
};

window.addEventListener("load", function(e) {
  assap.onInit();
}, false);
