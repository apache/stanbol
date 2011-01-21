package org.apache.stanbol.entityhub.core.utils;

import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

import javax.xml.namespace.QName;

import org.apache.stanbol.entityhub.core.model.DefaultEntityMappingImpl;
import org.apache.stanbol.entityhub.core.model.DefaultSignImpl;
import org.apache.stanbol.entityhub.core.model.DefaultSymbolImpl;
import org.apache.stanbol.entityhub.servicesapi.defaults.NamespaceEnum;
import org.apache.stanbol.entityhub.servicesapi.model.Reference;
import org.apache.stanbol.entityhub.servicesapi.model.Representation;
import org.apache.stanbol.entityhub.servicesapi.model.Sign;
import org.apache.stanbol.entityhub.servicesapi.model.ValueFactory;
import org.apache.stanbol.entityhub.servicesapi.model.Sign.SignTypeEnum;
import org.apache.stanbol.entityhub.servicesapi.model.rdf.RdfResourceEnum;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Utilities useful for implementations of the Entityhub Model
 * @author Rupert Westenthaler
 *
 */
public final class ModelUtils {

    private static final Logger log = LoggerFactory.getLogger(ModelUtils.class);

    /**
     * Holds the uri->RepresentationTypeEnum mappings
     */
    public static final Map<String,SignTypeEnum> REPRESENTATION_TYPE_MAPPING;
    static {
        Map<String,SignTypeEnum> repTypeMapping = new HashMap<String, SignTypeEnum>();
        for(SignTypeEnum type : SignTypeEnum.values()){
            repTypeMapping.put(type.getUri(), type);
        }
        REPRESENTATION_TYPE_MAPPING = Collections.unmodifiableMap(repTypeMapping);
    }
    /**
     * Getter for the {@link SignTypeEnum} based on the reference
     * @param referece The reference as defined by {@link SignTypeEnum#getUri()}
     * @return the type or <code>null</code> if no mapping is present for the parsed
     * reference.
     */
    public static SignTypeEnum getSignType(Reference reference){
        return reference == null?null:getSignType(reference.getReference());
    }
    /**
     * Getter for the {@link SignTypeEnum} based on the uri
     * @param uri The uri as defined by {@link SignTypeEnum#getUri()}
     * @return the type or <code>null</code> if no mapping is present for the parsed
     * uri.
     */
    public static SignTypeEnum getSignType(String uri){
        return REPRESENTATION_TYPE_MAPPING.get(uri);
    }
    /**
     * Random UUID generator with re-seedable RNG for the tests.
     *
     * @return a new Random UUID
     */
    protected static Random rng = new Random();

    /**
     * Do not allow instances of this class
     */
    private ModelUtils(){}

    /**
     * TODO: Maybe we need a better way to generate unique IDs
     * @return
     */
    public static UUID randomUUID() {
        return new UUID(rng.nextLong(), rng.nextLong());
    }

    public static void setSeed(long seed) {
        rng.setSeed(seed);
    }

    /**
     * Processes a value parsed as object to the representation.
     * This processing includes:
     * <ul>
     * <li> Removal of <code>null</code> values
     * <li> Converting URIs and URLs to {@link Reference}
     * <li> Converting String[] with at least a single entry where the first
     * entry is not null to {@link Text} (the second entry is used as language.
     * Further entries are ignored.
     * <li> Recursive calling of this Method if a {@link Iterable} (any Array or
     *      {@link Collection}), {@link Iterator} or {@link Enumeration} is parsed.
     * <li> All other Objects are added to the result list
     * </ul>
     * TODO: Maybe we need to enable an option to throw {@link IllegalArgumentException}
     * in case any of the parsed values is invalid. Currently invalid values are
     * just ignored.
     * @param value the value to parse
     * @param results the collections the results of the parsing are added to.
     */
    public static void checkValues(ValueFactory valueFactory, Object value,Collection<Object> results){
        if(value == null){
            return;
        } else if(value instanceof Iterable<?>){
            for(Object current : (Iterable<?>)value){
                checkValues(valueFactory,current,results);
            }
        } else if(value instanceof Iterator<?>){
            while(((Iterator<?>)value).hasNext()){
                checkValues(valueFactory,((Iterator<?>)value).next(),results);
            }
        } else if(value instanceof Enumeration<?>){
            while(((Enumeration<?>)value).hasMoreElements()){
                checkValues(valueFactory,((Enumeration<?>)value).nextElement(),results);
            }
        } else if(value instanceof URI || value instanceof URL){
            results.add(valueFactory.createReference(value.toString()));
        } else if(value instanceof String[]){
            if(((String[])value).length>0 && ((String[])value)[0] != null){
                results.add(valueFactory.createText(((String[])value)[0],
                        ((String[])value).length>1?((String[])value)[1]:null));
            } else {
                log.warn("String[] "+Arrays.toString((String[])value)+" is not a valied natural language array! -> ignore value");
            }
        } else {
            results.add(value);
        }
    }
    public static String getRepresentationInfo(Representation rep) {
        StringBuilder info = new StringBuilder();
        info.append("Representation id=");
        info.append(rep.getId());
        info.append(" | impl=");
        info.append(rep.getClass());
        info.append('\n');
        for(Iterator<String> fields = rep.getFieldNames();fields.hasNext();){
            String field = fields.next();
            info.append(" o ");
            info.append(field);
            info.append(':');
            Collection<Object> values = new ArrayList<Object>();
            for(Iterator<Object> valueIt = rep.get(field);valueIt.hasNext();){
                values.add(valueIt.next());
            }
            info.append(values);
            info.append('\n');
        }
        return info.toString();
    }
    public static <T> Collection<T> asCollection(Iterator<T> it){
        Collection<T> c = new ArrayList<T>();
        while(it.hasNext()){
            c.add(it.next());
        }
        return c;
    };

