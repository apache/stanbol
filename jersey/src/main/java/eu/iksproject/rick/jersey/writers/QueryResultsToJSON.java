package eu.iksproject.rick.jersey.writers;

import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;

import eu.iksproject.rick.servicesapi.model.Representation;
import eu.iksproject.rick.servicesapi.model.Sign;
import eu.iksproject.rick.servicesapi.query.QueryResultList;

final class QueryResultsToJSON {
	
	private QueryResultsToJSON() { /* do not create instances of utility classes */}

	static <T> JSONObject toJSON(QueryResultList<?> resultList) throws JSONException{
		JSONObject jResultList = new JSONObject();
		if(resultList.getQuery() != null){
			jResultList.put("query", FieldQueryToJSON.toJSON(resultList.getQuery()));
		}
		jResultList.put("results", convertResultsToJSON(resultList,resultList.getType()));
		return jResultList;
	}
	
	private static <T> JSONArray convertResultsToJSON(Iterable<?> results,Class<?> type) throws JSONException{
		JSONArray jResults = new JSONArray();
		if(String.class.isAssignableFrom(type)){
			for(Object result : results){
				jResults.put(result);
			}
		} else if(Representation.class.isAssignableFrom(type)){
			for(Object result : results){
				jResults.put(SignToJSON.toJSON((Representation)result));
			}
		} else if(Sign.class.isAssignableFrom(type)){
			for(Object result : results){
				jResults.put(SignToJSON.toJSON((Sign)result));
			}
		}
		return jResults;
	}
}
