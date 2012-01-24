package org.apache.stanbol.enhancer.jobmanager.event.impl;

import static org.apache.stanbol.enhancer.servicesapi.helper.ExecutionPlanHelper.getDependend;
import static org.apache.stanbol.enhancer.servicesapi.helper.ExecutionPlanHelper.getEngine;
import static org.apache.stanbol.enhancer.servicesapi.helper.ExecutionPlanHelper.isOptional;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;

import org.apache.clerezza.rdf.core.Graph;
import org.apache.clerezza.rdf.core.NonLiteral;
import org.apache.stanbol.enhancer.servicesapi.Chain;
import org.apache.stanbol.enhancer.servicesapi.ContentItem;
import org.apache.stanbol.enhancer.servicesapi.EnhancementEngine;
import org.apache.stanbol.enhancer.servicesapi.EnhancementJobManager;
import org.apache.stanbol.enhancer.servicesapi.helper.ExecutionPlanHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Represents the enhancement of a {@link ContentItem} by following the
 * execution plan provided by a {@link Chain} as parsed to an
 * {@link EnhancementJobManager}.<p>
 * TODO: This might become part of the Stanbol Enhancer Services API.
 * 
 * @author Rupert Westenthaler
 */
public class EnhancementJob {

    /*
     * NOTE on debug level Loggings
     * 
     *  ++ ... init some activity
     *  >> ... started some activity (thread has the requested lock)
     *  << ... completed some activity (thread has released the lock)
     *  
     *  n: ... no lock
     *  r: ... read lock
     *  w: ... write lock
     */
    private final Logger log = LoggerFactory.getLogger(EnhancementJob.class);

    private final Lock readLock;
    private final Lock writeLock;
    private final Graph executionPlan;
    private final String chain;
    private final ContentItem contentItem;

    private final Set<NonLiteral> completed;
    private final Set<NonLiteral> unmodCompleted;
    private final Set<NonLiteral> running;
    private final Set<NonLiteral> unmodRunning;

    private Set<NonLiteral> executable;
    private boolean failed = false;
    private List<String> errormessages = new ArrayList<String>();
    private Exception error = null;

    public EnhancementJob(ContentItem contentItem, String chainName, Graph executionPlan) {
        if (contentItem == null || chainName == null || executionPlan == null) {
            throw new IllegalArgumentException("The parsed contentItem and executionPlan MUST NOT be NULL");
        }
        this.contentItem = contentItem;
        this.readLock = contentItem.getLock().readLock();
        this.writeLock = contentItem.getLock().writeLock();
        this.executionPlan = executionPlan;
        this.chain = chainName;
        completed = new HashSet<NonLiteral>();
        unmodCompleted = Collections.unmodifiableSet(completed);
        running = new HashSet<NonLiteral>();
        unmodRunning = Collections.unmodifiableSet(running);
        //check the first engines to execute
        checkExecutable();
    }

    /**
     * The used execution plan for processing the {@link ContentItem}
     * 
     * @return the executionPlan
     */
    public final Graph getExecutionPlan() {
        return executionPlan;
    }

    /**
     * The ContentItem enhanced by this job.
     * 
     * @return
     */
    public final ContentItem getContentItem() {
        return contentItem;
    }

    /**
     * This returns the name of the Chain used to initialise the execution plan for this enhancement job.
     * <p>
     * NOTE that because chains can change (activate, change, deactivate) at any moment there is no guarantee
     * that the a chain with that name is still available nor that calling {@link Chain#getExecutionPlan()}
     * will be equals to the {@link #getExecutionPlan()} used by this enhancement job. This is the reason why
     * this method just returns the name of the chain and not the {@link Chain} instance.
     * 
     * @return
     */
    public final String getChainName() {
        return chain;
    }

    /**
     * Getter for the lock used to synchronise read/write to this enhancement job. This returns the lock
     * provided by {@link ContentItem#getLock()}
     * 
     * @return the read/write lock for this enhancement job
     */
    public final ReadWriteLock getLock() {
        return contentItem.getLock();
    }


    /**
     * Getter for a read only view over the currently running executions.
     * 
     * @return the currently running executions.
     */
    public Set<NonLiteral> getRunning() {
        log.debug("++ r: {}","getRunning");
        readLock.lock();
        try {
            log.debug(">> r: {}","getRunning");
            return unmodRunning;
        } finally {
            log.debug("<< r: {}","getRunning");
            readLock.unlock();
        }
    }

    /**
     * Getter for a read only view over the completed execution.
     * 
     * @return the completed execution nodes
     */
    public Set<NonLiteral> getCompleted() {
        log.debug("++ r: {}","getCompleted");
        readLock.lock();
        try {
            log.debug(">> r: {}","getCompleted");
            return unmodCompleted;
        } finally {
            log.debug("<< r: {}","getCompleted");
            readLock.unlock();
        }
    }

