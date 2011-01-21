package org.apache.stanbol.entityhub.core.yard;

import java.util.Dictionary;

import org.apache.stanbol.entityhub.core.yard.AbstractYard.YardConfig;
import org.apache.stanbol.entityhub.servicesapi.yard.Yard;
import org.osgi.service.cm.ConfigurationException;


/**
 * Default implementation for Yard. It uses only the fields defined by the
 * {@link Yard} interface and only enforces a valid ID.<p>
 * This implementation can be used for the configuration of Yards that do not
 * need any further configuration.
 * @author Rupert Westenthaler
 *
 */
public final class DefaultYardConfig extends YardConfig {

    /**
     * Creates a new configuration with the minimal set of required properties
     * @param id the ID of the Yard
     * @throws IllegalArgumentException if the parsed valued do not fulfil the
     * requirements.
     */
    public DefaultYardConfig(String id) throws IllegalArgumentException {
        super(id);
        try {
            isValid();
        } catch (ConfigurationException e) {
            throw new IllegalArgumentException(e.getMessage(),e);
        }
    }
    /**
     * Initialise the Yard configuration based on a parsed configuration. Usually
     * used on the context of an OSGI environment in the activate method.
     * @param config the configuration usually parsed within an OSGI activate
     * method
     * @throws ConfigurationException if the configuration is incomplete of
     * some values are not valid
     * @throws IllegalArgumentException if <code>null</code> is parsed as
     * configuration
     */
    public DefaultYardConfig(Dictionary<String, Object> config) throws ConfigurationException, IllegalArgumentException {
        super(config);
    }

    @Override
    protected void validateConfig() throws ConfigurationException {
    }

}
