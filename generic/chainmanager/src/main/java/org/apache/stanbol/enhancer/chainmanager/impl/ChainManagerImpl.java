package org.apache.stanbol.enhancer.chainmanager.impl;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Service;
import org.apache.stanbol.enhancer.servicesapi.ChainManager;
import org.apache.stanbol.enhancer.servicesapi.helper.ChainsTracker;
import org.osgi.service.component.ComponentContext;

/**
 * Implementation of the ChainManager interface as OSGI component based
 * on {@link ChainsTracker}.
 * 
 * @author Rupert Westenthaler
 *
 */
@Component(immediate=true,enabled=true)
@Service(value=ChainManager.class)
public class ChainManagerImpl extends ChainsTracker implements ChainManager {

    public ChainManagerImpl(){
        super();
    }
    
    @Activate
    public void activate(ComponentContext ctx){
        initChainTracker(ctx.getBundleContext(), null, null);
        open();
    }
    @Deactivate
    public void deactivate(ComponentContext ctx){
        close();
    }
}
