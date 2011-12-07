package org.apache.stanbol.contenthub.core.utils;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.query.QueryExecutionFactory;
import com.hp.hpl.jena.query.QuerySolution;
import com.hp.hpl.jena.query.ResultSet;
import com.hp.hpl.jena.rdf.model.RDFNode;

/**
 * is written to find nearby places of dbpedia/place typed entities.
 * While finding nearby places, first gets the dbpedia/region and dbpedia/isPartOf
 * properties of the entity. Later on queries the dbpedia to find another entities
 * that have the same values at property dbpedia/region , dbpedia/isPartOf
 * @author srdc
 *
 */
public class NearByFinder {
	
	private static final Logger logger = LoggerFactory.getLogger(NearByFinder.class);
	
	private List<String> nearByProperties;
 	
	public boolean canFindNearby(OntModel resultModel) {
		StringBuilder qb = new StringBuilder(
				"PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>\n");
		qb.append("PREFIX dbp.ont: <http://dbpedia.org/ontology/>\n");
		qb.append("PREFIX about.ns: <http://www.iks-project.eu/ontology/rick/model/>\n");
		qb.append("ASK {\n");
		qb.append("?entity about.ns:about ?description.\n");
		qb.append("OPTIONAL {?description dbp.ont:region ?region}\n ");
		qb.append("OPTIONAL {?description dbp.ont:isPartOf ?isPartOf}\n ");
		qb.append("}");
		
		String query = qb.toString();
		
		if (resultModel != null) {
			boolean result = QueryExecutionFactory.create(query,resultModel).execAsk();
			logger.info("Ask query executed successfully and nearcy places can be found is {}",result);
			return result;
		}
		
		logger.info("Entity Model is null, no nearby places can be found");
		
		return false;
	}
	
	public NearByFinder(OntModel resultModel)
	{
		nearByProperties = new ArrayList<String>();
		
		if (canFindNearby(resultModel)) {
			StringBuilder qb = new StringBuilder(
					"PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>\n");
			qb.append("PREFIX dbp.ont: <http://dbpedia.org/ontology/>\n");
			qb.append("PREFIX about.ns: <http://www.iks-project.eu/ontology/rick/model/>\n");
			qb.append("SELECT DISTINCT ?isPartOf ?region WHERE {\n");
			qb.append("?entity about.ns:about ?description.\n");
			qb.append("OPTIONAL { ?description dbp.ont:isPartOf ?isPartOf}\n");
			qb.append("OPTIONAL { ?description dbp.ont:region ?region}\n");
			qb.append("}");
			
			String query = qb.toString();
			
			ResultSet resultSet = QueryExecutionFactory.create(query,resultModel).execSelect();
			while(resultSet.hasNext()) {
				QuerySolution sol = resultSet.next();
				RDFNode region = sol.get("region");
				RDFNode isPartOf = sol.get("isPartOf");
				
				if(region != null) {
					nearByProperties.add(region.toString());
				}
				
				if(isPartOf != null) {
					nearByProperties.add(isPartOf.toString());
				}
			}
			
		}
		
		logger.warn("Nearby places of entity CAN NOT be FOUND");
	}
	
	

}
