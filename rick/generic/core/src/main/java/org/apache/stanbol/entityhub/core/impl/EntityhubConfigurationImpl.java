package org.apache.stanbol.entityhub.core.impl;

import java.util.Arrays;
import java.util.Collection;
import java.util.Dictionary;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.PropertyOption;
import org.apache.felix.scr.annotations.Service;
import org.apache.stanbol.entityhub.core.utils.OsgiUtils;
import org.apache.stanbol.entityhub.servicesapi.EntityhubConfiguration;
import org.apache.stanbol.entityhub.servicesapi.model.EntityMapping;
import org.apache.stanbol.entityhub.servicesapi.model.Symbol;
import org.apache.stanbol.entityhub.servicesapi.model.EntityMapping.MappingState;
import org.apache.stanbol.entityhub.servicesapi.model.Symbol.SymbolState;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Implementation of the Entityhub Configuration as an own component.
 * TODO: Currently the {@link EntityhubImpl} has a 1..1 dependency to this one.
 * One could also just extend the {@link EntityhubImpl} from this class.
 * @author Rupert Westenthaler
 *
 */
@Component(immediate=true,metatype=true)
@Service
public class EntityhubConfigurationImpl implements EntityhubConfiguration {

    private final Logger log = LoggerFactory.getLogger(EntityhubConfigurationImpl.class);

    @Property(name=EntityhubConfiguration.ID,value="entityhub")
    protected String entityhubID;
    @Property(name=EntityhubConfiguration.NAME,value="<organisations> Entityhub")
    protected String entityhubName;
    @Property(name=EntityhubConfiguration.DESCRIPTION,value="The entityhub holding all entities of <organisation>")
    protected String entityhubDescription;
    @Property(name=EntityhubConfiguration.PREFIX,value="urn:org.apache.stanbol:entityhub:")
    protected String entityhubPrefix;
    @Property(name=EntityhubConfiguration.ENTITYHUB_YARD_ID,value=EntityhubConfiguration.DEFAULT_ENTITYHUB_YARD_ID)
    protected String entityhubYardId;
    @Property(
            name=EntityhubConfiguration.FIELD_MAPPINGS,
            value ={ //This is the default config for well known Ontologies
                // --- Define the Languages for all fields ---
                //NOTE: the leading space is required for the global filter!
                " | @=null;en;de;fr;it", //will filter all labels with other languages
                // --- RDF, RDFS and OWL Mappings ---
                "rdfs:label", //rdf:label
                "rdfs:label > entityhub:label",
                "rdfs:comment",//rdf:comment
                "rdfs:comment > entityhub:description",
                "rdf:type | d=entityhub:ref",//The types
                "owl:sameAs | d=entityhub:ref",//used by LOD to link to URIs used to identify the same Entity
                // --- Dublin Core ---
                "dc:*", //all DC Terms properties
                "dc:title > entityhub:label",
                "dc:description > entityhub:description",
                "dc-elements:*", //all DC Elements (one could also define the mappings to the DC Terms counterparts here
                "dc-elements:title > entityhub:label",
                "dc-elements:description > entityhub:description",
                // --- Spatial Things ---
                "geo:lat | d=xsd:double",
                "geo:long | d=xsd:double",
                "geo:alt | d=xsd:int;xsd:float", //also allow floating point if one needs to use fractions of meters
                // --- Thesaurus (via SKOS) ---
                //SKOS can be used to define hierarchical terminologies
                "skos:*",
                "skos:prefLabel  > entityhub:label",
                "skos:definition > entityhub:description",
                "skos:note > entityhub:description",
                "skos:broader | d=entityhub:ref",
                "skos:narrower | d=entityhub:ref",
                "skos:related | d=entityhub:ref",
//                "skos:member | d=entityhub:ref",
                "skos:subject | d=entityhub:ref",
                "skos:inScheme | d=entityhub:ref",
//                "skos:hasTopConcept | d=entityhub:ref",
//                "skos:topConceptOf | d=entityhub:ref",
                // --- Social Networks (via foaf) ---
                "foaf:*", //The Friend of a Friend schema often used to describe social relations between people
                "foaf:name > entityhub:label",
//                "foaf:knows | d=entityhub:ref",
//                "foaf:made | d=entityhub:ref",
//                "foaf:maker | d=entityhub:ref",
//                "foaf:member | d=entityhub:ref",
                "foaf:homepage | d=xsd:anyURI",
                "foaf:depiction | d=xsd:anyURI",
                "foaf:img | d=xsd:anyURI",
                "foaf:logo | d=xsd:anyURI",
                "foaf:page | d=xsd:anyURI" //page about the entity
            })
    protected String[] fieldMappingConfig;
    //NOTE: there is no other way than hard coding the names there!
    @Property(name=EntityhubConfiguration.DEFAULT_MAPPING_STATE,options={
            @PropertyOption( //seems, that name and value are exchanged ...
                    value='%'+EntityhubConfiguration.DEFAULT_MAPPING_STATE+".option.proposed",
                    name="proposed"),
            @PropertyOption(
                    value='%'+EntityhubConfiguration.DEFAULT_MAPPING_STATE+".option.confirmed",
                    name="confirmed")
            },value="proposed")
    protected String defaultMappingStateString;
    @Property(name=EntityhubConfiguration.DEFAULT_SYMBOL_STATE,options={
            @PropertyOption( //seems, that name and value are exchanged ...
                    value='%'+EntityhubConfiguration.DEFAULT_SYMBOL_STATE+".option.proposed",
                    name="proposed"),
            @PropertyOption(
                    value='%'+EntityhubConfiguration.DEFAULT_SYMBOL_STATE+".option.active",
                    name="active")
            },value="proposed")
    protected String defaultSymblStateString;

