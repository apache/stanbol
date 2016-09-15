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
package org.apache.stanbol.commons.namespaceprefix.mappings;



/**
 * Enumeration defining the Namespace Prefix Mappings used exposed by the 
 * Stanbol Default Namespace Mapping Provider.
 */
public enum DefaultNamespaceMappingsEnum{
    /*
     * STANBOL ENHANCER 
     */
    /**
     * The Stanbol Enhancer namespace defining Enhancer, EnhancementEngine and
     * EnhancementChain. This is NOT the namespace of the enhancement structure.
     * As EnhancementStrucutre up to now still the old FISE namespace is used.
     */
    enhancer("http://stanbol.apache.org/ontology/enhancer/enhancer#"),
    /**
     * The FISE namespace (1st version of the Enhancement Structure).
     * Will be replaced by the Stanbol Enhancement Structure by a future
     * release (see STANBOL-3).
     */
    fise("http://fise.iks-project.eu/ontology/"),    
    /**
     * Namespace for the Stanbol Enhancer Execution Plan ontology
     */
    ep("http://stanbol.apache.org/ontology/enhancer/executionplan#"),
    /**
     * Namespace for the Stanbol Enhancer Execution Metadata ontology
     */
    em("http://stanbol.apache.org/ontology/enhancer/executionmetadata#"),
    /*
     * STANBOL Entityhub
     */
    /**
     * The namespace of the Apache Stanbol Entityhub
     */
    entityhub("http://stanbol.apache.org/ontology/entityhub/entityhub#"),
    /**
     * The namespace used by the Entityhub to define query related concepts
     * e.g. the full text search field, semantic context field, result score ...
     */
    entityhubQuery("entityhub-query","http://stanbol.apache.org/ontology/entityhub/query#"),
    /*
     * Namespaces directly referenced by Stanbol
     */
    /**
     * Stanbol Enhancement Structure uses dc:terms with the prefix 'dc'
     */
    dc("http://purl.org/dc/terms/"),
    /**
     * The dbpedia ontology as used by the Enhancer for NamedEntit
     */
    dbpedia_ont("dbpedia-ont", "http://dbpedia.org/ontology/"),
    /**
     * SKOS is used for hierarchical controlled vocabularies 
     */
    skos("http://www.w3.org/2004/02/skos/core#"),
    /*
     * XML related namespaces
     */
    xsd("http://www.w3.org/2001/XMLSchema#"),
    xsi("http://www.w3.org/2001/XMLSchema-instance#"),
    xml("http://www.w3.org/XML/1998/namespace#"),    
    /*
     * Semantic Web Technology core name spaces
     */
    rdf("http://www.w3.org/1999/02/22-rdf-syntax-ns#"),
    rdfs("http://www.w3.org/2000/01/rdf-schema#"),
    owl("http://www.w3.org/2002/07/owl#"),
    /*
     * CMS related namespaces
     */
    //CMIS related
    atom("http://www.w3.org/2005/Atom/"),
    cmis("http://docs.oasis-open.org/ns/cmis/core/200908/"),
    cmisRa("cmis-ra","http://docs.oasis-open.org/ns/cmis/restatom/200908/"),
    //now the JCR related Namespaces
    jcr("jcr","http://www.jcp.org/jcr/1.0/"),
    jcrSv("jcr-sv","http://www.jcp.org/jcr/sv/1.0/"),
    jcrNt("jcr-nt","http://www.jcp.org/jcr/nt/1.0/"),
    jcrMix("jcr-mix","http://www.jcp.org/jcr/mix/1.0/"),
    /*
     * Other Namespaces defined by Stanbol before the introduction of the
     * NamespacePrefixService
     */
    geo("http://www.w3.org/2003/01/geo/wgs84_pos#"),
    georss("http://www.georss.org/georss/"),
    gml("http://www.opengis.net/gml/"),
    dcElements("dc-elements","http://purl.org/dc/elements/1.1/"),
    dcTerms("dct","http://purl.org/dc/terms/"),
    foaf("http://xmlns.com/foaf/0.1/"),
    vCal("http://www.w3.org/2002/12/cal#"),
    vCard("http://www.w3.org/2001/vcard-rdf/3.0#"),
    sioc("http://rdfs.org/sioc/ns#"),
    siocTypes("sioc-types","http://rdfs.org/sioc/types#"),
    bio("dc-bio","http://purl.org/vocab/bio/0.1/"),
    rss("http://purl.org/rss/1.0/"),
    goodRelations("gr","http://purl.org/goodrelations/v1#"),
    /**
     * The Semantic Web for Research Communities Ontology
     */
    swrc("http://swrc.ontoware.org/ontology#"),
    /**
     * Nepomuk Information Element Ontology
     */
    nie("http://www.semanticdesktop.org/ontologies/2007/01/19/nie#"),
    //Linked Data Ontologies
    dbpediaOnt("dbp-ont","http://dbpedia.org/ontology/"),
    /**
     * The "dbpedia-owl" prefix was used by a single mapping of the dbpedia
     * indexing tool. This was actually not intended, but as the new service does
     * validate prefixes this now causes errors. So this prefix was added to the
     * list. However it is not recommended to be used - hence deprecated
     * @deprecated
     */
    dbpediaOnt2("dbpedia-owl","http://dbpedia.org/ontology/"),
    dbpediaProp("dbp-prop","http://dbpedia.org/property/"),
    geonames("http://www.geonames.org/ontology#"),
    //copyright and license
    cc("http://creativecommons.org/ns#"),
    //Schema.org (see http://schema.org/docs/schemaorg.owl for the Ontology)
    schema("http://schema.org/"),
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
    tcm("http://purl.org/net/tcm/tcm.lifescience.ntu.edu.tw/"),
    /**
     * The Music Ontology (http://musicontology.com/)
     */
    mo("http://purl.org/ontology/mo/"),
    /**
     *  The Time ontology (http://www.w3.org/TR/owl-time/)
     */
    owlTime("owl-time","http://www.w3.org/2006/time#"),
    /**
     *  The Event ontology (http://purl.org/NET/c4dm/event.owl#)
     */
    event("http://purl.org/NET/c4dm/event.owl#"),
    /**
     *  The Timeline ontology (http://purl.org/NET/c4dm/timeline.owl#)
     */
    timeline("http://purl.org/NET/c4dm/timeline.owl#"),
    /**
     *  Relationship: A vocabulary for describing relationships between people
     *  (http://purl.org/vocab/relationship/)
     */
    rel("http://purl.org/vocab/relationship/"),
    /**
     *  Expression of Core FRBR Concepts in RDF (http://vocab.org/frbr/core)
     */
    frbr("http://purl.org/vocab/frbr/core#"),
    /*
     * Freebase namesoaces
     */
    /**
     * The freebase.com namespace
     */
    fb("http://rdf.freebase.com/ns/"),
    /**
     * The freebase.com key namespace. Keys are used to refer to keys used by
     * for freebase topics (entities) on external sites (e.g. musicbrainz, 
     * wikipedia ...).
     */
    key("http://rdf.freebase.com/key/"),
    /**
     * The EnhancementProperties namespace as introduced by <a 
     * href="https://issues.apache.org/jira/browse/STANBOL-488">STANBOL-488</a>
     */
    ehp("http://stanbol.apache.org/ontology/enhancementproperties#"),
    /*
     * Added several mappings form prefix.cc for namespaces defined above
     */
    /**
     * Alternative to {@link #dcElements}
     */
    dce("http://purl.org/dc/elements/1.1/"),
    /**
     * Alternative for {@link #dbpedia_ont}
     */
    dbo("http://dbpedia.org/ontology/"),
    /**
     * DBpedia resources
     */
    dbr("http://dbpedia.org/resource/"),
    /**
     * Alternative to {@link #dbpediaProp}
     */
    dbp("http://dbpedia.org/property/"),
    /**
     * Alternative to {@link #geonames}
     */
    gn("http://www.geonames.org/ontology#")
    ;
    private String namespace;
    private String prefix;

    DefaultNamespaceMappingsEnum(String namespace){
        this(null,namespace);
    }
    DefaultNamespaceMappingsEnum(String prefix,String namespace){
        this.prefix = prefix == null ? name() : prefix;
        this.namespace = namespace;
    }

    public String getNamespace() {
        return namespace;
    }
    public String getPrefix() {
        return prefix;
    }
    /**
     * "{prefix}\t{namespace}"
     */
    @Override
    public String toString() {
        return prefix+"\t"+namespace;
    }
    
}
