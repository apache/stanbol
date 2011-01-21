package org.apache.stanbol.entityhub.core.site;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Dictionary;
import java.util.List;
import java.util.RandomAccess;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Property;
import org.apache.stanbol.entityhub.servicesapi.site.ConfiguredSite;
import org.apache.stanbol.entityhub.servicesapi.site.EntityDereferencer;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;

@Property(name = EntityDereferencer.ACCESS_URI, label = "%dereference.baseUri.name", description = "%dereference.baseUri.description")
public abstract class AbstractEntityDereferencer implements EntityDereferencer {

    protected final Logger log;

    protected AbstractEntityDereferencer(Logger log) {
        this.log = log;
        log.info("create instance of " + this.getClass().getName());
    }

    private String baseUri;

    private Dictionary<String, ?> config;
    private List<String> prefixes;
    private ComponentContext context;

    @Override
    public final String getAccessUri() {
        return baseUri;
    }

    @SuppressWarnings("unchecked")
    @Activate
    protected void activate(ComponentContext context) {
        log.info("in " + AbstractEntityDereferencer.class.getSimpleName() + " activate with context " + context);
        // TODO handle updates to the configuration
        if (context != null && context.getProperties() != null) {
            this.context = context;
            Dictionary<String, ?> properties = context.getProperties();
            Object baseUri = properties.get(EntityDereferencer.ACCESS_URI);
            if (baseUri != null) {
                this.baseUri = baseUri.toString();
                //now set the new config
                this.config = properties;
            } else {
                throw new IllegalArgumentException("The property " + EntityDereferencer.ACCESS_URI + " must be defined");
            }
            //TODO: I am sure, there is some Utility, that supports getting multiple
            //      values from a OSGI Dictionary
            Object prefixObject = properties.get(ConfiguredSite.ENTITY_PREFIX);
            ArrayList<String> prefixes = new ArrayList<String>();
            if (prefixObject == null) {
                prefixes = null;
            } else if (prefixObject.getClass().isArray()) {
                prefixes.addAll(Arrays.asList((String[]) prefixObject));
            } else if (prefixObject instanceof Collection<?>) {
                prefixes.addAll((Collection<String>) prefixObject);
            } else { //assuming a single value
                prefixes.add(prefixObject.toString());
            }
            Collections.sort(prefixes); //sort the prefixes List
            this.prefixes = Collections.unmodifiableList(prefixes); //use an unmodifiable wrapper for the member variable
        } else {
            throw new IllegalArgumentException("The property " + EntityDereferencer.ACCESS_URI + " must be defined");
        }

    }

    @Deactivate
    protected void deactivate(ComponentContext context) {
        log.info("in " + AbstractEntityDereferencer.class.getSimpleName() + " deactivate with context " + context);
        this.config = null;
        this.prefixes = null;
        this.baseUri = null;
    }

    /**
     * Default Implementation based on the configured EntityPrefixes that
     * executes in O(log n) by using a binary search
     * {@link Collections#binarySearch(List, Object)} in the list of prefixes
     * and than checking if the parsed uri starts with the prefix of the
     * "insertion point-1" as returned by the binary search
     */
    @Override
    public boolean canDereference(String uri) {
        //binary search for the uri returns "insertion point-1"
        int pos = Collections.binarySearch(prefixes, uri);
        //This site can dereference the URI if
        //  - the pos >=0 (parsed uri > than the first element in the list AND
        //  - the returned index is an prefix of the parsed uri
        return pos >= 0 && uri.startsWith(prefixes.get(pos));
    }

    /**
     * The prefixes as configured for this dereferencer as unmodifiable and
     * sorted list guaranteed to implementing {@link RandomAccess}.
     *
     * @return the prefixes in an unmodifiable and sorted list that implements
     *         {@link RandomAccess}.
     */
    protected final List<String> getPrefixes() {
        return prefixes;
    }

    /**
     * The OSGI configuration as provided by the activate method
     *
     * @return
     */
    protected final Dictionary<String, ?> getSiteConfiguration() {
        return config;
    }

    protected final ComponentContext getComponentContext() {
        return context;
    }
}
