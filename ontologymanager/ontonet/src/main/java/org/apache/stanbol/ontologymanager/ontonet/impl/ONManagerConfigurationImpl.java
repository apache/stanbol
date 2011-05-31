package org.apache.stanbol.ontologymanager.ontonet.impl;

import java.util.Arrays;
import java.util.Dictionary;
import java.util.List;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Service;
import org.apache.stanbol.ontologymanager.ontonet.api.ONManagerConfiguration;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Default implementation of the {@link ONManagerConfiguration}.
 * 
 * @author alessandro
 * 
 */
@Component(immediate = true, metatype = true)
@Service
public class ONManagerConfigurationImpl implements ONManagerConfiguration {

    public static final String _CONFIG_ONTOLOGY_PATH_DEFAULT = "";

    public static final String _ID_DEFAULT = "ontonet";

    public static final String _ONTOLOGY_NETWORK_NS_DEFAULT = "http://kres.iksproject.eu/";

    @Property(name = ONManagerConfiguration.CONFIG_ONTOLOGY_PATH, value = _CONFIG_ONTOLOGY_PATH_DEFAULT)
    private String configPath;

    protected Logger log = LoggerFactory.getLogger(getClass());

    /**
     * TODO how do you use array initializers in Property annotations without causing compile errors?
     */
    @Property(name = ONManagerConfiguration.ONTOLOGY_PATHS, value = {".", "ontologies"})
    private String[] ontologyDirs;

    @Property(name = ONManagerConfiguration.ID, value = _ID_DEFAULT)
    private String ontonetID;

    @Property(name = ONManagerConfiguration.ONTOLOGY_NETWORK_NS, value = _ONTOLOGY_NETWORK_NS_DEFAULT)
    private String ontonetNS;

    /**
     * This default constructor is <b>only</b> intended to be used by the OSGI environment with Service
     * Component Runtime support.
     * <p>
     * DO NOT USE to manually create instances - the ReengineerManagerImpl instances do need to be configured!
     * YOU NEED TO USE {@link #ONManagerConfigurationImpl(Dictionary)} or its overloads, to parse the
     * configuration and then initialise the rule store if running outside an OSGI environment.
     */
    public ONManagerConfigurationImpl() {}

    /**
     * To be invoked by non-OSGi environments.
     * 
     * @param configuration
     */
    public ONManagerConfigurationImpl(Dictionary<String,Object> configuration) {
        this();
        activate(configuration);
    }

    @SuppressWarnings("unchecked")
    @Activate
    protected void activate(ComponentContext context) {
        log.info("in {} activate with context {}", getClass(), context);
        if (context == null) {
            throw new IllegalStateException("No valid" + ComponentContext.class + " parsed in activate!");
        }
        activate((Dictionary<String,Object>) context.getProperties());
    }

    protected void activate(Dictionary<String,Object> configuration) {
        // Parse configuration.
        ontonetID = (String) configuration.get(ONManagerConfiguration.ID);
        if (ontonetID == null) ontonetID = _ID_DEFAULT;
        ontologyDirs = (String[]) configuration.get(ONManagerConfiguration.ONTOLOGY_PATHS);
        if (ontologyDirs == null) ontologyDirs = new String[] {".", "ontologies"};
        ontonetNS = (String) configuration.get(ONManagerConfiguration.ONTOLOGY_NETWORK_NS);
        if (ontonetNS == null) ontonetNS = _ONTOLOGY_NETWORK_NS_DEFAULT;
        configPath = (String) configuration.get(ONManagerConfiguration.CONFIG_ONTOLOGY_PATH);
        if (configPath == null) configPath = _CONFIG_ONTOLOGY_PATH_DEFAULT;
    }

    @Deactivate
    protected void deactivate(ComponentContext context) {
        ontonetID = null;
        ontologyDirs = null;
        ontonetNS = null;
        configPath = null;
        log.info("in {} deactivate with context {}", getClass(), context);
    }

    @Override
    public String getID() {
        return ontonetID;
    }

    @Override
    public String getOntologyNetworkConfigurationPath() {
        return configPath;
    }

    @Override
    public String getOntologyNetworkNamespace() {
        return ontonetNS;
    }

    @Override
    public List<String> getOntologySourceDirectories() {
        return Arrays.asList(ontologyDirs);
    }

}
