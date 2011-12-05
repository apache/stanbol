package org.apache.stanbol.entityhub.ldpath.backend;

import org.apache.stanbol.entityhub.core.mapping.ValueConverterFactory;
import org.apache.stanbol.entityhub.servicesapi.Entityhub;

public class EntityhubBackend extends YardBackend {

    public EntityhubBackend(Entityhub entityhub) {
        this(entityhub,null);
    }
    public EntityhubBackend(Entityhub entityhub,ValueConverterFactory valueConverter) {
        super(entityhub.getYard(),valueConverter);
    }
    
}