    @Activate
    protected void activate(ComponentContext context) throws ConfigurationException {
        Dictionary<?, ?> properties = context.getProperties();
        log.debug("Activate Entityhub Configuration:");
        log.info("entityhubID:{}",entityhubID); //TODO remove ... just there to check if property annotations do actually set the property value
        log.info("entityhubName:{}",entityhubName);
        this.entityhubID = OsgiUtils.checkProperty(properties, EntityhubConfiguration.ID).toString();
        this.entityhubName = OsgiUtils.checkProperty(properties, EntityhubConfiguration.NAME, this.entityhubID).toString();
        Object entityhubDescription = properties.get(EntityhubConfiguration.DESCRIPTION);
        this.entityhubDescription = entityhubDescription==null?null:entityhubDescription.toString();
        this.entityhubPrefix = OsgiUtils.checkProperty(properties, EntityhubConfiguration.PREFIX).toString();
        this.entityhubYardId = OsgiUtils.checkProperty(properties, EntityhubConfiguration.ENTITYHUB_YARD_ID).toString();
        Object defaultSymbolState = properties.get(EntityhubConfiguration.DEFAULT_SYMBOL_STATE);
        if(defaultSymbolState == null){
            this.defaultSymblStateString = Symbol.DEFAULT_SYMBOL_STATE.name();
        } else {
            this.defaultSymblStateString = defaultSymbolState.toString();
        }
        Object defaultMappingState = properties.get(EntityhubConfiguration.DEFAULT_MAPPING_STATE);
        if(defaultMappingState == null){
            this.defaultMappingStateString = EntityMapping.DEFAULT_MAPPING_STATE.name();
        } else {
            this.defaultMappingStateString = defaultMappingState.toString();
        }
        Object fieldMappingConfig = OsgiUtils.checkProperty(properties, EntityhubConfiguration.FIELD_MAPPINGS);
        if(fieldMappingConfig instanceof String[]){
            this.fieldMappingConfig = (String[])fieldMappingConfig;
        } else {
            throw new ConfigurationException(EntityhubConfiguration.FIELD_MAPPINGS, "Values for this property must be of type Stirng[]!");
        }
    }
    @Override
    public String getID() {
        return entityhubID;
    }

    @Override
    public String getEntityhubYardId() {
        return entityhubYardId;
    }

    @Override
    public String getDescription() {
        return entityhubDescription;
    }

    @Override
    public String getEntityhubPrefix() {
        return entityhubID;
    }

    @Override
    public String getName() {
        return entityhubName;
    }

    @Override
    public Collection<String> getFieldMappingConfig() {
        return Arrays.asList(fieldMappingConfig);
    }

    @Override
    public MappingState getDefaultMappingState() {
        try {
            return MappingState.valueOf(defaultMappingStateString);
        } catch (IllegalArgumentException e) {
            log.warn("The value \""+defaultMappingStateString+"\" configured as default MappingState does not match any value of the Enumeration! " +
                    "Return the default state as defined by the "+EntityMapping.class+" interface (value="+EntityMapping.DEFAULT_MAPPING_STATE+")");
            return EntityMapping.DEFAULT_MAPPING_STATE;
        }
    }

    @Override
    public SymbolState getDefaultSymbolState() {
        try {
            return SymbolState.valueOf(defaultSymblStateString);
        } catch (IllegalArgumentException e) {
            log.warn("The value \""+defaultSymblStateString+"\" configured as default SymbolState does not match any value of the Enumeration! " +
                    "Return the default state as defined by the "+Symbol.class+" interface (value="+Symbol.DEFAULT_SYMBOL_STATE+")");
            return Symbol.DEFAULT_SYMBOL_STATE;
        }
    }
}
