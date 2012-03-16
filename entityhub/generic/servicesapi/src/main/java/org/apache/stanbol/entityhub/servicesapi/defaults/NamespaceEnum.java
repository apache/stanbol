/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.stanbol.entityhub.servicesapi.defaults;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
/**
 * Defines commonly used name spaces to prevent multiple definitions in several
 * classes
 * @author Rupert Westenthaler
 *
 */
public enum NamespaceEnum {
    
    //Namespaces defined by the entityhub
    entityhubModel("entityhub","http://www.iks-project.eu/ontology/rick/model/"),
    entityhubQuery("entityhub-query","http://www.iks-project.eu/ontology/rick/query/"),


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
    gml("http://www.opengis.net/gml/"),
    dcElements("dc-elements","http://purl.org/dc/elements/1.1/"),
    dcTerms("dc","http://purl.org/dc/terms/"), // Entityhub prefers DC-Terms, therefore use the "dc" prefix for the terms name space
    foaf("http://xmlns.com/foaf/0.1/"),
    vCal("http://www.w3.org/2002/12/cal#"),
    vCard("http://www.w3.org/2001/vcard-rdf/3.0#"),
    skos("http://www.w3.org/2004/02/skos/core#"),
    sioc("http://rdfs.org/sioc/ns#"),
    siocTypes("sioc-types","http://rdfs.org/sioc/types#"),
    bio("dc-bio","http://purl.org/vocab/bio/0.1/"),
    rss("http://purl.org/rss/1.0/"),
    goodRelations("gr","http://purl.org/goodrelations/v1#"),
    swrc("http://swrc.ontoware.org/ontology#"), //The Semantic Web for Research Communities Ontology
    //Linked Data Ontologies
    dbpediaOnt("dbp-ont","http://dbpedia.org/ontology/"),
    dbpediaProp("dbp-prop","http://dbpedia.org/property/"),
    geonames("http://www.geonames.org/ontology#"),
    //copyright and license
    cc("http://creativecommons.org/ns#"),
    //Schema.org (see http://schema.org/docs/schemaorg.owl for the Ontology)
    schema("http://schema.org/",true),
    /**
     * The W3C Ontology for Media Resources http://www.w3.org/TR/mediaont-10/
     */
    media("http://www.w3.org/ns/ma-ont#"),
    /*
     * eHealth domain 
     */
    /**
     * DrugBank is a repository of almost 5000 FDA-approved small molecule and 
     * biotech drugs. 
     */
    drugbank("http://www4.wiwiss.fu-berlin.de/drugbank/resource/drugbank/"),
    /**
     * Dailymed is published by the National Library of Medicine, 
     * and provides high quality information about marketed drugs.
     */
    dailymed("http://www4.wiwiss.fu-berlin.de/dailymed/resource/dailymed/"),
    /**
     * SIDER contains information on marketed drugs and their adverse effects. 
     * The information is extracted from public documents and package inserts.
     */
    sider("http://www4.wiwiss.fu-berlin.de/sider/resource/sider/"),
    /**
     * The Linked Clinical Trials (LinkedCT) project aims at publishing the 
     * first open Semantic Web data source for clinical trials data.
     */
    linkedct("http://data.linkedct.org/resource/linkedct/"),
    /**
     * STITCH contains information on chemicals and proteins as well as their 
     * interactions and links.
     */
    stitch("http://www4.wiwiss.fu-berlin.de/stitch/resource/stitch/"),
    /**
     * Diseasome publishes a network of 4,300 disorders and disease genes linked 
     * by known disorder-gene associations for exploring all known phenotype and 
     * disease gene associations, indicating the common genetic origin of many 
     * diseases.
     */
    diseasome("http://www4.wiwiss.fu-berlin.de/diseasome/resource/diseasome/"),
    /**
     * National Cancer Institute Thesaurus (http://www.mindswap.org/2003/CancerOntology/)
     */
    nci("http://www.mindswap.org/2003/nciOncology.owl#"),
    tcm("http://purl.org/net/tcm/tcm.lifescience.ntu.edu.tw/")
    ;
    /**
     * The logger
     */
    private static final Logger log = LoggerFactory.getLogger(NamespaceEnum.class);

