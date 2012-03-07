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

// Create the HTTPHelper
if (!this.PSTORE.HTTPHelper) {
	this.PSTORE.HTTPHelper = {};
}

(function() {
	var httphelper = this.PSTORE.HTTPHelper;

	httphelper.send = function(method, uri, normalize, override, name, value) {
		var request = null;
		if (window.XMLHttpRequest) {
			request = new XMLHttpRequest();
		} else if (window.ActiveXObject) {
			request = new ActiveXObject("Msxml2.XMLHTTP");
		}
		if (normalize) {
			uri = httphelper.normalize(uri);
		}
		request.open(method, uri, false);
		if (override) {
			request.setRequestHeader('X-HTTP-Method-Override', 'DELETE');
		}
		if (method === 'POST' || override) {
			var data = name + '=' + value;
			request.setRequestHeader("Content-Type",
					"application/x-www-form-urlencoded");
			request.send(data);
		} else {
			request.send();
		}
		location.reload('false');
	};

	httphelper.normalize = function(uri) {
		var n = uri.lastIndexOf("#");
		uri = uri.substring(0, n) + "/" + uri.substring(n + 1, uri.length);
		return uri;
	};

}());

function getResponse(method, uri, acceptHeader) {
	if (typeof acceptHeader == "undefined") {
		acceptHeader = "application/xml";
	}
	var request = new XMLHttpRequest();
	request.open(method, uri, false);
	request.setRequestHeader("Accept", acceptHeader);
	request.send();
	var headers = request.getAllResponseHeaders();

	return ("HTTP/1.1 " + request.status + " " + request.statusText + "\n"
			+ headers + "\n\n" + request.responseText.replace(/</gi, "&lt;")
			.replace(/>/gi, "&gt;"));

}
