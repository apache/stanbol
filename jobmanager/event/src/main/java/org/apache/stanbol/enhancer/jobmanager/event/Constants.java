package org.apache.stanbol.enhancer.jobmanager.event;

import org.apache.clerezza.rdf.core.NonLiteral;
import org.apache.stanbol.enhancer.jobmanager.event.impl.EnhancementJob;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventConstants;

/**
 * Defines constants such as the used {@link EventConstants#EVENT_TOPIC} and the
 * properties used by the sent {@link Event}s.
 * @author Rupert Westenthaler
 *
 */
public interface Constants {

    /**
     * The topic used to report the completion the execution of an
     * EnhancementEngine back to the event job manager
     */
    String TOPIC_JOB_MANAGER = "stanbol/enhancer/jobmanager/event/topic";
    
    /**
     * Property used to provide the {@link EnhancementJob} instance
     */
    String PROPERTY_JOB_MANAGER = "stanbol.enhancer.jobmanager.event.job";
    /**
     * Property used to provide the {@link NonLiteral} node to execute
     */
    String PROPERTY_NODE = "stanbol.enhancer.jobmanager.event.node";

}
