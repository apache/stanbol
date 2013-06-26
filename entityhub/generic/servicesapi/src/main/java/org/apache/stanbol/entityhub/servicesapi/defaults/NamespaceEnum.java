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
 */
public enum NamespaceEnum {
    /*
     * STANBOL internally used Namespaces
     */
    /**
     * The Stanbol Enhancer namespace defining Enhancer, EnhancementEngine and
     * EnhancementChain. This is NOT the namespace of the enhancement structure.
     * As EnhancementStrucutre up to now still the old FISE namespace is used.
     */
    enhancer("http://stanbol.apache.org/ontology/enhancer/enhancer#"),
    //Namespaces defined by the entityhub
    /**
     * The Namespace used by the Entityhub to define its concepts such as
     * Entity, Representation ...
     */
    entityhub("http://stanbol.apache.org/ontology/entityhub/entityhub#"),
    /**
     * The namespace used by the Entityhub to define query related concepts
     * e.g. the full text search field, semantic context field, result score ...
     */
    entityhubQuery("entityhub-query","http://stanbol.apache.org/ontology/entityhub/query#"),
    /**
     * The FISE namespace (1st version of the Enhancement Structure).
     * Will be replaced by the Stanbol Enhancement Structure by a future
     * release (see STANBOL-3).
     */
    fise("http://fise.iks-project.eu/ontology/"),

