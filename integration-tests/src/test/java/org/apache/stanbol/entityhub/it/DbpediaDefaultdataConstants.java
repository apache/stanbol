package org.apache.stanbol.entityhub.it;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class DbpediaDefaultdataConstants {
    
    private DbpediaDefaultdataConstants() { /* no instances */}
    
    public static final String DBPEDIA_SITE_ID = "dbpedia";
    public static final String DBPEDIA_SITE_PATH = "/entityhub/site/"+DBPEDIA_SITE_ID;

    
    public static final Set<String> DBPEDIA_DEFAULTDATA_REQUIRED_FIELDS;
    public static final Set<String> DBPEDIA_DEFAULTDATA_OPTIONAL_FIELDS;
    static {
        Set<String> required = new HashSet<String>();
        required.add("http://www.w3.org/2000/01/rdf-schema#label");
        required.add("http://www.w3.org/1999/02/22-rdf-syntax-ns#type");
        required.add("http://www.iks-project.eu/ontology/rick/model/entityRank");
        DBPEDIA_DEFAULTDATA_REQUIRED_FIELDS = Collections.unmodifiableSet(required);

        Set<String> optional = new HashSet<String>();
        optional.add("http://purl.org/dc/terms/subject");
        optional.add("http://xmlns.com/foaf/0.1/depiction");
        optional.add("http://dbpedia.org/ontology/populationTotal");
        optional.add("http://www.w3.org/2003/01/geo/wgs84_pos#lat");
        optional.add("http://www.w3.org/2003/01/geo/wgs84_pos#long");
        optional.add("http://www.w3.org/2003/01/geo/wgs84_pos#alt");
        optional.add("http://dbpedia.org/ontology/birthDate");
        optional.add("http://dbpedia.org/ontology/deathDate");
        optional.add("http://xmlns.com/foaf/0.1/homepage");
        optional.add("http://www.w3.org/2000/01/rdf-schema#comment");
        DBPEDIA_DEFAULTDATA_OPTIONAL_FIELDS = Collections.unmodifiableSet(optional);
    }


}
