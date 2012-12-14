package org.apache.stanbol.entityhub.indexing.geonames;

import java.util.Map;
import java.util.TreeMap;

import org.apache.stanbol.entityhub.core.model.InMemoryValueFactory;
import org.apache.stanbol.entityhub.servicesapi.model.Reference;
import org.apache.stanbol.entityhub.servicesapi.model.ValueFactory;

public final class GeonamesConstants {

    /**
     * The default directory name used to search for geonames data (currently
     * directly the indexing/resource folder
     */
    public static final String DEFAULT_SOURCE_FOLDER_NAME = "";

    private GeonamesConstants() {}
    public static final String GEONAMES_RESOURCE_NS = "http://sws.geonames.org/";
    public static final String GEONAMES_ONTOLOGY_NS = "http://www.geonames.org/ontology#";

    public static final ValueFactory valueFactory = InMemoryValueFactory.getInstance();
    /**
     * Caches {@link Reference} instances to the controlled vocabulary defined by
     * the Geonamses Ontology
     */
    private static final Map<String,Reference> indexDocRefs = new TreeMap<String, Reference>();
    
    /**
     * Caches references for parsed strings. Intended to be used for
     * Entiries of the geonames ontology.
     * @param refString the string reference
     * @return the {@link Reference} for the parsed String
     */
    public static Reference getReference(String refString){
        Reference ref = indexDocRefs.get(refString);
        if(ref == null){
            ref = valueFactory.createReference(refString);
            indexDocRefs.put(refString, ref);
        }
        return ref;
    }
}