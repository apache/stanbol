/*
* Licensed to the Apache Software Foundation (ASF) under one or more
* contributor license agreements.  See the NOTICE file distributed with
* this work for additional information regarding copyright ownership.
* The ASF licenses this file to You under the Apache License, Version 2.0
* (the "License"); you may not use this file except in compliance with
* the License.  You may obtain a copy of the License at
*
*     http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/
package org.apache.stanbol.factstore.model;

import java.util.ArrayList;
import java.util.List;

import org.apache.stanbol.commons.jsonld.JsonLd;
import org.apache.stanbol.commons.jsonld.JsonLdResource;
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
		if (rows != null && !rows.isEmpty()) {
		    int rowCount = 0;
			for (FactResult result : rows) {
			    rowCount++;
			    JsonLdResource subject = new JsonLdResource();
			    subject.setSubject("R" + rowCount);
				for (int i = 0; i < header.size(); i++) {
					subject.putProperty(header.get(i), result.getValues().get(i));
				}
				root.put(subject);
			}
		}

		return root.toString();
	}
}