    /**
     * Splits up a URI in local name and namespace based on the following rules
     * <ul>
     * <li> If URI starts with "urn:" and last index of ':' == 3 than the there
     *      is no namespace and the whole URI is a local name
     * <li> if the uri starts with "urn:" and the last index of ':' ia > 3, than
     *      the last index ':' is used.
     * <li> split by the last index of '#' if index >= 0
     * <li> split by the last index of '/' if index >= 0
     * <li> return after the first split
     * <li> return the whole URI as local name if no split was performed.
     * </ul>
     * @param uri The uri
     * @return A array with two fields. In the first the namespace is stored (
     * might be <code>null</code>. In the second the local name is stored.
     */
    public static String[] getNamespaceLocalName(String uri){
        String[] parts = new String[2];
        if(uri.startsWith("urn:")){
            if(uri.lastIndexOf(':')>3){
                parts[1] = uri.substring(uri.lastIndexOf(":")+1);
                parts[0] = uri.substring(0, uri.lastIndexOf(":")+1);
            } else {
                parts[1] = uri;
                parts[0] = null;
            }
        } else if(uri.lastIndexOf("#")>=0){
            parts[1] = uri.substring(uri.lastIndexOf("#")+1);
            parts[0] = uri.substring(0, uri.lastIndexOf("#")+1);
        } else if(uri.lastIndexOf("/")>=0){
            parts[1] = uri.substring(uri.lastIndexOf("/")+1);
            parts[0] = uri.substring(0, uri.lastIndexOf("/")+1);
        } else {
            parts[0] = null;
            parts[1] = uri;
        }
        return parts;
    }
    /**
     * This Method uses {@link #getNamespaceLocalName(String)} to split up
     * namespace and local name. It uses also the Data in the
     * {@link NamespaceEnum} to retrieve prefixes for Namespaces.
     * @param uri the URI
     * @return the QName
     */
    public static QName getQName(String uri){
        String[] nsln = getNamespaceLocalName(uri);
        if(nsln[0] != null){
            NamespaceEnum entry = NamespaceEnum.forNamespace(nsln[0]);
            if(entry != null){
                return new QName(nsln[0], nsln[1],entry.getPrefix());
            } else {
                return new QName(nsln[0], nsln[1]);
            }
        } else {
            return new QName(nsln[1]);
        }
    }
    /**
     * Getter for the SignType for a Representation. If the Representation does
     * not define a value part of the {@link SignTypeEnum} for the field
     * {@link RdfResourceEnum#signType} ({@value RdfResourceEnum#signType}), that
     * the default sign type {@link Sign#DEFAULT_SIGN_TYPE} is returned.
     * @param representation The representation
     * @return the sign type
     * @throws IllegalArgumentException if <code>null</code> is parsed as representation!
     */
    public static SignTypeEnum getSignType(Representation representation) throws IllegalArgumentException {
        if(representation == null){
            throw new IllegalArgumentException("Parameter represetnation MUST NOT be NULL!");
        }
        Reference ref = representation.getFirstReference(RdfResourceEnum.signType.getUri());
        if(ref == null){
            return Sign.DEFAULT_SIGN_TYPE;
        } else {
            SignTypeEnum type = ModelUtils.getSignType(ref.getReference());
            if(type == null){
                log.warn("Sign "+representation.getId()+" is set to an unknown SignType "+ref.getReference()+"! -> return default type (value is not reseted)");
                return Sign.DEFAULT_SIGN_TYPE;
            } else {
                return type;
            }
        }
    }
    /**
     * Creates a Sign for the parsed Representation and the signSite id
     * @param rep the Represetnation
     * @param signSite the id of the site for the sign
     * @return the sign
     * @throws IllegalArgumentException if any of the two parameter is <code>null</code>.
     */
    public static Sign createSign(Representation rep,String signSite) throws IllegalArgumentException {
        if(rep == null){
            throw new IllegalArgumentException("The parsed Representation MUST NOT be NULL!");
        }
        if(signSite == null){
            throw new IllegalArgumentException("The parsed ID of the SignSite MUST NOT be NULL!");
        }
        rep.setReference(Sign.SIGN_SITE, signSite);
        SignTypeEnum signType = ModelUtils.getSignType(rep);
        //instantiate the correct Sign Implementation
        Sign sign;
        /*
         * TODO: change this part to separate the implementation of the
         * ReferencedSite with the instantiation of Sign Type Implementations
         * Maybe introduce an SignFactory or add such Methods to the
         * existing ValueFactory
         */
        switch (signType) {
        case Symbol:
            sign = new DefaultSymbolImpl(signSite,rep);
            break;
        case EntityMapping:
            sign = new DefaultEntityMappingImpl(signSite,rep);
            break;
        case Sign:
            sign = new DefaultSignImpl(signSite,rep);
            break;
        default:
            log.warn("Unsupported SignType "+signType.getUri()+" (create Sign instance). Please adapt this implementation!");
            sign = new DefaultSignImpl(signSite,rep);
            break;
        }
        return sign;
    }
}