    /**
     * Sets the state of the parsed executionNode to completed. This also validates if the new state
     * confirms to the ExectionPlan (e.g. if all nodes the parsed node depends on are also marked as
     * completed).
     * 
     * @param executionNode
     *            the exectionNode to be marked as running
     * @throws IllegalArgumentException
     *             if <code>null</code> is parsed as execution node
     * @throws IllegalStateException
     *             if the parsed execution node can not be marked as completed because some of its
     *             depended nodes are not yet marked as completed.
     */
    public void setCompleted(NonLiteral executionNode) {
        if (executionNode != null) {
            String engine = getEngine(executionPlan, executionNode);
            boolean optional = isOptional(executionPlan, executionNode);
            Set<NonLiteral> dependsOn = getDependend(executionPlan, executionNode);
            log.debug("++ w: {}: {}","setCompleted",ExecutionPlanHelper.getEngine(executionPlan, executionNode));
            writeLock.lock();
            try {
                log.debug(">> w: {}: {}","setCompleted",ExecutionPlanHelper.getEngine(executionPlan, executionNode));
                if (completed.contains(executionNode)) {
                    log.warn("Execution of Engine '{}' for ContentItem {} already "
                             + "marked as completed(chain: {}, node: {}, optional {})."
                             + " -> call ignored", 
                             new Object[] {engine, contentItem.getUri().getUnicodeString(),
                                           chain, executionNode, optional});
                    return;
                }
                if (!completed.containsAll(dependsOn)) {
                    // TODO maybe define an own Exception for such cases
                    throw new IllegalStateException(
                            "Unable to set state of ExectionNode '"
                                    + executionNode+ "' (chain '"+ chain
                                    + "' | contentItem '"+ contentItem.getUri()
                                    + "') to completed, because some of its depended "
                                    + "nodes are not marked completed yet. This indicates an Bug in the "
                                    + "implementation of the JobManager used to execute the ExecutionPlan. "
                                    + "(this.dependsOn=" + dependsOn + "| chain.completed " + completed
                                    + " | chain.running " + running + ")!");
                }
                if (running.remove(executionNode)) {
                    log.info(
                        "Execution of '{}' for ContentItem {} completed "
                        + "(chain: {}, node: {}, optional {})",
                        new Object[] {engine, contentItem.getUri().getUnicodeString(), 
                                      chain, executionNode, optional});
                }
                completed.add(executionNode);
                // update the executables ... this will also recognise if finished 
                checkExecutable();
            } finally {
                log.debug("<< w: {}: {}","setCompleted",ExecutionPlanHelper.getEngine(executionPlan, executionNode));
                writeLock.unlock();
            }
        }
    }

    /**
     * Sets the state of the parsed executionNode to running. This also validates if the new state
     * confirms to the ExectionPlan (e.g. if all nodes the parsed node depends on are already marked as
     * completed).
     * 
     * @param executionNode
     *            the exectionNode to be marked as running
     * @throws IllegalArgumentException
     *             if <code>null</code> is parsed as execution node
     * @throws IllegalStateException
     *             if the parsed execution node can not be marked as running because some of its depended
     *             nodes are not yet marked as completed.
     */
    public void setRunning(NonLiteral executionNode) {
        if (executionNode != null) {
            String engine = getEngine(executionPlan, executionNode);
            boolean optional = isOptional(executionPlan, executionNode);
            Set<NonLiteral> dependsOn = getDependend(executionPlan, executionNode);
            log.debug("++ w: {}: {}","setRunning",ExecutionPlanHelper.getEngine(executionPlan, executionNode));
            writeLock.lock();
            try {
                log.debug(">> w: {}: {}","setRunning",ExecutionPlanHelper.getEngine(executionPlan, executionNode));
                if (completed.contains(executionNode)) {
                    throw new IllegalStateException(
                            "Unable to set state of ExectionNode '"+ executionNode
                            + "'(chain '"+chain+ "' | contentItem '"
                            + contentItem.getUri()+"') to running, because"
                            + "it is already marked as completed. This indicates "
                            + "an Bug in the implementation of the JobManager "
                            + "used to execute the ExecutionPlan (chain state: "
                            +"completed " + completed + " | running " + running
                            + ")!");
                }
                if (!completed.containsAll(dependsOn)) {
                    // TODO maybe define an own Exception for such cases
                    throw new IllegalStateException(
                            "Unable to set state of ExectionNode '"+ executionNode
                            + "' (chain '"+chain+ "' | contentItem '"
                            + contentItem.getUri()+"') to running, because "
                            + "some of its depended nodes are not marked "
                            + "completed yet. This indicates an Bug in the "
                            + "implementation of the JobManager used to execute "
                            + "the ExecutionPlan (this.dependsOn=" + dependsOn 
                            + "| chain.completed " + completed
                            + " | chain.running " + running + ")!");
                }
                if (!running.add(executionNode)) {
                    log.warn("Execution of Engine '{}' for ContentItem {} already "
                             + "marked as running(chain: {}, node: {}, optional {})."
                             + " -> call ignored", 
                             new Object[] {engine, contentItem.getUri().getUnicodeString(),
                                           chain, executionNode, optional});
                    return;
                } else {
                    log.info("Started Execution of '{}' for ContentItem {} "
                             + "(chain: {}, node: {}, optional {})",
                        new Object[] {engine, contentItem.getUri().getUnicodeString(), chain,
                                      executionNode, optional});
                    // update the executables ... this will also recognise if finished 
                    checkExecutable();
                }
            } finally {
                log.debug("<< w: {}: {}","setRunning",ExecutionPlanHelper.getEngine(executionPlan, executionNode));
                writeLock.unlock();
            }
        } else {
            throw new IllegalArgumentException("The parsed ExecutionNode MUST NOT be NULL!");
        }
    }
    /**
     * updated the {@link #executable} and also checks for {@link #finished}<p>
     * Assumed to be called within a write lock!
     */
    private void checkExecutable(){
        Set<NonLiteral> executeable = 
                ExecutionPlanHelper.getExecutable(executionPlan, completed);
        //a Chain finishes if no engine is running and no more nodes are executable
        if(!failed) { 
            executeable.removeAll(running);
            if(log.isInfoEnabled()){
                Collection<String> engines = new ArrayList<String>(executeable.size());
                for(NonLiteral node : executeable){
                    engines.add(getEngine(executionPlan, node));
                }
                log.info("MARK {} as executeable",engines);
            }
            this.executable = Collections.unmodifiableSet(executeable);
        } else {
            //do not mark engines as executeable if chain already failed
            this.executable = Collections.emptySet();
        }
    }
    /**
     * Getter for the executable nodes.
     * @return the nodes that can be executed next based on the completed and
     * currently running engines.
     */
    public Set<NonLiteral> getExecutable(){
        log.debug("++ r: {}","getExecutable");
        readLock.lock();
        log.debug(">> r: {}","getExecutable");
        try {
            return executable;
        } finally {
            log.debug("<< r: {}","getExecutable");
            readLock.unlock();  
        }
    }
    /**
     * Returns true if this chain has finished.
     * @return if this enhancement job is finished.
     */
    public boolean isFinished(){
        log.debug("++ r: {}","isFinished");
        readLock.lock();
        try {
            log.debug(">> r: {}","isFinished");
            return this.executable.isEmpty() && running.isEmpty();
        } finally {
            log.debug("<< r: {}","isFinished");
            readLock.unlock();
        }
    }

