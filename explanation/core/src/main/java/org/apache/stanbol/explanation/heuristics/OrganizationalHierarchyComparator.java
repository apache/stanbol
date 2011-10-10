package org.apache.stanbol.explanation.heuristics;

import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLIndividual;

/**
 * Transitive comparator for determining if one social person is another person's boss in a hierarchical
 * organization.
 * 
 * @author alessandro
 * 
 */
public class OrganizationalHierarchyComparator implements Comparator {

    private String NS_ORG = "http://www.w3.org/ns/org#";

    private IRI reportsTo = IRI.create(NS_ORG + "reportsTo");

    @Override
    public int compare(Entity arg0, Entity arg1) throws IncomparableException {

        if (!(arg0.getOWLEntity() instanceof OWLIndividual)) throw new IncomparableException(arg0, arg1);
        if (!(arg1.getOWLEntity() instanceof OWLIndividual)) throw new IncomparableException(arg0, arg1);

        // TODO: checks that the two individuals are not of types disjoint with foaf:Agent

        if (arg1.getRelatedEntities(reportsTo).contains(arg0)) return 1;
        if (arg0.getRelatedEntities(reportsTo).contains(arg1)) return -1;
        return 0;
    }

}
