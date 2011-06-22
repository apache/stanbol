package org.apache.stanbol.factstore.api;

import org.apache.stanbol.factstore.model.FactSchema;

public interface FactStore {

    public int getMaxFactSchemaURNLength();
    
    public boolean existsFactSchema(String factSchemaURN) throws Exception;

    public FactSchema getFactSchema(String factSchemaURN);

    public void createFactSchema(FactSchema factSchema) throws Exception;
    
}