    //First the XML Namespaces
    /**
     * The XSD namespace as used by the datatypes of XML and RDF literals 
     */
    xsd("http://www.w3.org/2001/XMLSchema#"),
    /**
     * The XSI namespace
     */
    xsi("http://www.w3.org/2001/XMLSchema-instance#"),
    /**
     * The XML namespace as used by the language attribute of XML and the language
     * for RDF literals
     */
    xml("http://www.w3.org/XML/1998/namespace#"),
    //Start with the semantic Web Namespaces
    /**
    * The RDF namespace (rdf:type)
    */
    rdf("http://www.w3.org/1999/02/22-rdf-syntax-ns#"),
    /**
    * The RDFS namespace (rdfs:label, rdfs:comment, rdfs:seeAlso)
    */
    rdfs("http://www.w3.org/2000/01/rdf-schema#"),
    /**
    * The OWL namespace (owl:sameAs)
    */
    owl("http://www.w3.org/2002/07/owl#"),
    //CMIS related
    /**
    * @deprecated All none core namespaces where deprecated. Users should use
    * the NamespacePrefixService (module:
    * org.apache.stanbol.commons.namespaceprefixservice) instead (see also
    * <a href="https://issues.apache.org/jira/browse/STANBOL-824">STANBOL-824)</a>
    */
    atom("http://www.w3.org/2005/Atom"),
    /**
    * @deprecated All none core namespaces where deprecated. Users should use
    * the NamespacePrefixService (module:
    * org.apache.stanbol.commons.namespaceprefixservice) instead (see also
    * <a href="https://issues.apache.org/jira/browse/STANBOL-824">STANBOL-824)</a>
    */
    cmis("http://docs.oasis-open.org/ns/cmis/core/200908/"),
    /**
    * @deprecated All none core namespaces where deprecated. Users should use
    * the NamespacePrefixService (module:
    * org.apache.stanbol.commons.namespaceprefixservice) instead (see also
    * <a href="https://issues.apache.org/jira/browse/STANBOL-824">STANBOL-824)</a>
    */
    cmisRa("cmis-ra","http://docs.oasis-open.org/ns/cmis/restatom/200908/"),
    //now the JCR related Namespaces
    /**
    * @deprecated All none core namespaces where deprecated. Users should use
    * the NamespacePrefixService (module:
    * org.apache.stanbol.commons.namespaceprefixservice) instead (see also
    * <a href="https://issues.apache.org/jira/browse/STANBOL-824">STANBOL-824)</a>
    */
    jcr("jcr","http://www.jcp.org/jcr/1.0/"),
    /**
    * @deprecated All none core namespaces where deprecated. Users should use
    * the NamespacePrefixService (module:
    * org.apache.stanbol.commons.namespaceprefixservice) instead (see also
    * <a href="https://issues.apache.org/jira/browse/STANBOL-824">STANBOL-824)</a>
    */
    jcrSv("jcr-sv","http://www.jcp.org/jcr/sv/1.0/"),
    /**
    * @deprecated All none core namespaces where deprecated. Users should use
    * the NamespacePrefixService (module:
    * org.apache.stanbol.commons.namespaceprefixservice) instead (see also
    * <a href="https://issues.apache.org/jira/browse/STANBOL-824">STANBOL-824)</a>
    */
    jcrNt("jcr-nt","http://www.jcp.org/jcr/nt/1.0/"),
    /**
    * @deprecated All none core namespaces where deprecated. Users should use
    * the NamespacePrefixService (module:
    * org.apache.stanbol.commons.namespaceprefixservice) instead (see also
    * <a href="https://issues.apache.org/jira/browse/STANBOL-824">STANBOL-824)</a>
    */
    jcrMix("jcr-mix","http://www.jcp.org/jcr/mix/1.0/"),
    //Some well known Namespaces of Ontologies
    /**
    * @deprecated All none core namespaces where deprecated. Users should use
    * the NamespacePrefixService (module:
    * org.apache.stanbol.commons.namespaceprefixservice) instead (see also
    * <a href="https://issues.apache.org/jira/browse/STANBOL-824">STANBOL-824)</a>
    */
    geo("http://www.w3.org/2003/01/geo/wgs84_pos#"),
    /**
    * @deprecated All none core namespaces where deprecated. Users should use
    * the NamespacePrefixService (module:
    * org.apache.stanbol.commons.namespaceprefixservice) instead (see also
    * <a href="https://issues.apache.org/jira/browse/STANBOL-824">STANBOL-824)</a>
    */
    georss("http://www.georss.org/georss/"),
    /**
    * @deprecated All none core namespaces where deprecated. Users should use
    * the NamespacePrefixService (module:
    * org.apache.stanbol.commons.namespaceprefixservice) instead (see also
    * <a href="https://issues.apache.org/jira/browse/STANBOL-824">STANBOL-824)</a>
    */
    gml("http://www.opengis.net/gml/"),
    /**
    * @deprecated All none core namespaces where deprecated. Users should use
    * the NamespacePrefixService (module:
    * org.apache.stanbol.commons.namespaceprefixservice) instead (see also
    * <a href="https://issues.apache.org/jira/browse/STANBOL-824">STANBOL-824)</a>
    */
    dcElements("dc-elements","http://purl.org/dc/elements/1.1/"),
    /**
    * The DC terms ontology as used by the Entityhub to manage metadata such as
    * creator, creation/modification dates as well as linking to the license
    */
    dcTerms("dc","http://purl.org/dc/terms/"), // Entityhub prefers DC-Terms, therefore use the "dc" prefix for the terms name space
    /**
    * FOAF ontology used as type for the resource providing metadata about an
    * Entity (foaf:Document)
    */
    foaf("http://xmlns.com/foaf/0.1/"),
    /**
    * @deprecated All none core namespaces where deprecated. Users should use
    * the NamespacePrefixService (module:
    * org.apache.stanbol.commons.namespaceprefixservice) instead (see also
    * <a href="https://issues.apache.org/jira/browse/STANBOL-824">STANBOL-824)</a>
    */
    vCal("http://www.w3.org/2002/12/cal#"),
    /**
    * @deprecated All none core namespaces where deprecated. Users should use
    * the NamespacePrefixService (module:
    * org.apache.stanbol.commons.namespaceprefixservice) instead (see also
    * <a href="https://issues.apache.org/jira/browse/STANBOL-824">STANBOL-824)</a>
    */
    vCard("http://www.w3.org/2001/vcard-rdf/3.0#"),
    /**
    * The SKOS namespace (skos:Concept)
    */
    skos("http://www.w3.org/2004/02/skos/core#"),
    /**
    * @deprecated All none core namespaces where deprecated. Users should use
    * the NamespacePrefixService (module:
    * org.apache.stanbol.commons.namespaceprefixservice) instead (see also
    * <a href="https://issues.apache.org/jira/browse/STANBOL-824">STANBOL-824)</a>
    */
    sioc("http://rdfs.org/sioc/ns#"),
    /**
    * @deprecated All none core namespaces where deprecated. Users should use
    * the NamespacePrefixService (module:
    * org.apache.stanbol.commons.namespaceprefixservice) instead (see also
    * <a href="https://issues.apache.org/jira/browse/STANBOL-824">STANBOL-824)</a>
    */
    siocTypes("sioc-types","http://rdfs.org/sioc/types#"),
    /**
    * @deprecated All none core namespaces where deprecated. Users should use
    * the NamespacePrefixService (module:
    * org.apache.stanbol.commons.namespaceprefixservice) instead (see also
    * <a href="https://issues.apache.org/jira/browse/STANBOL-824">STANBOL-824)</a>
    */
    bio("dc-bio","http://purl.org/vocab/bio/0.1/"),
    /**
    * @deprecated All none core namespaces where deprecated. Users should use
    * the NamespacePrefixService (module:
    * org.apache.stanbol.commons.namespaceprefixservice) instead (see also
    * <a href="https://issues.apache.org/jira/browse/STANBOL-824">STANBOL-824)</a>
    */
    rss("http://purl.org/rss/1.0/"),
    /**
    * @deprecated All none core namespaces where deprecated. Users should use
    * the NamespacePrefixService (module:
    * org.apache.stanbol.commons.namespaceprefixservice) instead (see also
    * <a href="https://issues.apache.org/jira/browse/STANBOL-824">STANBOL-824)</a>
    */
    goodRelations("gr","http://purl.org/goodrelations/v1#"),
    /**
    * @deprecated All none core namespaces where deprecated. Users should use
    * the NamespacePrefixService (module:
    * org.apache.stanbol.commons.namespaceprefixservice) instead (see also
    * <a href="https://issues.apache.org/jira/browse/STANBOL-824">STANBOL-824)</a>
    */
    swrc("http://swrc.ontoware.org/ontology#"), //The Semantic Web for Research Communities Ontology
    //Linked Data Ontologies
    /**
    * THe DBpedia.org ontology (dbp-ont:Person, dbp-ont:Organisation, dbp-ont:Place, ...)
    */
    dbpediaOnt("dbp-ont","http://dbpedia.org/ontology/"),
    /**
    * @deprecated All none core namespaces where deprecated. Users should use
    * the NamespacePrefixService (module:
    * org.apache.stanbol.commons.namespaceprefixservice) instead (see also
    * <a href="https://issues.apache.org/jira/browse/STANBOL-824">STANBOL-824)</a>
    */
    dbpediaProp("dbp-prop","http://dbpedia.org/property/"),
    /**
    * @deprecated All none core namespaces where deprecated. Users should use
    * the NamespacePrefixService (module:
    * org.apache.stanbol.commons.namespaceprefixservice) instead (see also
    * <a href="https://issues.apache.org/jira/browse/STANBOL-824">STANBOL-824)</a>
    */
    geonames("http://www.geonames.org/ontology#"),
    //copyright and license
    /**
    * Creative Commons as used by the Entityhub for license and attribution information
    */
    cc("http://creativecommons.org/ns#"),
    //Schema.org (see http://schema.org/docs/schemaorg.owl for the Ontology)
    /**
    * @deprecated All none core namespaces where deprecated. Users should use
    * the NamespacePrefixService (module:
    * org.apache.stanbol.commons.namespaceprefixservice) instead (see also
    * <a href="https://issues.apache.org/jira/browse/STANBOL-824">STANBOL-824)</a>
    */
    schema("http://schema.org/",true),
    /**
     * The W3C Ontology for Media Resources http://www.w3.org/TR/mediaont-10/
    * @deprecated All none core namespaces where deprecated. Users should use
    * the NamespacePrefixService (module:
    * org.apache.stanbol.commons.namespaceprefixservice) instead (see also
    * <a href="https://issues.apache.org/jira/browse/STANBOL-824">STANBOL-824)</a>
     */
    media("http://www.w3.org/ns/ma-ont#"),
    /*
     * eHealth domain 
     */
    /**
     * DrugBank is a repository of almost 5000 FDA-approved small molecule and 
     * biotech drugs. 
    * @deprecated All none core namespaces where deprecated. Users should use
    * the NamespacePrefixService (module:
    * org.apache.stanbol.commons.namespaceprefixservice) instead (see also
    * <a href="https://issues.apache.org/jira/browse/STANBOL-824">STANBOL-824)</a>
     */
    drugbank("http://www4.wiwiss.fu-berlin.de/drugbank/resource/drugbank/"),
    /**
     * Dailymed is published by the National Library of Medicine, 
     * and provides high quality information about marketed drugs.
    * @deprecated All none core namespaces where deprecated. Users should use
    * the NamespacePrefixService (module:
    * org.apache.stanbol.commons.namespaceprefixservice) instead (see also
    * <a href="https://issues.apache.org/jira/browse/STANBOL-824">STANBOL-824)</a>
     */
    dailymed("http://www4.wiwiss.fu-berlin.de/dailymed/resource/dailymed/"),
    /**
     * SIDER contains information on marketed drugs and their adverse effects. 
     * The information is extracted from public documents and package inserts.
    * @deprecated All none core namespaces where deprecated. Users should use
    * the NamespacePrefixService (module:
    * org.apache.stanbol.commons.namespaceprefixservice) instead (see also
    * <a href="https://issues.apache.org/jira/browse/STANBOL-824">STANBOL-824)</a>
     */
    sider("http://www4.wiwiss.fu-berlin.de/sider/resource/sider/"),
    /**
     * The Linked Clinical Trials (LinkedCT) project aims at publishing the 
     * first open Semantic Web data source for clinical trials data.
    * @deprecated All none core namespaces where deprecated. Users should use
    * the NamespacePrefixService (module:
    * org.apache.stanbol.commons.namespaceprefixservice) instead (see also
    * <a href="https://issues.apache.org/jira/browse/STANBOL-824">STANBOL-824)</a>
     */
    linkedct("http://data.linkedct.org/resource/linkedct/"),
    /**
     * STITCH contains information on chemicals and proteins as well as their 
     * interactions and links.
    * @deprecated All none core namespaces where deprecated. Users should use
    * the NamespacePrefixService (module:
    * org.apache.stanbol.commons.namespaceprefixservice) instead (see also
    * <a href="https://issues.apache.org/jira/browse/STANBOL-824">STANBOL-824)</a>
     */
    stitch("http://www4.wiwiss.fu-berlin.de/stitch/resource/stitch/"),
    /**
     * Diseasome publishes a network of 4,300 disorders and disease genes linked 
     * by known disorder-gene associations for exploring all known phenotype and 
     * disease gene associations, indicating the common genetic origin of many 
     * diseases.
    * @deprecated All none core namespaces where deprecated. Users should use
    * the NamespacePrefixService (module:
    * org.apache.stanbol.commons.namespaceprefixservice) instead (see also
    * <a href="https://issues.apache.org/jira/browse/STANBOL-824">STANBOL-824)</a>
     */
    diseasome("http://www4.wiwiss.fu-berlin.de/diseasome/resource/diseasome/"),
    /**
     * National Cancer Institute Thesaurus (http://www.mindswap.org/2003/CancerOntology/)
    * @deprecated All none core namespaces where deprecated. Users should use
    * the NamespacePrefixService (module:
    * org.apache.stanbol.commons.namespaceprefixservice) instead (see also
    * <a href="https://issues.apache.org/jira/browse/STANBOL-824">STANBOL-824)</a>
     */
    nci("http://www.mindswap.org/2003/nciOncology.owl#"),
    /**
    * @deprecated All none core namespaces where deprecated. Users should use
    * the NamespacePrefixService (module:
    * org.apache.stanbol.commons.namespaceprefixservice) instead (see also
    * <a href="https://issues.apache.org/jira/browse/STANBOL-824">STANBOL-824)</a>
     */
    tcm("http://purl.org/net/tcm/tcm.lifescience.ntu.edu.tw/"),
    /**
     * The Music Ontology (http://musicontology.com/)
    * @deprecated All none core namespaces where deprecated. Users should use
    * the NamespacePrefixService (module:
    * org.apache.stanbol.commons.namespaceprefixservice) instead (see also
    * <a href="https://issues.apache.org/jira/browse/STANBOL-824">STANBOL-824)</a>
     */
    mo("http://purl.org/ontology/mo/"),
    /**
     *  The Time ontology (http://www.w3.org/TR/owl-time/)
    * @deprecated All none core namespaces where deprecated. Users should use
    * the NamespacePrefixService (module:
    * org.apache.stanbol.commons.namespaceprefixservice) instead (see also
    * <a href="https://issues.apache.org/jira/browse/STANBOL-824">STANBOL-824)</a>
     */
    owlTime("owl-time","http://www.w3.org/2006/time#"),
    /**
     *  The Event ontology (http://purl.org/NET/c4dm/event.owl#)
    * @deprecated All none core namespaces where deprecated. Users should use
    * the NamespacePrefixService (module:
    * org.apache.stanbol.commons.namespaceprefixservice) instead (see also
    * <a href="https://issues.apache.org/jira/browse/STANBOL-824">STANBOL-824)</a>
     */
    event("http://purl.org/NET/c4dm/event.owl#"),
    /**
     *  The Timeline ontology (http://purl.org/NET/c4dm/timeline.owl#)
    * @deprecated All none core namespaces where deprecated. Users should use
    * the NamespacePrefixService (module:
    * org.apache.stanbol.commons.namespaceprefixservice) instead (see also
    * <a href="https://issues.apache.org/jira/browse/STANBOL-824">STANBOL-824)</a>
     */
    timeline("http://purl.org/NET/c4dm/timeline.owl#"),
    /**
     *  Relationship: A vocabulary for describing relationships between people
     *  (http://purl.org/vocab/relationship/)
    * @deprecated All none core namespaces where deprecated. Users should use
    * the NamespacePrefixService (module:
    * org.apache.stanbol.commons.namespaceprefixservice) instead (see also
    * <a href="https://issues.apache.org/jira/browse/STANBOL-824">STANBOL-824)</a>
     */
    rel("http://purl.org/vocab/relationship/"),
    /**
     *  Expression of Core FRBR Concepts in RDF (http://vocab.org/frbr/core)
    * @deprecated All none core namespaces where deprecated. Users should use
    * the NamespacePrefixService (module:
    * org.apache.stanbol.commons.namespaceprefixservice) instead (see also
    * <a href="https://issues.apache.org/jira/browse/STANBOL-824">STANBOL-824)</a>
     */
    frbr("http://purl.org/vocab/frbr/core#"),
    
