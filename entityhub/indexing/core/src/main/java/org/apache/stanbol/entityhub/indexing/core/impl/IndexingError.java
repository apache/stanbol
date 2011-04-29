/**
 * 
 */
package org.apache.stanbol.entityhub.indexing.core.impl;

public class IndexingError {
    private final Exception ex;
    private final String msg;
    private final String entityId;
    public IndexingError(String id,String msg,Exception ex){
        this.entityId = id;
        this.msg = msg;
        this.ex = ex;
    }
    /**
     * @return the ex
     */
    public Exception getException() {
        return ex;
    }
    /**
     * @return the msg
     */
    public String getMessage() {
        return msg;
    }
    /**
     * @return the entityId
     */
    public String getEntity() {
        return entityId;
    }
}