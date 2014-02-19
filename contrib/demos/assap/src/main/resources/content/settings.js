var assap = {
  onSettingsAccept: function() {
     var prefManager = Components.classes["@mozilla.org/preferences-service;1"].getService(Components.interfaces.nsIPrefBranch);

     prefManager.setCharPref("extensions.assap.server", document.getElementById("settings_server").value);
     prefManager.setBoolPref("extensions.assap.service_automatic", document.getElementById("settings_service_automatic").checked);
//     prefManager.setBoolPref("extensions.assap.money", document.getElementById("settings_money").checked);
//     prefManager.setBoolPref("extensions.assap.revenue", document.getElementById("settings_revenue").checked);
//     prefManager.setBoolPref("extensions.assap.time", document.getElementById("settings_time").checked);
//     prefManager.setBoolPref("extensions.assap.market", document.getElementById("settings_market").checked);
//     prefManager.setBoolPref("extensions.assap.organization", document.getElementById("settings_organization").checked);

     window.close();
  },
  onSettingsCancel: function() {
     window.close();
  },
  onSettingsLoad: function() {
    var prefManager = Components.classes["@mozilla.org/preferences-service;1"].getService(Components.interfaces.nsIPrefBranch);

    document.getElementById("settings_server").value = prefManager.getCharPref("extensions.assap.server");
    document.getElementById("settings_service_automatic").checked = prefManager.getBoolPref("extensions.assap.service_automatic");
//    document.getElementById("settings_money").checked = prefManager.getBoolPref("extensions.assap.money");
//    document.getElementById("settings_revenue").checked = prefManager.getBoolPref("extensions.assap.revenue");
//    document.getElementById("settings_time").checked = prefManager.getBoolPref("extensions.assap.time");
    //document.getElementById("settings_market").checked = prefManager.getBoolPref("extensions.assap.market"); 
    //document.getElementById("settings_organization").checked = prefManager.getBoolPref("extensions.assap.organization");
  }
};
