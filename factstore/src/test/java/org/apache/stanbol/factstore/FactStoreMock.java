package org.apache.stanbol.factstore;

import org.apache.stanbol.commons.jsonld.JsonLdCommon;
import org.apache.stanbol.factstore.api.FactStore;
import org.apache.stanbol.factstore.model.FactSchema;

public class FactStoreMock implements FactStore {

    public void createFactSchema(String factSchemaURN, JsonLdCommon jsonLd) throws Exception {}

    public boolean existsFactSchema(String factSchemaURN) throws Exception {
        return false;
    }

    @Override
    public int getMaxFactSchemaURNLength() {
        return 96;
    }

    @Override
    public void createFactSchema(FactSchema factSchema) throws Exception {
        // TODO Auto-generated method stub
        
    }

    @Override
    public FactSchema getFactSchema(String factSchemaURN) {
        // TODO Auto-generated method stub
        return null;
    }

}
