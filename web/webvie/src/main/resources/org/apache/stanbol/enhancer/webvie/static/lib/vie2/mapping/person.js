// File:   person.js
// Author: <a href="mailto:sebastian.germesin@dfki.de">Sebastian Germesin</a>
//

new VIE2.Mapping(
    'person',  //the id of the mapping 
    ['foaf:Person', 'dbonto:Person', 'cm:Person', 'yago:LivingPeople'],  //a list of all types that fall into this category
    ['rdfs:label', 'foaf:name', 'c:name', 'foaf:page', 'foaf:depiction'], //a list of default properties
    {// optional options
        namespaces: { //the used namespaces, these can be given here, or placed directly into the HTML document's xmlns attribute.
            'rdfs'   : 'http://www.w3.org/2000/01/rdf-schema#',
            'foaf'   : 'http://xmlns.com/foaf/0.1/',
            'dbonto' : 'http://dbpedia.org/ontology/',
            'cm'     : 'http://s.opencalais.com/1/type/em/e/',
            'c' : 'http://s.opencalais.com/1/pred/',
            'yago' : 'http://dbpedia.org/class/yago/'
        }
    }
);
