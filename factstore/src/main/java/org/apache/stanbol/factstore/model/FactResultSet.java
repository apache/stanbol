package org.apache.stanbol.factstore.model;

import java.util.ArrayList;
import java.util.List;

import org.apache.stanbol.commons.jsonld.JsonLd;
import org.apache.stanbol.commons.jsonld.JsonLdResource;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FactResultSet {
	private static Logger logger = LoggerFactory.getLogger(FactResultSet.class);

	private List<String> header;
	private List<FactResult> rows;

	public List<String> getHeader() {
		return header;
	}

	public List<FactResult> getRows() {
		return rows;
	}

	public void setHeader(List<String> header) {
		this.header = header;
	}

	public void addFactResult(FactResult result) {
		if (this.rows == null) {
			rows = new ArrayList<FactResult>();
		}
		rows.add(result);
	}

	public String toJSON() {
		JsonLd root = new JsonLd();

		JsonLdResource subject = new JsonLdResource();
		JSONArray resultset = new JSONArray();

		if (rows != null && !rows.isEmpty()) {
			for (FactResult result : rows) {
				JSONObject value = new JSONObject();
				for (int i = 0; i < header.size(); i++) {
					try {
						value.put(header.get(i), result.getValues().get(i));
					} catch (JSONException e) {
						logger.warn("Error creating JSON from FactResultSet. {}", e
								.getMessage());
					}
				}
				resultset.put(value);
			}
		}

		subject.putProperty("resultset", resultset);
		root.put(subject);

		return root.toString();
	}
}
