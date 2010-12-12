package eu.iksproject.rick.core.impl;

import java.util.Arrays;
import java.util.Collection;
import java.util.Dictionary;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.PropertyOption;
import org.apache.felix.scr.annotations.Service;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import eu.iksproject.rick.core.utils.OsgiUtils;
import eu.iksproject.rick.servicesapi.RickConfiguration;
import eu.iksproject.rick.servicesapi.model.EntityMapping;
import eu.iksproject.rick.servicesapi.model.Symbol;
import eu.iksproject.rick.servicesapi.model.EntityMapping.MappingState;
import eu.iksproject.rick.servicesapi.model.Symbol.SymbolState;

/**
 * Implementation of the Rick Configuration as an own component.
 * TODO: Currently the {@link RickImpl} has a 1..1 dependency to this one.
 * One could also just extend the {@link RickImpl} from this class.
 * @author Rupert Westenthaler
 *
 */
@Component(immediate=true,metatype=true)
@Service
public class RickConfigurationImpl implements RickConfiguration {

    private final Logger log = LoggerFactory.getLogger(RickConfigurationImpl.class);

    @Property(name=RickConfiguration.ID,value="rick")
    protected String rickID;
    @Property(name=RickConfiguration.NAME,value="<organisations> Rick")
    protected String rickName;
    @Property(name=RickConfiguration.DESCRIPTION,value="The Rick holding all Entities of <organisation>")
    protected String rickDescription;
    @Property(name=RickConfiguration.PREFIX,value="urn:eu.iksproject:rick:")
    protected String rickPrefix;
    @Property(name=RickConfiguration.RICK_YARD_ID,value=RickConfiguration.DEFAULT_RICK_YARD_ID)
    protected String rickYardId;
    @Property(
            name=RickConfiguration.FIELD_MAPPINGS,
            value ={ //This is the default config for well known Ontologies
                // --- Define the Languages for all fields ---
                //NOTE: the leading space is required for the global filter!
                " | @=null;en;de;fr;it", //will filter all labels with other languages
                // --- RDF, RDFS and OWL Mappings ---
                "rdfs:label", //rdf:label
                "rdfs:label > rick:label",
                "rdfs:comment",//rdf:comment
                "rdfs:comment > rick:description",
                "rdf:type | d=rick:ref",//The types
                "owl:sameAs | d=rick:ref",//used by LOD to link to URIs used to identify the same Entity
                // --- Dublin Core ---
                "dc:*", //all DC Terms properties
                "dc:title > rick:label",
                "dc:description > rick:description",
                "dc-elements:*", //all DC Elements (one could also define the mappings to the DC Terms counterparts here
                "dc-elements:title > rick:label",
                "dc-elements:description > rick:description",
                // --- Spatial Things ---
                "geo:lat | d=xsd:double",
                "geo:long | d=xsd:double",
                "geo:alt | d=xsd:int;xsd:float", //also allow floating point if one needs to use fractions of meters
                // --- Thesaurus (via SKOS) ---
                //SKOS can be used to define hierarchical terminologies
                "skos:*",
                "skos:prefLabel  > rick:label",
                "skos:definition > rick:description",
                "skos:note > rick:description",
                "skos:broader | d=rick:ref",
                "skos:narrower | d=rick:ref",
                "skos:related | d=rick:ref",
//                "skos:member | d=rick:ref",
                "skos:subject | d=rick:ref",
                "skos:inScheme | d=rick:ref",
//                "skos:hasTopConcept | d=rick:ref",
//                "skos:topConceptOf | d=rick:ref",
                // --- Social Networks (via foaf) ---
                "foaf:*", //The Friend of a Friend schema often used to describe social relations between people
                "foaf:name > rick:label",
//                "foaf:knows | d=rick:ref",
//                "foaf:made | d=rick:ref",
//                "foaf:maker | d=rick:ref",
//                "foaf:member | d=rick:ref",
                "foaf:homepage | d=xsd:anyURI",
                "foaf:depiction | d=xsd:anyURI",
                "foaf:img | d=xsd:anyURI",
                "foaf:logo | d=xsd:anyURI",
                "foaf:page | d=xsd:anyURI" //page about the entity
            })
    protected String[] fieldMappingConfig;
    //NOTE: there is no other way than hard coding the names there!
    @Property(name=RickConfiguration.DEFAULT_MAPPING_STATE,options={
            @PropertyOption( //seems, that name and value are exchanged ...
                    value='%'+RickConfiguration.DEFAULT_MAPPING_STATE+".option.proposed",
                    name="proposed"),
            @PropertyOption(
                    value='%'+RickConfiguration.DEFAULT_MAPPING_STATE+".option.confirmed",
                    name="confirmed")
            },value="proposed")
    protected String defaultMappingStateString;
    @Property(name=RickConfiguration.DEFAULT_SYMBOL_STATE,options={
            @PropertyOption( //seems, that name and value are exchanged ...
                    value='%'+RickConfiguration.DEFAULT_SYMBOL_STATE+".option.proposed",
                    name="proposed"),
            @PropertyOption(
                    value='%'+RickConfiguration.DEFAULT_SYMBOL_STATE+".option.active",
                    name="active")
            },value="proposed")
    protected String defaultSymblStateString;

    @Activate
    protected void activate(ComponentContext context) throws ConfigurationException {
        Dictionary<?, ?> properties = context.getProperties();
        log.debug("Activate Rick Configuration:");
        log.info("rickID:{}",rickID); //TODO remove ... just there to check if property annotations do actually set the property value
        log.info("rickName:{}",rickName);
        this.rickID = OsgiUtils.checkProperty(properties, RickConfiguration.ID).toString();
        this.rickName = OsgiUtils.checkProperty(properties, RickConfiguration.NAME, this.rickID).toString();
        Object rickDescription = properties.get(RickConfiguration.DESCRIPTION);
        this.rickDescription = rickDescription==null?null:rickDescription.toString();
        this.rickPrefix = OsgiUtils.checkProperty(properties, RickConfiguration.PREFIX).toString();
        this.rickYardId = OsgiUtils.checkProperty(properties, RickConfiguration.RICK_YARD_ID).toString();
        Object defaultSymbolState = properties.get(RickConfiguration.DEFAULT_SYMBOL_STATE);
        if(defaultSymbolState == null){
            this.defaultSymblStateString = Symbol.DEFAULT_SYMBOL_STATE.name();
        } else {
            this.defaultSymblStateString = defaultSymbolState.toString();
        }
        Object defaultMappingState = properties.get(RickConfiguration.DEFAULT_MAPPING_STATE);
        if(defaultMappingState == null){
            this.defaultMappingStateString = EntityMapping.DEFAULT_MAPPING_STATE.name();
        } else {
            this.defaultMappingStateString = defaultMappingState.toString();
        }
        Object fieldMappingConfig = OsgiUtils.checkProperty(properties, RickConfiguration.FIELD_MAPPINGS);
        if(fieldMappingConfig instanceof String[]){
            this.fieldMappingConfig = (String[])fieldMappingConfig;
        } else {
            throw new ConfigurationException(RickConfiguration.FIELD_MAPPINGS, "Values for this property must be of type Stirng[]!");
        }
    }
    @Override
    public String getID() {
        return rickID;
    }

    @Override
    public String getRickYardId() {
        return rickYardId;
    }

    @Override
    public String getDescription() {
        return rickDescription;
    }

    @Override
    public String getRickPrefix() {
        return rickID;
    }

    @Override
    public String getName() {
        return rickName;
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
