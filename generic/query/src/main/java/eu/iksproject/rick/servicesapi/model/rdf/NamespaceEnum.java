package eu.iksproject.rick.servicesapi.model.rdf;

public enum NamespaceEnum {
    rick("http://www.iks-project.eu/ontology/rick"),
//    dbpedia_ont("dbpedia-ont","http://dbpedia.org/ontology/"),
    rdf("http://www.w3.org/1999/02/22-rdf-syntax-ns#"),
    rdfs("http://www.w3.org/2000/01/rdf-schema#"),
    dc("http://purl.org/dc/terms/"),
    skos("http://www.w3.org/2004/02/skos/core#"),
//    foaf("http://xmlns.com/foaf/0.1/"),
    geonames("http://www.geonames.org/ontology#"),
    georss("http://www.georss.org/georss/"),
    geo("http://www.w3.org/2003/01/geo/wgs84_pos#");
    String ns;
    String prefix;
    private NamespaceEnum(String ns) {
        if(ns == null){
            throw new IllegalArgumentException("The namespace MUST NOT be NULL");
        }
        this.ns = ns;
    }
    private NamespaceEnum(String prefix,String ns) {
        this(ns);
        this.prefix = prefix;
    }
    public String getNamespace(){
        return ns;
    }
    public String getPrefix(){
        return prefix == null ? name() : prefix;
    }
    @Override
    public String toString() {
        return ns;
    }

}
