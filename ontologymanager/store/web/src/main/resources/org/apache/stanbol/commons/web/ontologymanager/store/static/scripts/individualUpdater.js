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
//Create the PSTORE object if one does not exist. 
if (!this.PSTORE) {
	this.PSTORE = {};
}

// Create indUtil object if does not exist
if (!this.PSTORE.indUtil) {
	this.PSTORE.indUtil = {};
}

// Create individual if does not exist
if (!this.PSTORE.indUtil.individual) {
	this.PSTORE.indUtil.individual = {}
	this.PSTORE.indUtil.individual.containerClass = null;
	this.PSTORE.indUtil.individual.updateContext = "data";
	this.PSTORE.indUtil.individual.literal = null;
	this.PSTORE.indUtil.individual.property = null;
	this.PSTORE.indUtil.individual.individual = null;
}

(function() {
	// Define variables to make code more readable
	var indUtil = PSTORE.indUtil;
	var ind = PSTORE.indUtil.individual;

	indUtil.setPropertyValue = function(value) {
		if (individual.updateContext == "object") {
			individual.individual = value;
			individual.literal = null;
		} else if (individual.updateContext == "data") {
			individual.literal = value;
			individual.individual = null;
		}
	};

	indUtil.setProperty = function(property) {
		ind.property = property;
	};

	indUtil.post = function(uri) {
		var data = "";
		if (ind.containerClass != null) {
			data += "additionalContainerClassURI=" + ind.containerClass + "&";
		}

		if (ind.updateContext == "data" && ind.literal != null
				&& ind.property != null) {
			data += "propertyURI=" + ind.property + "&";
			data += "literal=" + ind.literal + "&";
		}
		if (ind.updateContext == "object" && ind.individual != null
				&& ind.property != null) {
			data += "propertyURI=" + ind.property + "&";
			data += "individualAsValueURI=" + ind.individual + "&";
		}

		var xmlhttp = new XMLHttpRequest();
		xmlhttp.open('POST', uri, false);
		xmlhttp.setRequestHeader("Content-Type",
				"application/x-www-form-urlencoded");
		xmlhttp.setRequestHeader("Accept", "text/html");
		xmlhttp.send(data);
		location.reload('false');
	};
}());