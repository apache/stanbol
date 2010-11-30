package eu.iksproject.fise.servicesapi;

import java.net.URI;
import java.util.Collection;
import java.util.Date;

import org.apache.clerezza.rdf.core.MGraph;
import org.apache.clerezza.rdf.core.UriRef;
import org.apache.clerezza.rdf.core.impl.SimpleMGraph;

import eu.iksproject.fise.servicesapi.helper.Rdf;
import eu.iksproject.fise.servicesapi.helper.RdfEntity;
import eu.iksproject.fise.servicesapi.helper.RdfEntityFactory;

/**
 * This Interface represents a Fise enhancement.
 * To create an instance of this interface use the following code
 * <code><pre>
 *  ContentItem ci;
 * 	MGraph graph = ci.getMetadata();
 *  RdfEntityFactory factory = RdfEntityFactory.createInstance(graph);
 *	String enhancementId = "http://wwww.example.com/iks-project/fise/example-enhancement";
 *	UriRef enhancementNode = new UriRef(enhancementId);
 *	Enhancement enhancement = factory.getProxy(enhancementNode, Enhancement.class);
 *	enhancement.setCreator("Rupert Westenthaler");
 *  enhancement.setCreated(new Date());
 *  ...
 * </pre></code>
 * @author Rupert Westenthaler
 *
 */
@Rdf(id="http://fise.iks-project.eu/ontology/Enhancement")
public interface Enhancement extends RdfEntity{
	
	@Rdf(id="http://purl.org/dc/terms/creator")
	UriRef getCreator();
	@Rdf(id="http://purl.org/dc/terms/creator")
	void setCreator(UriRef creator);

	@Rdf(id="http://purl.org/dc/terms/created")
	void setCreated(Date date);
	@Rdf(id="http://purl.org/dc/terms/created")
	Date getCreated();
	
//	@Rdf(id="http://purl.org/dc/terms/type")
//	void setDcType(Collection<URI> types);
	@Rdf(id="http://purl.org/dc/terms/type")
	Collection<UriRef> getDcType();
	
	@Rdf(id="http://fise.iks-project.eu/ontology/confidence")
	Double getConfidence();
	@Rdf(id="http://fise.iks-project.eu/ontology/confidence")
	void setConfidence(Double value);
	
	@Rdf(id="http://fise.iks-project.eu/ontology/extracted-from")
	UriRef getExtractedFrom();
	@Rdf(id="http://fise.iks-project.eu/ontology/extracted-from")
	void setExtractedFrom(UriRef contentItem);
	
	@Rdf(id="http://purl.org/dc/terms/requires")
	Collection<Enhancement> getRequires();
//	@Rdf(id="http://purl.org/dc/terms/requires")
//	void setRequires(Collection<Enhancement> required);

	@Rdf(id="http://purl.org/dc/terms/relation")
	Collection<Enhancement> getRelations();
//	@Rdf(id="http://purl.org/dc/terms/relation")
//	void setRelation(Collection<Enhancement> related);
}
