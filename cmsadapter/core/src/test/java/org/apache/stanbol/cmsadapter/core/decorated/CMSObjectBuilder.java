package org.apache.stanbol.cmsadapter.core.decorated;

import static org.apache.stanbol.cmsadapter.core.decorated.NamingHelper.LOCAL_NAME;
import static org.apache.stanbol.cmsadapter.core.decorated.NamingHelper.NAMESPACE;
import static org.apache.stanbol.cmsadapter.core.decorated.NamingHelper.PATH;
import static org.apache.stanbol.cmsadapter.core.decorated.NamingHelper.UNIQUE_REF;

import org.apache.stanbol.cmsadapter.servicesapi.model.web.CMSObject;
import org.apache.stanbol.cmsadapter.servicesapi.model.web.ObjectFactory;

// Unnecessarily, eagerly builds objects but OK for tests.
public class CMSObjectBuilder {

    private static ObjectFactory of = new ObjectFactory();
    private CMSObject instance = of.createCMSObject();
    private String prefix;

    public CMSObjectBuilder(String prefix) {
        this.prefix = prefix;
        instance.setUniqueRef(prefix + UNIQUE_REF);
        instance.setLocalname(prefix + LOCAL_NAME);
        instance.setPath(prefix + PATH);
    }

    public CMSObjectBuilder(String prefix, String id, String name, String path) {
        this.prefix = prefix;
        instance.setUniqueRef(prefix + id);
        instance.setLocalname(prefix + name);
        instance.setPath(prefix + path);
    }

    public CMSObjectBuilder namespace() {
        instance.setNamespace(prefix + NAMESPACE);
        return this;
    }

    public CMSObjectBuilder namespace(String namespace) {
        instance.setNamespace(prefix + namespace);
        return this;
    }

    public CMSObjectBuilder child(CMSObject child) {
        instance.getChildren().add(child);
        return this;
    }

    public CMSObject build() {
        return instance;
    }

}