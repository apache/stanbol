package org.apache.stanbol.enhancer.enginemanager.impl;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Service;
import org.apache.stanbol.enhancer.servicesapi.EnhancementEngineManager;
import org.apache.stanbol.enhancer.servicesapi.helper.EnginesTracker;
import org.osgi.service.component.ComponentContext;

/**
 * Implementation of the {@link EnhancementEngineManager} interface as OSGI 
 * component based on {@link EnginesTracker}.
 * 
 * @author Rupert Westenthaler
 *
 */
@Component(immediate=true,enabled=true)
@Service(value=EnhancementEngineManager.class)
public class EnhancementEngineManagerImpl extends EnginesTracker implements EnhancementEngineManager {

    public EnhancementEngineManagerImpl() {
        super();
    }

    @Activate
    public void activate(ComponentContext ctx){
        initEngineTracker(ctx.getBundleContext(), null, null);
        open();
    }
    @Deactivate
    public void deactivate(ComponentContext ctx){
        close();
    }

}
