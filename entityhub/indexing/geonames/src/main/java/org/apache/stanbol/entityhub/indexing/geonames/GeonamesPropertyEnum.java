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
package org.apache.stanbol.entityhub.indexing.geonames;

import org.apache.stanbol.entityhub.core.model.InMemoryValueFactory;
import org.apache.stanbol.entityhub.servicesapi.defaults.NamespaceEnum;
import org.apache.stanbol.entityhub.servicesapi.model.Reference;

public enum GeonamesPropertyEnum {
    /*
     * idx ... temporarily used during indexing
     */
    idx_id(GeonamesPropertyEnum.idx,"id"), //the integer id
    idx_CC(GeonamesPropertyEnum.idx,"CC"), //the country code
    idx_ADM(GeonamesPropertyEnum.idx,"ADM"), //the string ADM
    idx_ADM1(GeonamesPropertyEnum.idx,"ADM1"), //the string ADM1
    idx_ADM2(GeonamesPropertyEnum.idx,"ADM2"), //the string ADM1.ADM2
    idx_ADM3(GeonamesPropertyEnum.idx,"ADM3"), //the string ADM1.ADM2.ADM3
    idx_ADM4(GeonamesPropertyEnum.idx,"ADM4"), //the string ADM1.ADM2.ADM3.ADM4
    rdf_type(NamespaceEnum.rdf.getNamespace(),"type"),
    rdfs_label(NamespaceEnum.rdfs.getNamespace(),"label"),
    dc_creator(GeonamesPropertyEnum.dct,"creator"),
    dc_date(GeonamesPropertyEnum.dct,"date"),
    gn_Feature("Feature"),
    //gn_Country("Country"),
    gn_countryCode("countryCode"),
    //gn_Map("Map"),
    //gn_RDFData("RDFData"),
    //gn_WikipediaArticle("WikipediaArticle"),
    gn_parentFeature("parentFeature"),
    gn_parentCountry("parentCountry"),
    gn_parentADM1("parentADM1"),
    gn_parentADM2("parentADM2"),
    gn_parentADM3("parentADM3"),
    gn_parentADM4("parentADM4"),
    //gn_childrenFeatures("childrenFeatures"),
    //gn_inCountry("inCountry"),
    //gn_locatedIn("locatedIn"),
    //gn_locationMap("locationMap"),
    //gn_nearby("nearby"),
    //gn_nearbyFeatures("nearbyFeatures"),
    //gn_neighbour("neighbour"),
    //gn_neighbouringFeatures("neighbouringFeatures"),
    gn_wikipediaArticle("wikipediaArticle"),
    gn_featureClass("featureClass"),
    gn_featureCode("featureCode"),
    //gn_tag("tag"),
    gn_alternateName("alternateName"),
    gn_officialName("officialName"),
    gn_name("name"),
    gn_population("population"),
    gn_shortName("shortName"),
    gn_colloquialName("colloquialName"),
    gn_postalCode("postalCode"),
    geo_lat(GeonamesPropertyEnum.geo,"lat"),
    geo_long(GeonamesPropertyEnum.geo,"long"),
    geo_alt(GeonamesPropertyEnum.geo,"alt"),
    skos_notation(GeonamesPropertyEnum.skos,"notation"),
    skos_prefLabel(GeonamesPropertyEnum.skos,"prefLabel"),
    skos_altLabel(GeonamesPropertyEnum.skos,"altLabel"),
    skos_hiddenLabel(GeonamesPropertyEnum.skos,"hiddenLabel"),
    skos_note(GeonamesPropertyEnum.skos,"note"),
    skos_changeNote(GeonamesPropertyEnum.skos,"changeNote"),
    skos_definition(GeonamesPropertyEnum.skos,"definition"),
    skos_editorialNote(GeonamesPropertyEnum.skos,"editorialNote"),
    skos_example(GeonamesPropertyEnum.skos,"example"),
    skos_historyNote(GeonamesPropertyEnum.skos,"historyNote"),
    skos_scopeNote(GeonamesPropertyEnum.skos,"scopeNote"),
    skos_broader(GeonamesPropertyEnum.skos,"broader"),
    skos_narrower(GeonamesPropertyEnum.skos,"narrower"),
    skos_related(GeonamesPropertyEnum.skos,"related"),
    ;
    private static final String idx = "urn:stanbol:entityhub:indexing:geonames:";
    private static final String ns = "http://www.geonames.org/ontology#";
    private static final String geo = "http://www.w3.org/2003/01/geo/wgs84_pos#";
    private static final String skos = "http://www.w3.org/2004/02/skos/core#";
    private static final String dct = "http://purl.org/dc/terms/";
    private String uri;
    private Reference ref;
    
    GeonamesPropertyEnum(String name){
        this(null,name);
    }
    GeonamesPropertyEnum(String namespace,String name){
        uri = (namespace == null ? ns : namespace)+name;
        ref = InMemoryValueFactory.getInstance().createReference(uri);
    }
    @Override
    public String toString() {
        return uri;
    }
    public Reference getReference() {
        return ref;
    }
}