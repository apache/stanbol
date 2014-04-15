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
package org.apache.stanbol.entityhub.indexing.source.vcard;

import java.util.HashMap;
import java.util.Map;

import net.fortuna.ical4j.vcard.Property.Id;
import edu.emory.mathcs.backport.java.util.Collections;

public final class OntologyMappings {

    /**
     * Restrict instantiation
     */
    private OntologyMappings() {}

    public static final String N_FAMILY = "N.family";
    public static final String N_GIVEN = "N.given";
    public static final String N_ADDITIONAL ="N.additional";
    public static final String N_PREFIX = "N.prefix";
    public static final String N_SUFFIX = "N.suffix";
    public static final String ADR_POST_OFFICE_ADDRESS = "ADR.poBox";
    public static final String ADR_EXTENDED = "ADR.extended";
    public static final String ADR_STREET = "ADR.street";
    public static final String ADR_LOCALITY = "ADR.locality";
    public static final String ADR_REGION = "ADR.region";
    public static final String ADR_POSTAL_CODE = "ADR.postalCode";
    public static final String ADR_COUNTRY = "ADR.country";
    public static final String GEO_LONGITUDE = "GEO.longitude";
    public static final String GEO_LATITUDE = "GEO.latitude";
    
    public static final String ORG_NAME = "ORG.name";
    public static final String ORG_UNIT = "ORG.unit";
    
    public static final String RDF_TYPE = "rdf:type";
    
    public static final String VCARD_PERSON = "vcard:Person";
    public static final String VCARD_ORGANIZATION = "vcard:Organization";
    
    /**
     * Defines mapping information for vCard properties as used as 
     * values for the {@link Map} defined as static members of the parent
     * {@link OntologyMappings} class.<p>
     * The keys {@link OntologyMappings#VCARD_PERSON} and
     * {@link OntologyMappings#VCARD_ORGANIZATION} can be used to specify the
     * <code>rdf:type</code> used for imported Persons and Organisations.<p>
     * The <code>uri</code> is a required parameter and specifies the
     * property of the Ontology used to store the values of the mapped vcard
     * property.<p>
     * For object properties the creation of sub-mappings and an inverse relation
     * is supported. The created sub-resource will be linked the the resource
     * of the vCard object by the <code>uri</code> property. If a mapping for
     * {@link OntologyMappings#RDF_TYPE} is present fir sub-mappings this is used
     * as <code>rdf:type</code> value for sub-resources<p>
     * To specify that sub-values of a vCard property should be directly
     * added to the resource of the vCard object one needs to add a
     * <code>null</code> mapping for the parent and than add the mappings for
     * the sub-properties also directly to this mappings.<br>
     * Here an example for the vCard "N" element:<p>
     * <pre><code>
     *     mappings.put("N",null); //add null mapping for the parent
     *     mappings.put("N_FAMILY", new Mapping("http:schema.org/familyName");
     *     mappings.put("N_GIVEN", new Mapping("http:schema.org/givenName");
     * </code></pre>
     * @author Rupert Westenthaler
     */
    public static final class Mapping{
        /**
         * The uri
         */
        public final String uri;
        public final String invUri;
        /**
         * the sub-mappings (read only).
         */
        public final Map<String,Mapping> subMappings;
        /**
         * Mapping for a vCard property to an rdfs:Property uri.
         * @param uri the URI of the rdfs:Property
         */
        private Mapping(String uri) {
            this(uri,null,null);
        }
        /**
         * Allows to define a Mapping with type and subMappings. See 
         * class level documentation for details.
         * @param uri the rdfs:Property uri used to store the information of the
         * mapped vCard property 
         * @param invUri the inverse property used to link from the sub-resource
         * back to the vCard resource.
         * @param subMappings mappings fpr the sub-resource
         */
        private Mapping(String uri,String invUri,Map<String,Mapping> subMappings) {
            if(uri == null || uri.isEmpty()){
                throw new IllegalArgumentException("The parsed property MUST NOT be NULL nor empty!");
            }
            this.uri = uri;
            if(invUri != null && subMappings == null){
                throw new IllegalArgumentException("Inverse Properties are only supported if sub-mappings are present");
            }
            this.invUri = invUri;
            this.subMappings = subMappings == null ? null : Collections.unmodifiableMap(subMappings);
        }
    }
    
