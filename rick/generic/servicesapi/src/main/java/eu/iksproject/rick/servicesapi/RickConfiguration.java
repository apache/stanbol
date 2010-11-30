package eu.iksproject.rick.servicesapi;

import java.util.Collection;

import eu.iksproject.rick.servicesapi.mapping.FieldMapping;
import eu.iksproject.rick.servicesapi.model.EntityMapping;
import eu.iksproject.rick.servicesapi.model.Symbol;
import eu.iksproject.rick.servicesapi.model.EntityMapping.MappingState;
import eu.iksproject.rick.servicesapi.site.ReferencedSite;

/**
 * Provides the Configuration needed by the {@link Rick}.<p>
 * @author Rupert Westenthaler
 *
 */
public interface RickConfiguration {
	/**
	 * The key used to configure the ID of the RICK
	 */
	String ID = "eu.iksprojct.rick.id";
	/**
	 * The ID of the Rick. This ID is used as origin (sign site) for all symbols
	 * and mapped entities created by the Rick
	 * @return the ID of the Rick
	 */
	String getID();
	/**
	 * The property used to configure the prefix used for {@link Symbol}s and
	 * {@link EntityMapping}s created by the Rick
	 */
	String PREFIX = "eu.iksproject.rick.prefix";
	/**
	 * Getter for the Prefix to be used for all {@link Symbol}s and {@link EntityMapping}s
	 * created by the {@link Rick}
	 * @return The prefix for {@link Symbol}s and {@link EntityMapping}s
	 */
	String getRickPrefix();
	/**
	 * The key used to configure the name of the RICK
	 */
	String NAME = "eu.iksprojct.rick.name";
	/**
	 * The human readable name of this RICK instance. Typically used as label 
	 * in addition/instead of the ID.
	 * @return the Name (or the ID in case no name is defined)
	 */
	String getName();
	/**
	 * The key used to configure the description of the RICK
	 */
	String DESCRIPTION = "eu.iksprojct.rick.description";
	/**
	 * The human readable description to provide some background information about
	 * this RICK instance.
	 * @return the description or <code>null</code> if none is defined/configured.
	 */
	String getDescription();
	/**
	 * The property used to configure the id of the RickYard
	 */
	String RICK_YARD_ID = "eu.iksproject.rick.yard.rickYardId";
	/**
	 * The default ID for the Yard used for the Rick
	 */
	String DEFAULT_RICK_YARD_ID = "rickYard";
	/**
	 * This is the ID of the Yard used by the Rick to store its data
	 * @return the Rick-Yard id
	 */
	String getRickYardId();
	/**
	 * The property used to configure the field mappings for the RICK
	 */
	String FIELD_MAPPINGS = "eu.iksproject.rick.mapping.rick";
	/**
	 * Getter for the FieldMapping configuration of the Rick. These Mappings are
	 * used for every {@link ReferencedSite} of the Rick.<br>
	 * Note that {@link FieldMapping#parseFieldMapping(String)} is used to
	 * parsed the values returned by this Method
	 * @return the configured mappings for the Rick
	 */
	Collection<String> getFieldMappingConfig();
	/**
	 * The property used to configure the initial state for new {@link EntityMapping}s
	 */
	String DEFAULT_MAPPING_STATE = "eu.iksproject.rick.defaultMappingState";
	/**
	 * The initial (default) state for new {@link EntityMapping}s
	 * @return the default state for new {@link EntityMapping}s
	 */
	MappingState getDefaultMappingState();
	/**
	 * The property used to configure the initial state for new {@link Symbol}s
	 */
	String DEFAULT_SYMBOL_STATE = "eu.iksproject.rick.defaultSymbolState";
	/**
	 * The initial (default) state for new {@link Symbol}s
	 * @return the default state for new {@link Symbol}s
	 */
	Symbol.SymbolState getDefaultSymbolState();

}
