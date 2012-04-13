package org.apache.stanbol.contenthub.search.featured;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.clerezza.rdf.core.UriRef;
import org.apache.stanbol.contenthub.servicesapi.search.SearchException;
import org.apache.stanbol.contenthub.servicesapi.search.featured.ConstrainedDocumentSet;
import org.apache.stanbol.contenthub.servicesapi.search.featured.Constraint;
import org.apache.stanbol.contenthub.servicesapi.search.featured.Facet;
import org.apache.stanbol.contenthub.servicesapi.search.featured.FeaturedSearch;

public class ConstrainedDocumentSetImpl implements ConstrainedDocumentSet {

    private String queryTerm;
    private FeaturedSearch featuredSearch;
    private Set<Constraint> constraints;
    private Set<Facet> facets;

    public ConstrainedDocumentSetImpl(String queryTerm, FeaturedSearch featuredSearch) throws SearchException {
        this.queryTerm = queryTerm;
        this.featuredSearch = featuredSearch;
        this.facets = new HashSet<Facet>();
        this.constraints = new HashSet<Constraint>();
    }
    
    @Override
    public List<UriRef> getDocuments() throws SearchException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Set<Constraint> getConstraints() {
        return this.constraints;
    }

    @Override
    public Set<Facet> getFacets() {
        return this.facets;
    }

    @Override
    public ConstrainedDocumentSet narrow(Constraint constraint) throws SearchException {
        Set<Constraint> newConstraints = new HashSet<Constraint>(getConstraints());
        newConstraints.add(constraint);
        return featuredSearch.search(queryTerm, newConstraints);
    }

    @Override
    public ConstrainedDocumentSet broaden(Constraint constraint) throws SearchException {
        Set<Constraint> newConstraints = new HashSet<Constraint>(getConstraints());
        newConstraints.remove(constraint);
        return featuredSearch.search(queryTerm, newConstraints);
    }
}
