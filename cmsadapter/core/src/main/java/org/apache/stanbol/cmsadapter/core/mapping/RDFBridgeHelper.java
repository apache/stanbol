package org.apache.stanbol.cmsadapter.core.mapping;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.jcr.PropertyType;

import org.apache.clerezza.rdf.core.Literal;
import org.apache.clerezza.rdf.core.MGraph;
import org.apache.clerezza.rdf.core.NonLiteral;
import org.apache.clerezza.rdf.core.Resource;
import org.apache.clerezza.rdf.core.Triple;
import org.apache.clerezza.rdf.core.TypedLiteral;
import org.apache.clerezza.rdf.core.UriRef;
import org.apache.stanbol.cmsadapter.servicesapi.helper.CMSAdapterVocabulary;
import org.apache.stanbol.entityhub.servicesapi.defaults.NamespaceEnum;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Provides utility classes that are used parsing the RDF data
 * 
 * @author suat
 * 
 */
public class RDFBridgeHelper {
    private static final Logger log = LoggerFactory.getLogger(RDFBridgeHelper.class);

    public static final UriRef RDF_TYPE = new UriRef(NamespaceEnum.rdf + "Type");

    private static final UriRef base64Uri = dataTypeURI("base64Binary");
    private static final UriRef dateTimeUri = dataTypeURI("dateTime");
    private static final UriRef booleanUri = dataTypeURI("boolean");
    private static final UriRef stringUri = dataTypeURI("string");
    private static final UriRef xsdInteger = dataTypeURI("integer");
    private static final UriRef xsdInt = dataTypeURI("int");
    private static final UriRef xsdShort = dataTypeURI("short");
    private static final UriRef xsdLong = dataTypeURI("long");
    private static final UriRef xsdDouble = dataTypeURI("double");
    private static final UriRef xsdAnyURI = dataTypeURI("anyURI");

    /**
     * Gets a list of {@link NonLiteral} which indicates URIs of resources representing the root objects in
     * the graph e.g the object that do not have {@code CMSAdapterVocabulary#CMS_OBJECT_PARENT_REF} property
     * 
     * @param annotatedGraph
     * @return
     */
    public static List<NonLiteral> getRootObjetsOfGraph(MGraph annotatedGraph) {
        List<NonLiteral> roots = new ArrayList<NonLiteral>();
        Iterator<Triple> it = annotatedGraph.filter(null, RDF_TYPE, CMSAdapterVocabulary.CMS_OBJECT);
        while (it.hasNext()) {
            Triple t = it.next();
            if (isRoot(t, annotatedGraph)) {
                roots.add(t.getSubject());
            }
        }
        return roots;
    }

    public static List<NonLiteral> getRootObjectsOfGraph(MGraph annotatedGraph, List<NonLiteral> candidates) {
        List<NonLiteral> roots = new ArrayList<NonLiteral>();
        Iterator<Triple> it = annotatedGraph.filter(null, RDF_TYPE, CMSAdapterVocabulary.CMS_OBJECT);
        while (it.hasNext()) {
            Triple t = it.next();
            if (isRoot(t, annotatedGraph) && candidates.contains(t.getSubject())) {
                roots.add(t.getSubject());
            }
        }
        return roots;
    }

    private static boolean isRoot(Triple cmsObjectTriple, MGraph graph) {
        NonLiteral subject = cmsObjectTriple.getSubject();
        if (graph.filter(subject, CMSAdapterVocabulary.CMS_OBJECT_PARENT_REF, null).hasNext()) {
            return false;
        } else {
            return true;
        }
    }

    /**
     * Gets {@link Resource} of {@code Triple} which is specified the <code>subject</code> and
     * <code>propName</code> parameters
     * 
     * @param subject
     * @param propName
     * @param graph
     * @return specified resource if it exists, otherwise it returns <code>null</code>
     */
    public static Resource getResource(NonLiteral subject, UriRef propName, MGraph graph) {
        Iterator<Triple> it = graph.filter(subject, propName, null);
        if (it.hasNext()) {
            return it.next().getObject();
        } else {
            log.warn("No triple for subject: {} and property: {}", subject, propName);
            return null;
        }
    }

