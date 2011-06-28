package org.apache.stanbol.factstore.api;

import java.util.Set;

import org.apache.stanbol.factstore.model.Fact;
import org.apache.stanbol.factstore.model.FactSchema;

public interface FactStore {

    public int getMaxFactSchemaURNLength();
    
    public boolean existsFactSchema(String factSchemaURN) throws Exception;

    public FactSchema getFactSchema(String factSchemaURN);

    public void createFactSchema(FactSchema factSchema) throws Exception;
    
    public void addFact(Fact fact) throws Exception;
    
    public void addFacts(Set<Fact> factSet) throws Exception;
}
