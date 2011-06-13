package org.apache.stanbol.cmsadapter.cmis.repository;

import org.apache.chemistry.opencmis.client.api.ObjectId;

public class CMISObjectId implements ObjectId {

    CMISObjectId(String id) {
        this.id = id;
    }

    private String id;

    public static ObjectId getObjectId(String id) {
        return new CMISObjectId(id);
    }

    @Override
    public String getId() {
        return id;
    }

}
