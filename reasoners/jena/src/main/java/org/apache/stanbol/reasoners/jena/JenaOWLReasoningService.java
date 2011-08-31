package org.apache.stanbol.reasoners.jena;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Service;
import org.apache.stanbol.reasoners.servicesapi.ReasoningService;
import org.osgi.service.component.ComponentContext;

import com.hp.hpl.jena.reasoner.ReasonerRegistry;

/**
 * OWL Reasoning service
 */
@Component(immediate = true, metatype = true)
@Service
public class JenaOWLReasoningService extends AbstractJenaReasoningService {
    public static final String _DEFAULT_PATH = "owl";

    @Property(name = ReasoningService.SERVICE_PATH, value = _DEFAULT_PATH)
    private String path;

    public JenaOWLReasoningService() {
        super(ReasonerRegistry.getOWLReasoner());
    }

    @Override
    public String getPath() {
        return path;
    }

    @Activate
    public void activate(ComponentContext context) {
        this.path = (String) context.getProperties().get(ReasoningService.SERVICE_PATH);
    }
}
