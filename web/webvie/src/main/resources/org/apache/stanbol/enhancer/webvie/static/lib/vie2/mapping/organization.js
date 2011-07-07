// File:   organization.js
// Author: <a href="mailto:sebastian.germesin@dfki.de">Sebastian Germesin</a>
//

new VIE2.Mapping(
    'organization',  //the id of the mapping
    ['dbpedia:Organisation', 'google:Organization'],  //a list of all types that fall into this category
    ['rdfs:label', 'dbprop:companyName', 'foaf:page', 'foaf:depiction', 'google:flickr'], //a list of default properties
    { //optional options
        namespaces: { //the used namespaces, these can be given here, or placed directly into the HTML document's xmlns attribute.
            'google': 'http://rdf.data-vocabulary.org/#',
            'rdfs'  : 'http://www.w3.org/2000/01/rdf-schema#',
            'dbprop': 'http://dbpedia.org/property/',
            'foaf'  : 'http://xmlns.com/foaf/0.1/',
            'dbonto' : 'http://dbpedia.org/ontology/'
        }
    }
);