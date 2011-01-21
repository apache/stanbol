package org.apache.stanbol.entityhub.servicesapi.mapping;

import org.apache.stanbol.entityhub.servicesapi.Entityhub;
import org.apache.stanbol.entityhub.servicesapi.EntityhubConfiguration;
import org.apache.stanbol.entityhub.servicesapi.site.ConfiguredSite;
import org.apache.stanbol.entityhub.servicesapi.site.ReferencedSite;
/**
 * Intended to define the configuration of the fieldMapper.
 *
 * @author Rupert Westenthaler
 * @deprecated unsure - Currently the functionality of this service is part of
 * the {@link EntityhubConfiguration} and the {@link ConfiguredSite} interfaces. 
 * Access Methods for the {@link FieldMapper} are defined by the 
 * {@link Entityhub} and the {@link ReferencedSite} interfaces
 */
@Deprecated
public interface FieldMapperConfig {
    /**
     * The property used to configure the default mappings used by all
     * {@link ReferencedSite} instances active within the Entityhub
     */
    String DEFAULT_MAPPINGS = "org.apache.stanbol.entityhub.mapping.default";
    /**
     * The Property used to configure mappings that are only used for
     * representation of a specific Site.
     */
    String SITE_MAPPINGS = "org.apache.stanbol.entityhub.mapping.site";

}
