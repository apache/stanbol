package org.apache.stanbol.entityhub.indexing.core.event;

import java.util.EventObject;

import org.apache.stanbol.entityhub.indexing.core.impl.IndexerImpl;

public class IndexingEvent extends EventObject {

    private static final long serialVersionUID = 1L;
    public IndexingEvent(IndexerImpl source) {
        super(source);
    }
    
    @Override
    public IndexerImpl getSource() {
        // TODO Auto-generated method stub
        return (IndexerImpl)super.getSource();
    }

    

}
