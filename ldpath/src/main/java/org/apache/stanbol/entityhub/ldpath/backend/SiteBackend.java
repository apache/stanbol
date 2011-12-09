package org.apache.stanbol.entityhub.ldpath.backend;

import org.apache.stanbol.entityhub.core.mapping.ValueConverterFactory;
import org.apache.stanbol.entityhub.core.model.InMemoryValueFactory;
import org.apache.stanbol.entityhub.servicesapi.EntityhubException;
import org.apache.stanbol.entityhub.servicesapi.model.Entity;
import org.apache.stanbol.entityhub.servicesapi.model.Representation;
import org.apache.stanbol.entityhub.servicesapi.model.ValueFactory;
import org.apache.stanbol.entityhub.servicesapi.query.FieldQuery;
import org.apache.stanbol.entityhub.servicesapi.query.QueryResultList;
import org.apache.stanbol.entityhub.servicesapi.site.ReferencedSite;

public class SiteBackend extends AbstractBackend {

    protected final ReferencedSite site;
    private final ValueFactory vf;
    
    public SiteBackend(ReferencedSite site) {
        this(site,null,null);
    }
    public SiteBackend(ReferencedSite site,ValueFactory vf) {
        this(site,vf,null);
    }
    public SiteBackend(ReferencedSite site,ValueFactory vf,ValueConverterFactory valueConverter) {
        super(valueConverter);
        if(site == null){
            throw new IllegalArgumentException("The parsed ReferencedSite MUST NOT be NULL");
        }
        this.vf = vf == null ? InMemoryValueFactory.getInstance():vf;
        this.site = site;
    }
    @Override
    protected FieldQuery createQuery() {
        return site.getQueryFactory().createFieldQuery();
    }
    @Override
    protected Representation getRepresentation(String id) throws EntityhubException {
        Entity entity = site.getEntity(id);
        return entity != null ? entity.getRepresentation():null;
    }
    @Override
    protected ValueFactory getValueFactory() {
        return vf;
    }
    @Override
    protected QueryResultList<String> query(FieldQuery query) throws EntityhubException {
        return site.findReferences(query);
    }
    

}
