package org.apache.stanbol.entityhub.core.site;

import java.util.Dictionary;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Property;
import org.apache.stanbol.entityhub.servicesapi.site.ConfiguredSite;
import org.apache.stanbol.entityhub.servicesapi.site.EntitySearcher;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;

@Property(name=ConfiguredSite.QUERY_URI)
public abstract class AbstractEntitySearcher implements EntitySearcher {

    protected final Logger log;

    protected AbstractEntitySearcher(Logger log){
        this.log = log;
           log.info("create instance of "+this.getClass().getName());
    }

    private String queryUri;

    private Dictionary<String,?> config;
    private ComponentContext context;

    protected final String getQueryUri() {
        return queryUri;
    }

    @SuppressWarnings("unchecked")
    @Activate
    protected void activate(ComponentContext context) {
        log.debug("in "+AbstractEntitySearcher.class.getSimpleName()+" activate with context "+context);
        // TODO handle updates to the configuration
        if(context != null && context.getProperties() != null){
            this.context = context;
            Dictionary<String,?> properties = context.getProperties();
            Object queryUri = properties.get(EntitySearcher.QUERY_URI);
            Object accessUri = properties.get(ConfiguredSite.ACCESS_URI); //use as an fallback
            if(queryUri != null){
                this.queryUri = queryUri.toString();
                //now set the new config
            } else if(accessUri != null){
                log.info("Using AccessUri as fallback for missing QueryUri Proerty (accessUri="+accessUri);
                this.queryUri = accessUri.toString();
            } else {
                throw new IllegalArgumentException("The property "+EntitySearcher.QUERY_URI+" must be defined");
            }
            this.config = properties;
        } else {
            throw new IllegalArgumentException("The property "+EntitySearcher.QUERY_URI+" must be defined");
        }

    }
    @Deactivate
    protected void deactivate(ComponentContext context) {
        log.debug("in "+AbstractEntitySearcher.class.getSimpleName()+" deactivate with context "+context);
        this.config = null;
        this.queryUri = null;
    }
    /**
     * The OSGI configuration as provided by the activate method
     * @return
     */
    protected final Dictionary<String,?> getSiteConfiguration() {
        return config;
    }

    protected final ComponentContext getComponentContext(){
        return context;
    }
}