    public static final Map<String,Mapping> schemaOrgMappings;
    private static final String schema = "http://schema.org/";
    static {
        Map<String,Mapping> mappings = new HashMap<String,Mapping>();
        //the rdf:type for vCard objects is schema.org/Person
        mappings.put(VCARD_PERSON, new Mapping(schema+"Person"));
        //and schema.org/Organization for vCard objects that do not define a
        //FN but only a ORG property
        mappings.put(VCARD_ORGANIZATION, new Mapping(schema+"Organization"));
        //map the formatted name and the nick to schema.org/name
        mappings.put(Id.FN.getPropertyName(), new Mapping(schema+"name"));
        mappings.put(Id.NICKNAME.getPropertyName(), new Mapping(schema+"name"));
        mappings.put(Id.NOTE.getPropertyName(), new Mapping(schema+"description"));
        //Name details are directly added to the person
        mappings.put(Id.N.getPropertyName(), null);
        mappings.put(N_FAMILY, new Mapping(schema+"familyName"));
        mappings.put(N_GIVEN, new Mapping(schema+"givenName"));
        mappings.put(N_ADDITIONAL, new Mapping(schema+"additionalName"));
        mappings.put(N_PREFIX, new Mapping(schema+"honorificPrefix"));
        mappings.put(N_SUFFIX, new Mapping(schema+"honorificSuffix"));


        //Address are stored in an own resource
        Map<String,Mapping> subMappings = new HashMap<String,Mapping>();
        mappings.put(Id.ADR.getPropertyName(), new Mapping(
            schema+"address", null,//address is the property (no inverse)
            subMappings)); //and there are sub mappings
        subMappings.put(RDF_TYPE, new Mapping(schema+"PostalAddress"));
        subMappings.put(ADR_COUNTRY, new Mapping(schema+"addressCountry"));
        subMappings.put(ADR_POSTAL_CODE, new Mapping(schema+"postalCode"));
        subMappings.put(ADR_STREET, new Mapping(schema+"streetAddress"));
        subMappings.put(ADR_POST_OFFICE_ADDRESS, new Mapping(schema+"postOfficeBoxNumber"));
        subMappings.put(ADR_REGION, new Mapping(schema+"addressRegion"));
        subMappings.put(ADR_EXTENDED, new Mapping(schema+"addressLocality"));
        
        //WorkLocation can be both "PostalAddress" or "Place". For modelling the
        //ADR I have chosen to use "PostalAdress". However to support both
        //ADR and GEO one would need to use an intermediate Place and append the
        //PostalAddress and the GeoCoordinate to it.
        //Because in vCard the ADR is much more important I choose to use
        // "PostalAddres" and to ignore GEO information.
        //mappings.put(Id.GEO.getPropertyName(), new Mapping(schema+"workLocation",));

        //Organisational properties
        
        //Here both TITLE and ROLE is mapped to JobTitle
        mappings.put(Id.TITLE.getPropertyName(), new Mapping(schema+"jobTitle"));
        mappings.put(Id.ROLE.getPropertyName(), new Mapping(schema+"jobTitle"));
        subMappings = new HashMap<String,Mapping>();
        mappings.put(Id.ORG.getPropertyName(), new Mapping(
            schema+"worksFor", schema+"employees",
            subMappings));
        subMappings.put(RDF_TYPE, new Mapping(schema+"Organization"));
        subMappings.put(ORG_NAME, new Mapping(schema+"name"));
        
        mappings.put(Id.BDAY.getPropertyName(), new Mapping(schema+"birthDate"));
        mappings.put(Id.DEATH.getPropertyName(), new Mapping(schema+"deathDate"));
        mappings.put(Id.GENDER.getPropertyName(), new Mapping(schema+"gender"));
        
        mappings.put(Id.PHOTO.getPropertyName(), new Mapping(schema+"image"));

        mappings.put(Id.TEL.getPropertyName(), new Mapping(schema+"telephone"));
        mappings.put(Id.EMAIL.getPropertyName(), new Mapping(schema+"email"));
        mappings.put(Id.URL.getPropertyName(), new Mapping(schema+"url"));


        //Not mapped Properties
        //mappings.put(Id.AGENT.getPropertyName(), new Mapping(schema+""));
        //mappings.put(Id.LABEL.getPropertyName(), new Mapping(schema+""));
        //mappings.put(Id.LANG.getPropertyName(), new Mapping(schema+""));
        //mappings.put(Id.LOGO.getPropertyName(), new Mapping(schema+""));
        //mappings.put(Id.MEMBER.getPropertyName(), new Mapping(schema+""));
        //mappings.put(Id.PRODID.getPropertyName(), new Mapping(schema+""));
        //mappings.put(Id.RELATED.getPropertyName(), new Mapping(schema+""));
        //mappings.put(Id.REV.getPropertyName(), new Mapping(schema+""));
        //mappings.put(Id.SORT_STRING.getPropertyName(), new Mapping(schema+""));
        //mappings.put(Id.SOUND.getPropertyName(), new Mapping(schema+""));
        //mappings.put(Id.SOURCE.getPropertyName(), new Mapping(schema+""));
        //mappings.put(Id.TZ.getPropertyName(), new Mapping(schema+""));
        //mappings.put(Id.UID.getPropertyName(), new Mapping(schema+""));
        //mappings.put(Id.VERSION.getPropertyName(), new Mapping(schema+""));
        schemaOrgMappings = Collections.unmodifiableMap(mappings);
    }

}
