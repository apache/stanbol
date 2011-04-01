//Create the PSTORE object if one does not exist. 
if (!this.PSTORE) {
	this.PSTORE = {};
}

//Create the HTTPHelper
if (!this.PSTORE.HTTPHelper) {
	this.PSTORE.HTTPHelper = {};
}

(function () {
	var httphelper = this.PSTORE.HTTPHelper;
	
	httphelper.send = function(method, uri, normalize, override, name, value){
		var request  = null;
		if (window.XMLHttpRequest) {
			request = new XMLHttpRequest();
		} else if (window.ActiveXObject) {
			request = new ActiveXObject("Msxml2.XMLHTTP");
		}
		if(normalize){
			uri = httphelper.normalize(uri);
		}
		request.open(method, uri, false);
		if(override){
			request.setRequestHeader('X-HTTP-Method-Override', 'DELETE');	
		}
		if(method === 'POST' || override){
			var data = name + '=' + value;
			request.setRequestHeader("Content-Type",
			"application/x-www-form-urlencoded");
			request.send(data);
		}else{
			request.send();
		}
		location.reload('false');
	};
	
	httphelper.normalize = function(uri){
		var n = uri.lastIndexOf("#");
		uri =  uri.substring(0,n) + "/" + uri.substring(n+1, uri.length);
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
