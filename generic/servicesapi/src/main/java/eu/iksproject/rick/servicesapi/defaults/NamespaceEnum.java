package eu.iksproject.rick.servicesapi.defaults;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
/**
 * Defines commonly used name spaces to prevent multiple definitions in several
 * classes
 * @author Rupert Westenthaler
 *
 */
public enum NamespaceEnum {
    //Namespaces defined by RICK
    rickModel("rick","http://www.iks-project.eu/ontology/rick/model/"),
    rickQuery("rick-query","http://www.iks-project.eu/ontology/rick/query/"),


    //First the XML Namespaces
    xsd("http://www.w3.org/2001/XMLSchema#"),
    xsi("http://www.w3.org/2001/XMLSchema-instance#"),
    xml("http://www.w3.org/XML/1998/namespace#"),
    //Start with the semantic Web Namespaces
    rdf("http://www.w3.org/1999/02/22-rdf-syntax-ns#"),
    rdfs("http://www.w3.org/2000/01/rdf-schema#"),
    owl("http://www.w3.org/2002/07/owl#"),
    //CMIS related
    atom("http://www.w3.org/2005/Atom"),
    cmis("http://docs.oasis-open.org/ns/cmis/core/200908/"),
    cmisRa("cmis-ra","http://docs.oasis-open.org/ns/cmis/restatom/200908/"),
    //now the JCR related Namespaces
    jcr("jcr","http://www.jcp.org/jcr/1.0/"),
    jcrSv("jcr-sv","http://www.jcp.org/jcr/sv/1.0/"),
    jcrNt("jcr-nt","http://www.jcp.org/jcr/nt/1.0/"),
    jcrMix("jcr-mix","http://www.jcp.org/jcr/mix/1.0/"),
    //Some well known Namespaces of Ontologies
    geo("http://www.w3.org/2003/01/geo/wgs84_pos#"),
    georss("http://www.georss.org/georss/"),
    dcElements("dc-elements","http://purl.org/dc/elements/1.1/"),
    dcTerms("dc","http://purl.org/dc/terms/"), // RICK prefers DC-Terms, therefore use the "dc" prefix for the terms name space
    foaf("http://xmlns.com/foaf/0.1/"),
    vCal("http://www.w3.org/2002/12/cal#"),
    vCard("http://www.w3.org/2001/vcard-rdf/3.0#"),
    skos("http://www.w3.org/2004/02/skos/core#"),
    sioc("http://rdfs.org/sioc/ns#"),
    siocTypes("sioc-types","http://rdfs.org/sioc/types#"),
    bio("dc-bio","http://purl.org/vocab/bio/0.1/"),
    rss("http://purl.org/rss/1.0/"),
    goodRelations("gr","http://purl.org/goodrelations/v1#"),
    //Linked Data Ontologies
    dbpediaOnt("dbp-ont","http://dbpedia.org/ontology/"),
    dbpediaProp("dbp-prop","http://dbpedia.org/property/"),
    geonames("http://www.geonames.org/ontology#"),
    ;
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
    /*
     * ==== Code for Lookup Methods based on Prefix and Namespace ====
     */
    private static Map<String, NamespaceEnum> prefix2Namespace;
    private static Map<String, NamespaceEnum> namespace2Prefix;
    static {
        Map<String,NamespaceEnum> p2n = new HashMap<String, NamespaceEnum>();
        Map<String,NamespaceEnum> n2p = new HashMap<String, NamespaceEnum>();
        //The Exceptions are only thrown to check that this Enum is configured
        //correctly!
        for(NamespaceEnum entry : NamespaceEnum.values()){
            if(p2n.containsKey(entry.getPrefix())){
                throw new IllegalStateException(
                        String.format("Prefix %s used for multiple namespaces: %s and %s",
                                entry.getPrefix(),
                                p2n.get(entry.getPrefix()),
                                entry.getNamespace()));
            } else {
                p2n.put(entry.getPrefix(), entry);
            }
            if(n2p.containsKey(entry.getNamespace())){
                throw new IllegalStateException(
                        String.format("Multiple Prefixs %s and %s for namespaces: %s",
                                entry.getPrefix(),
                                p2n.get(entry.getNamespace()),
                                entry.getNamespace()));
            } else {
                n2p.put(entry.getNamespace(), entry);
            }
        }
        prefix2Namespace = Collections.unmodifiableMap(p2n);
        namespace2Prefix = Collections.unmodifiableMap(n2p);
    }
    /**
     * Getter for the {@link NamespaceEnum} entry based on the string namespace
     * @param namespace the name space
     * @return the {@link NamespaceEnum} entry or <code>null</code> if the prased
     *    namespace is not present
     */
    public static NamespaceEnum forNamespace(String namespace){
        return namespace2Prefix.get(namespace);
    }
    /**
     * Getter for the {@link NamespaceEnum} entry based on the prefix
     * @param prefix the prefix
     * @return the {@link NamespaceEnum} entry or <code>null</code> if the prased
     *    prefix is not present
     */
    public static NamespaceEnum forPrefix(String prefix){
        return prefix2Namespace.get(prefix);
    }
    /**
     * Lookup if the parsed URI uses one of the registered prefixes of this
     * Enumeration. If this is the case, the prefix is replaced by the namespace
     * and the full URI is returned. If no prefix is returned, the
     * parsed URI is returned
     * @param shortUri the short URI
     * @return the full URI if the parsed shortUri uses a prefix defined by this
     * Enumeration. Otherwise the parsed value.
     */
    public static String getFullName(String shortUri){
        if(shortUri == null) return null;
        int index = shortUri.indexOf(':');
        if(index>0){
            NamespaceEnum namespace = NamespaceEnum.forPrefix(shortUri.substring(0, index));
            if(namespace!= null){
                shortUri = namespace.getNamespace()+shortUri.substring(index+1);
            }
        }
        return shortUri;
    }
}
