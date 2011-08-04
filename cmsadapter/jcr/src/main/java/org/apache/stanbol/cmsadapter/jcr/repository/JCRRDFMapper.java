package org.apache.stanbol.cmsadapter.jcr.repository;

import java.util.Iterator;

import org.apache.clerezza.rdf.core.MGraph;
import org.apache.clerezza.rdf.core.Triple;
import org.apache.clerezza.rdf.core.UriRef;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Service;
import org.apache.stanbol.cmsadapter.servicesapi.helper.CMSAdapterVocabulary;
import org.apache.stanbol.cmsadapter.servicesapi.mapping.RDFMapper;
import org.apache.stanbol.entityhub.servicesapi.defaults.NamespaceEnum;

@Component(immediate = true)
@Service
public class JCRRDFMapper implements RDFMapper {

	@Override
	public void storeRDFinRepository(Object session, MGraph annotatedGraph) {
		Iterator<Triple> cmsObjectIt = annotatedGraph.filter(null, new UriRef(NamespaceEnum.rdf + "Type"),
				CMSAdapterVocabulary.CMS_OBJECT);
		
	}

}
