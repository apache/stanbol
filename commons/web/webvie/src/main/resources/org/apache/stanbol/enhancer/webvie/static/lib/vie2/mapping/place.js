// File:   place.js
// Author: <a href="mailto:sebastian.germesin@dfki.de">Sebastian Germesin</a>
//

new VIE2.Mapping(
    'place',  //the id of the mapping 
    ['dbonto:Place', 'cm:Facility'],  //a list of all types that fall into this category
    ['rdfs:label', 'foaf:name', 'foaf:page', 'foaf:depiction', 'geo:long', 'geo:lat'], //a list of default properties
    {// optional options
        namespaces: { //the used namespaces, these can be given here, or placed directly into the HTML document's xmlns attribute.
            'rdfs'       : 'http://www.w3.org/2000/01/rdf-schema#',
            'foaf'       : 'http://xmlns.com/foaf/0.1/',
            'dbonto'     : 'http://dbpedia.org/ontology/',
            'cm'         : 'http://s.opencalais.com/1/type/em/e/',
            'z'          : 'http://s.zemanta.com/ns#',
            'geo'        : 'http://www.w3.org/2003/01/geo/wgs84_pos#'
        }
    }
);