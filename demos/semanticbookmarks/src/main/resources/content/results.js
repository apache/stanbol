var semanticbookmarks = {
  onResultsCancel: function() {
    window.close();
  },
  onResultsLoad: function() {
    var rawdata = window.arguments[0];
    
    var rawResultDoc = document.getElementById("raw_result_frame").contentDocument;
    rawResultDoc.getElementById("raw_results").firstChild.data = rawdata.toString();
      
    // TODO: If the JSON-LD use type CURIEs, those need to be handled.
    var data = JSON.parse(rawdata);
           
    var persons = [];
    var organisations = [];
    var places = [];
    for (var i = 0; i < data['@subject'].length; i++) {
      var item = data['@subject'][i];
      
      if (item['@subject'] == 'enhancement') {
        if (item['type'] == 'Person') {
          persons.push(item['selected-text']);
        }
        if (item['type'] == 'Organisation') {
          organisations.push(item['selected-text']);
        }
        if (item['type'] == 'Place') {
          places.push(item['selected-text']);
        }
      }
    }
    
    var personList = document.getElementById("person_list");
    for (var i=0; i < persons.length; i++) {
      personList.appendItem(persons[i]); 
    }
    
    var organisationList = document.getElementById("organisation_list");
    for (var i=0; i < organisations.length; i++) {
      organisationList.appendItem(organisations[i]); 
    }

    var placeList = document.getElementById("place_list");
    for (var i=0; i < places.length; i++) {
      placeList.appendItem(places[i]); 
    }
    
  }
};
