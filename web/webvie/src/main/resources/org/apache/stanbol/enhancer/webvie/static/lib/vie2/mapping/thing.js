// File:   thing.js
// Author: <a href="mailto:sebastian.germesin@dfki.de">Sebastian Germesin</a>
//

new VIE2.Mapping(
    'thing',  //the id of the mapping 
    ['owl:Thing'],  //a list of all types that fall into this category
    [], //a list of default properties
    {// optional options
        namespaces: { //the used namespaces, these can be given here, or placed directly into the HTML document's xmlns attribute.
            'owl'   : 'http://www.w3.org/2002/07/owl#'
        }
    }
);


