var assap = {
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
    for (var i = 0; i < data['@'].length; i++) {
      var item = data['@'][i];
      
      var enhancementPattern = /\<urn:enhancement.*/i;
      if (item['@'].match(enhancementPattern)) {
        if (item['http://purl.org/dc/terms/type'] == '<http://dbpedia.org/ontology/Person>') {
          persons.push(item['http://fise.iks-project.eu/ontology/selected-text']);
        }
        if (item['http://purl.org/dc/terms/type'] == '<http://dbpedia.org/ontology/Organisation>') {
          organisations.push(item['http://fise.iks-project.eu/ontology/selected-text']);
        }
        if (item['http://purl.org/dc/terms/type'] == '<http://dbpedia.org/ontology/Place>') {
          places.push(item['http://fise.iks-project.eu/ontology/selected-text']);
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
