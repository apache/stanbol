package org.apache.stanbol.entityhub.web.impl;

import java.util.Collection;

import javax.ws.rs.core.MediaType;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Service;
import org.apache.stanbol.entityhub.servicesapi.model.Representation;
import org.apache.stanbol.entityhub.web.ModelWriter;
import org.apache.stanbol.entityhub.web.ModelWriterRegistry;
import org.apache.stanbol.entityhub.web.writer.ModelWriterTracker;
import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentContext;
@Component(immediate=true)
@Service
public class ModelwriterRegistryImpl implements ModelWriterRegistry {

    
    private ModelWriterTracker modelTracker;
    
    @Activate
    protected void activate(ComponentContext ctx){
        modelTracker = new ModelWriterTracker(ctx.getBundleContext());
        modelTracker.open();
    }
    
    @Deactivate
    protected void deactivate(ComponentContext ctx){
        if(modelTracker != null){
            modelTracker.close();
        }
        modelTracker = null;
    }
    
    @Override
    public Collection<ServiceReference> getModelWriters(MediaType mediaType,
            Class<? extends Representation> nativeType) {
        return modelTracker.getModelWriters(mediaType, nativeType);
    }

    @Override
    public ModelWriter getService(ServiceReference ref) {
        return modelTracker.getService(ref);
    }

    @Override
    public boolean isWriteable(MediaType mediaType, Class<? extends Representation> nativeType) {
        return !modelTracker.getModelWriters(mediaType, nativeType).isEmpty();
    }

}