    /**
     * Special namespace used for disambiguation metadata. NOTE: that with STANBOL-1053
     * the URI used for disambiguation changed.
     * 
     */
    disambiguation("dis","http://stanbol.apache.org/ontology/disambiguation/disambiguation#"),
    /*
     * Old namespaces still kept for historical reasons
     */
    /**
     * The old URI for the 'entityhub' namespace prefix as used by STANBOL
     * 0.9.0-incubating.
     * @see NamespaceEnum#entityhubModel
     */
    @Deprecated
    rickModel("rick","http://www.iks-project.eu/ontology/rick/model/"),
    /**
     * The old URI for the 'entityhub-query' namespace prefix as used by STANBOL
     * 0.9.0-incubating.
     * @see NamespaceEnum#entityhubQuery
     */
    @Deprecated
    rickQuery("rick-query","http://www.iks-project.eu/ontology/rick/query/"),

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
     * Enumeration. Otherwise (including <code>null</code>) the parsed value.
    * @deprecated To obtain the fullName for an URI users should use  
    * the NamespacePrefixService (module:
    * org.apache.stanbol.commons.namespaceprefixservice) instead (see also
    * <a href="https://issues.apache.org/jira/browse/STANBOL-824">STANBOL-824)</a>
    * <b>NOTE</b> that this service will return <code>null</code> if a shortUri
    * was parsed AND no mapping for the prefix was defined. This is different to
    * this version that would return the parsed string.
     */
    public static String getFullName(String shortUri){
        //ignore null and empty strings
        if(shortUri == null || shortUri.isEmpty()) {
            return shortUri;
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
    * @deprecated To obtain the shortName for an URI users should use  
    * the NamespacePrefixService (module:
    * org.apache.stanbol.commons.namespaceprefixservice) instead (see also
    * <a href="https://issues.apache.org/jira/browse/STANBOL-824">STANBOL-824)</a>
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
    * @deprecated To obtain the default namespace please lookup the prefx '' 
    * on the the NamespacePrefixService (module:
    * org.apache.stanbol.commons.namespaceprefixservice) instead (see also
    * <a href="https://issues.apache.org/jira/browse/STANBOL-824">STANBOL-824)</a>
     */
    public boolean isDefault() {
        return defaultPrefix;
    }
}
