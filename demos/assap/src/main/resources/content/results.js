var assap = {
  onResultsCancel: function() {
    window.close();
  },
  onResultsLoad: function() {
    var data = window.arguments[0];
    
    var rawResultDoc = document.getElementById("raw_result_frame").contentDocument; 
    rawResultDoc.getElementById("raw_results").innerHTML = data.toString();
  }
};
