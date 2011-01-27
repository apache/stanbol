package org.apache.stanbol.enhancer.jersey.writers;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import javax.ws.rs.core.MediaType;

import org.apache.clerezza.rdf.core.BNode;
import org.apache.clerezza.rdf.core.NonLiteral;
import org.apache.clerezza.rdf.core.Triple;
import org.apache.clerezza.rdf.core.TripleCollection;
import org.apache.clerezza.rdf.core.UriRef;
import org.apache.clerezza.rdf.core.serializedform.SerializingProvider;
import org.apache.clerezza.rdf.core.serializedform.SupportedFormat;
import org.apache.stanbol.jsonld.JsonLd;
import org.apache.stanbol.jsonld.JsonLdResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Implements a <a href="http://json-ld.org/">JSON-LD</a> serialization of a Clerezza
 * {@link TripleCollection}.<br>
 * <br>
 * Note: This implementation is based on <a href="http://json-ld.org/spec/">JSON-LD
 * specification</a> draft from 25 October 2010.
 *
 * @author Fabian Christ
 *
 * @scr.component immediate="true"
 * @scr.service
 *                 interface="org.apache.clerezza.rdf.core.serializedform.SerializingProvider"
 */
@SupportedFormat(JsonLdSerializerProvider.SUPPORTED_FORMAT)
public class JsonLdSerializerProvider implements SerializingProvider {

    public static final String SUPPORTED_FORMAT = MediaType.APPLICATION_JSON;

    private static final String RDF_NS_TYPE="http://www.w3.org/1999/02/22-rdf-syntax-ns#type";

    private static final Logger logger = LoggerFactory.getLogger(JsonLdSerializerProvider.class);

    // Map from Namespace -> to Prefix
    private Map<String, String> namespacePrefixMap = new HashMap<String, String>();

    private int indentation = 2;

    @Override
    public void serialize(OutputStream serializedGraph, TripleCollection tc, String formatIdentifier) {
        if (!formatIdentifier.equals(SUPPORTED_FORMAT)) {
            logger.info("serialize() the format '" + formatIdentifier + "' is not supported by this implementation");
            return;
        }

        JsonLd jsonLd = new JsonLd();
        jsonLd.setNamespacePrefixMap(this.namespacePrefixMap);

        Map<NonLiteral, String> subjects = createSubjectsMap(tc);
        for (NonLiteral subject : subjects.keySet()) {
            JsonLdResource resource = new JsonLdResource();
            resource.setSubject(subject.toString());

            Iterator<Triple> triplesFromSubject = tc.filter(subject, null, null);
            while (triplesFromSubject.hasNext()) {
                Triple currentTriple = triplesFromSubject.next();
                if (currentTriple.getPredicate().getUnicodeString().equals(RDF_NS_TYPE)) {
                    if (logger.isDebugEnabled()) {
                        logger.debug("serialize() adding rdf:type: \"a\":" + currentTriple.getObject());
                    }
                    resource.addType(currentTriple.getObject().toString());
                } else {
                    if (logger.isDebugEnabled()) {
                        logger.debug("serializer() adding predicate " + currentTriple.getPredicate().toString() + " with object " + currentTriple.getObject().toString());
                    }

                    String property = currentTriple.getPredicate().getUnicodeString();
                    String value = currentTriple.getObject().toString();
                    resource.putProperty(property, value);
                }
            }

            jsonLd.put(resource.getSubject(), resource);
        }

        try {
            BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(serializedGraph));
            writer.write(jsonLd.toString(this.indentation));
            writer.flush();
        } catch (IOException ioe) {
            logger.error(ioe.getMessage());
            throw new RuntimeException(ioe.getMessage());
        }
    }

    private Map<NonLiteral, String> createSubjectsMap(TripleCollection tc) {
        Map<NonLiteral, String> subjects = new HashMap<NonLiteral, String>();
        int bNodeCounter = 0;
        for (Triple triple : tc) {
            NonLiteral subject = triple.getSubject();
            if (!subjects.containsKey(subject)) {
                if (subject instanceof UriRef) {
                    subjects.put(subject, subject.toString());
                } else if (subject instanceof BNode) {
                    bNodeCounter++;
                    subjects.put(subject, "_:bnode" + bNodeCounter);
                }
            }
        }
        return subjects;
    }

    /**
     * Get the known namespace to prefix mapping.
     *
     * @return A {@link Map} from namespace String to prefix String.
     */
    public Map<String, String> getNamespacePrefixMap() {
        return namespacePrefixMap;
    }

    /**
     * Sets the known namespaces for the serializer.
     *
     * @param knownNamespaces A {@link Map} from namespace String to prefix String.
     */
    public void setNamespacePrefixMap(Map<String, String> knownNamespaces) {
        this.namespacePrefixMap = knownNamespaces;
    }

    /**
     * Returns the current number of space characters which are used
     * to indent the serialized output.
     *
     * @return Number of space characters used for indentation.
     */
    public int getIndentation() {
        return indentation;
    }

    /**
     * Sets the number of characters used per indentation level for the serialized output.<br />
     * Set this value to zero (0) if you don't want indentation. Default value is 2.
     *
     * @param indentation Number of space characters used for indentation.
     */
    public void setIndentation(int indentation) {
        this.indentation = indentation;
    }
}
