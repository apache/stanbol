package org.apache.stanbol.cmsadapter.servicesapi.processor;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.ReferencePolicy;
import org.apache.felix.scr.annotations.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component(immediate = true)
@Service(value = TypeLifterManager.class)
public class TypeLifterManager {
    @Reference(cardinality = ReferenceCardinality.MANDATORY_MULTIPLE, referenceInterface = TypeLifter.class, policy = ReferencePolicy.DYNAMIC, bind = "bindTypeLifter", unbind = "unbindTypeLifter")
    private List<TypeLifter> typeLifters = new CopyOnWriteArrayList<TypeLifter>();

    private static final Logger logger = LoggerFactory.getLogger(TypeLifterManager.class);

    public TypeLifter getRepositoryAccessor(String connectionType) {
        for (TypeLifter typeLifter : typeLifters) {
            if (typeLifter.canLift(connectionType)) {
                return typeLifter;
            }
        }

        logger.warn("No suitable type lifter implementation for connection type: {} ", connectionType);
        return null;
    }

    protected void bindTypeLifter(TypeLifter typeLifter) {
        typeLifters.add(typeLifter);
    }

    protected void unbindTypeLifter(TypeLifter typeLifter) {
        typeLifters.remove(typeLifter);
    }
}