    /**
     * Gets lexical form of {@code Triple} which is specified the <code>subject</code> and
     * <code>propName</code> parameters if the target resource is an instance of {@link Literal}.
     * 
     * @param subject
     * @param propName
     * @param graph
     * @return lexical value of specified resource it exists and an instance of {@link Literal}, otherwise it
     *         returns empty string
     */
    public static String getResourceStringValue(NonLiteral subject, UriRef propName, MGraph graph) {
        Resource r = getResource(subject, propName, graph);
        if (r != null) {
            if (r instanceof Literal) {
                return ((Literal) r).getLexicalForm();
            } else {
                log.warn("Resource value is not a Literal for subject: {} and property: {}", r);
                return "";
            }
        } else {
            return "";
        }
    }

    /**
     * Gets {@link UriRef} from the {@link Resource} of {@link Triple} which is specified the
     * <code>subject</code> and <code>propName</code> parameters if the target resource is an instance of
     * {@link UriRef}.
     * 
     * @param subject
     * @param propName
     * @param graph
     * @return {@link UriRef} of resource if it exists and is instance of {@link UriRef}
     */
    public static UriRef getResourceURIValue(NonLiteral subject, UriRef propName, MGraph graph) {
        Resource r = getResource(subject, propName, graph);
        if (r != null) {
            if (r instanceof UriRef) {
                return new UriRef(removeEndCharacters(r.toString()));
            } else {
                log.warn("Resource value is not a UriRef for subject: {} and property: {}", r);
                return null;
            }
        } else {
            return null;
        }
    }

    /**
     * Remove first {@link Triple} specified with <code>subject</code> and <code>predicate</code> parameters
     * from the specified {@link MGraph}
     * 
     * @param subject
     *            {@link NonLiteral} subject of triple to be deleted
     * @param predicate
     *            {@link UriRef} predicate of triple to be deleted
     * @param mGraph
     *            {@link MGraph} from where the triple to be deleted
     */
    public static void removeExistingTriple(NonLiteral subject, UriRef predicate, MGraph mGraph) {
        Iterator<Triple> it = mGraph.filter(subject, predicate, null);
        if (it.hasNext()) {
            mGraph.remove(it.next());
        }
    }

    /**
     * Removes <b>&lt;</b> and <b>&gt;</b> characters from start and end of the string respectively
     * 
     * @param resource
     * @return
     */
    public static String removeEndCharacters(String resource) {
        return resource.replace("<", "").replace(">", "");
    }

    /**
     * Return related {@link PropertyType} according to data type of a {@link Resource} if it is an instance
     * of {@link TypedLiteral} ot {@link UriRef}, otherwise it return {@code PropertyType#STRING} as default
     * type.
     * 
     * @param r
     * @link {@link Resource} instance of which property type is demanded
     * @return related {@link PropertyType}
     */
    public static int getPropertyType(Resource r) {
        if (r instanceof TypedLiteral) {
            UriRef type = ((TypedLiteral) r).getDataType();
            if (type.equals(stringUri)) {
                return PropertyType.STRING;
            } else if (type.equals(base64Uri)) {
                return PropertyType.BINARY;
            } else if (type.equals(booleanUri)) {
                return PropertyType.BOOLEAN;
            } else if (type.equals(dateTimeUri)) {
                return PropertyType.DATE;
            } else if (type.equals(xsdAnyURI)) {
                return PropertyType.URI;
            } else if (type.equals(xsdDouble)) {
                return PropertyType.DOUBLE;
            } else if (type.equals(xsdInt)) {
                return PropertyType.DECIMAL;
            } else if (type.equals(xsdInteger)) {
                return PropertyType.DECIMAL;
            } else if (type.equals(xsdLong)) {
                return PropertyType.LONG;
            } else if (type.equals(xsdShort)) {
                return PropertyType.DECIMAL;
            } else {
                return PropertyType.STRING;
            }
        } else if (r instanceof UriRef) {
            return PropertyType.URI;
        } else {
            return PropertyType.STRING;
        }
    }

    private static UriRef dataTypeURI(String type) {
        return new UriRef(NamespaceEnum.xsd + type);
    }
}
