package org.apache.stanbol.autotagging.jena;

import com.hp.hpl.jena.rdf.model.Resource;

public class ResourceInfo {

    public final Resource resource;

    public final Double score;

    public ResourceInfo(Resource resource, Double score) {
        this.resource = resource;
        this.score = score;
    }

}
