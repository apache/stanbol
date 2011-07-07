// File:   task.js
// Author: <a href="mailto:sebastian.germesin@dfki.de">Sebastian Germesin</a>
//

new VIE2.Mapping(
    'task',  //the id of the mapping 
    ['rdfcal:Task'],  //a list of all types that fall into this category
    ['rdfcal:hasAgent', 'rdfcal:name', 'rdfcal:startDate', 'rdfcal:targetDate'], //a list of default properties
    {// optional options
        namespaces: { //the used namespaces, these can be given here, or placed directly into the HTML document's xmlns attribute.
            'rdfcal'   : 'http://www.w3.org/2002/12/cal#'
        }
    }
);