package eu.iksproject.rick.servicesapi.mapping;

import eu.iksproject.rick.servicesapi.Rick;
import eu.iksproject.rick.servicesapi.site.ConfiguredSite;
import eu.iksproject.rick.servicesapi.site.ReferencedSite;
/**
 * Intended to define the configuration of the fieldMapper.
 *
 * @author Rupert Westenthaler
 * @deprecated unsure - Currently the functionality of this service is part of
 * the {@link RickConfig} and the {@link ConfiguredSite} interfaces. Access
 * Methods for the {@link FieldMapper} are defined by the {@link Rick} and
 * the {@link ReferencedSite} interfaces
 */
@Deprecated
public interface FieldMapperConfig {
    /**
     * The property used to configure the default mappings used by all
     * {@link ReferencedSite} instances active within the Rick
     */
    String DEFAULT_MAPPINGS = "eu.iksproject.rick.mapping.default";
    /**
     * The Property used to configure mappings that are only used for
     * representation of a specific Site.
     */
    String SITE_MAPPINGS = "eu.iksproject.rick.mapping.site";

}