    public void setFailed(NonLiteral node, EnhancementEngine engine, Exception exception) {
        final boolean optional = isOptional(executionPlan, node);
        final String engineName = getEngine(executionPlan, node);
        log.debug("++ w: {}: {}","setFailed",ExecutionPlanHelper.getEngine(executionPlan, node));
        writeLock.lock();
        try {
            log.debug(">> w: {}: {}","setFailed",ExecutionPlanHelper.getEngine(executionPlan, node));
            if(!optional && !failed){ //the first errors for this chain
                failed = true;
                error = exception;
            }
            StringBuilder message = new StringBuilder();
            if(optional){
                message.append(String.format("Optional Engine '%s' of enhancement " +
                		"Chain '%s' skiped for ContentItem %s because the Engine",
                		engineName,chain,contentItem.getUri()));
            } else {
                message.append(String.format("Failed to enhance ContentItem '%s' by using " +
                		"enhancement chain '%s' because the required Enhancement Engine %s ",
                		contentItem.getUri(),chain, engineName));
            }
            if(engine == null){
                message.append("is currently not active");
            } else {
                message.append(String.format("was unable to process the content " +
                		"(Engine class: %s)",engine.getClass().getName()));
            }
            if(exception != null){
                message.append("(reason:").append(exception.getMessage()).append(')');
            }
            message.append('!');
            if(optional){
                log.info(message.toString(),exception);
            } else {
                errormessages.add(message.toString());
            }
            setCompleted(node); //we are done with that node!
        } finally {
            log.debug("<< w: {}: {}","setFailed",ExecutionPlanHelper.getEngine(executionPlan, node));
            writeLock.unlock();
        }

    }
    /**
     * Getter for the failed state. Note that EnhancementJobs might be already
     * failed but not yet finished.
     * @return if the EnhancementJob has failed or not.
     */
    public boolean isFailed() {
        log.debug("++ r: {}","isFailed");
        readLock.lock();
        try {
            log.debug(">> r: {}","isFailed");
            return failed;
        } finally {
            log.debug("<< r: {}","isFailed");
            readLock.unlock();
        }
    }
    
    @Override
    public int hashCode() {
        return contentItem.getUri().hashCode();
    }
    @Override
    public boolean equals(Object o) {
        return o instanceof EnhancementJob && 
                contentItem.getUri().equals(((EnhancementJob)o).contentItem.getUri());
    }
    @Override
    public String toString() {
        return "EnhancementJob for ContentItem "+contentItem.getUri();
    }
    /**
     * If {@link #isFailed()} this can be used to retrieve the message of the
     * occurred error.
     * @return the message of the error that caused the enhancement job to fail.
     */
    public String getErrorMessage(){
        return errormessages == null || errormessages.isEmpty() ? null : errormessages.get(0);
    }
    /**
     * if {@link #isFailed()} this may contain the {@link Exception} that caused
     * the enhancement job to fail. 
     * @return The Exception or <code>null</code> if no exception is available
     */
    public Exception getError(){
        return error;
    }
}