    private final String ns;
    private final String prefix;
    private final boolean defaultPrefix;
    /**
     * Defines a namespace that used the {@link #name()} as prefix.
     * @param ns the namespace. MUST NOT be NULL nor empty
     */
    NamespaceEnum(String ns) {
        this(null,ns,false);
    }
    /**
     * Defines a namespace by using the {@link #name()} as prefix. If
     * <code>true</code> is parsed a second parameter this namespace is marked
     * as the default<p>
     * <b>NOTE: </b> Only a single namespace can be defined as default. In case
     * multiple namespaces are marked as default the one with the lowest
     * {@link #ordinal()} will be used as default. This will be the topmost entry
     * in this enumeration.
     * @param ns the namespace. MUST NOT be <code>null</code> nor empty
     * @param defaultPrefix the default namespace indicator
     */
    NamespaceEnum(String ns,boolean defaultPrefix) {
        this(null,ns,defaultPrefix);
    }
    /**
     * Defines a namespace with a customised prefix. This should be used if the
     * prefix needs to be different as the {@link #name()} of the enumeration
     * entry.
     * @param prefix the prefix. If <code>null</code> the {@link #name()} is
     * used. MUST NOT be an empty string
     * @param ns the namespace. MUST NOT be <code>null</code> nor empty
     */
    NamespaceEnum(String prefix, String ns) {
        this(prefix,ns,false);
    }
    /**
     * Defines a namespace with a customised prefix. This should be used if the
     * prefix needs to be different as the {@link #name()} of the enumeration
     * entry.<p>
     * <b>NOTE: </b> Only a single namespace can be defined as default. In case
     * multiple namespaces are marked as default the one with the lowest
     * {@link #ordinal()} will be used as default. This will be the topmost entry
     * in this enumeration.
     * @param prefix the prefix. If <code>null</code> the {@link #name()} is
     * used. MUST NOT be an empty string
     * @param ns the namespace. MUST NOT be <code>null</code> nor empty
     * @param defaultPrefix the default namespace indicator
     */
    NamespaceEnum(String prefix, String ns,boolean defaultPrefix) {
        if(ns == null || ns.isEmpty()){
            throw new IllegalArgumentException("The namespace MUST NOT be NULL nor empty");
        }
        this.ns = ns;
        if(prefix == null){
            this.prefix = name();
        } else if(prefix.isEmpty()){
            throw new IllegalArgumentException("The prefix MUST NOT be emtpty." +
            		"Use NULL to use the name or parse the prefix to use");
        } else {
            this.prefix = prefix;
        }
        this.defaultPrefix = defaultPrefix;
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
    private final static Map<String, NamespaceEnum> prefix2Namespace;
    private final static Map<String, NamespaceEnum> namespace2Prefix;
    private final static NamespaceEnum defaultNamespace;
    static {
        Map<String,NamespaceEnum> p2n = new HashMap<String, NamespaceEnum>();
        Map<String,NamespaceEnum> n2p = new HashMap<String, NamespaceEnum>();
        //The Exceptions are only thrown to check that this Enum is configured
        //correctly!
        NamespaceEnum defaultNs = null;
        for(NamespaceEnum entry : NamespaceEnum.values()){
            if(entry.isDefault()){
                if(defaultNs == null){
                    defaultNs = entry;
                } else {
                    log.warn("Found multiple default namespace definitions! Will use the one with the lowest ordinal value.");
                    log.warn(" > used default: prefix:{}, namespace:{}, ordinal:{}",
                        new Object[]{defaultNs.getPrefix(),defaultNs.getNamespace(),defaultNs.ordinal()});
                    log.warn(" > this one    : prefix:{}, namespace:{}, ordinal:{}",
                        new Object[]{entry.getPrefix(),entry.getNamespace(),entry.ordinal()});
                }
            }
            if(p2n.containsKey(entry.getPrefix())){
                throw new IllegalStateException(
                        String.format("Prefix %s used for multiple namespaces: %s and %s",
                                entry.getPrefix(),
                                p2n.get(entry.getPrefix()),
                                entry.getNamespace()));
            } else {
                log.debug("add {} -> {} mapping",entry.getPrefix(),entry.getNamespace());
                p2n.put(entry.getPrefix(), entry);
            }
            if(n2p.containsKey(entry.getNamespace())){
                throw new IllegalStateException(
                        String.format("Multiple Prefixs %s and %s for namespaces: %s",
                                entry.getPrefix(),
                                p2n.get(entry.getNamespace()),
                                entry.getNamespace()));
            } else {
                log.debug("add {} -> {} mapping",entry.getNamespace(),entry.getPrefix());
                n2p.put(entry.getNamespace(), entry);
            }
        }
        prefix2Namespace = Collections.unmodifiableMap(p2n);
        namespace2Prefix = Collections.unmodifiableMap(n2p);
        defaultNamespace = defaultNs;
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
     * @param prefix the prefix or <code>null</code> to get the default namespace
     * @return the {@link NamespaceEnum} entry or <code>null</code> if the prased
     *    prefix is not present
     */
    public static NamespaceEnum forPrefix(String prefix){
        return prefix == null ? defaultNamespace : prefix2Namespace.get(prefix);
    }
    /**
     * Lookup if the parsed short URI (e.g "rdfs:label") uses one of the 
     * registered prefixes of this Enumeration of if the parsed short URI uses
     * the default namespace (e.g. "name"). In case the prefix could not be found
     * the parsed URI is returned unchanged
     * @param shortUri the short URI
     * @return the full URI if the parsed shortUri uses a prefix defined by this
     * Enumeration. Otherwise the parsed value.
     */
    public static String getFullName(String shortUri){
        if(shortUri == null) {
            return null;
        }
        int index = shortUri.indexOf(':');
        if(index>0){
            NamespaceEnum namespace = NamespaceEnum.forPrefix(shortUri.substring(0, index));
            if(namespace!= null){
                shortUri = namespace.getNamespace()+shortUri.substring(index+1);
            }
        } else if(defaultNamespace != null){
            shortUri = defaultNamespace.getNamespace()+shortUri;
        }
        return shortUri;
    }
    /**
     * Parsed the namespace of the parsed full URI by searching the last occurrence
     * of '#' or '/' and than looks if the namespace is part of this enumeration.
     * If a namesoace is found it is replaced by the registered prefix. If not
     * the parsed URI is resturned
     * @param fullUri the full uri to convert
     * @return the converted URI or the parsed value of <code>null</code> was
     * parsed, no local name was present (e.g. if the namespace itself was parsed)
     * or the parsed namespace is not known.
     */
    public static String getShortName(String fullUri){
        if(fullUri == null){
            return fullUri;
        }
        int index = Math.max(fullUri.lastIndexOf('#'),fullUri.lastIndexOf('/'));
        //do not convert if the parsed uri does not contain a local name
        if(index > 0 && index+1 < fullUri.length()){
            String ns = fullUri.substring(0, index+1);
            NamespaceEnum namespace = namespace2Prefix.get(ns);
            if(namespace != null){
                return namespace.getPrefix()+':'+fullUri.substring(index+1);
            }
        }
        return fullUri;
    }
    /**
     * @return the defaultPrefix
     */
    public boolean isDefault() {
        return defaultPrefix;
    }
}
