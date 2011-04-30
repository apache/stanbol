package org.apache.stanbol.cmsadapter.jcr.processor;

import javax.jcr.Session;

import org.apache.stanbol.cmsadapter.servicesapi.helper.OntologyResourceHelper;
import org.apache.stanbol.cmsadapter.servicesapi.mapping.MappingEngine;
import org.apache.stanbol.cmsadapter.servicesapi.repository.RepositoryAccess;

import com.hp.hpl.jena.ontology.OntModel;

public abstract class JCRProcessor {
    protected MappingEngine engine;
    protected Session session;
    protected RepositoryAccess accessor;
    protected OntologyResourceHelper ontologyResourceHelper;
    protected OntModel jcrOntModel;

    public JCRProcessor(MappingEngine engine) {
        this.engine = engine;
        this.session = (Session) engine.getSession();
        this.ontologyResourceHelper = this.engine.getOntologyResourceHelper();
        this.jcrOntModel = this.engine.getOntModel();
        this.accessor = this.engine.getRepositoryAccessManager()
                .getRepositoryAccess(this.engine.getSession());
        if (this.accessor == null) {
            throw new IllegalArgumentException("Can not find suitable accessor");
        }
    }

    
    protected boolean matches(String path, String query) {
        if (path != null) {
            if (query.endsWith("%")) {
                return path.contains(query.substring(0, query.length() - 2));
            } else {
                return path.equals(query);
            }
        } else {
            return false;
        }
    }
}
