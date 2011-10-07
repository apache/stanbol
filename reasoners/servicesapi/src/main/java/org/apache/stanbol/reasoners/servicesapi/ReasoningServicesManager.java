package org.apache.stanbol.reasoners.servicesapi;

import java.util.Set;

public interface ReasoningServicesManager {

    public abstract int size();

    public abstract ReasoningService<?,?,?> get(String path) throws UnboundReasoningServiceException;

    public abstract Set<ReasoningService<?,?,?>> asUnmodifiableSet();

}
