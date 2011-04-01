//Create the PSTORE object if one does not exist. 
if (!this.PSTORE) {
	this.PSTORE = {};
}

// Create the propUtil and oPropUtil object if one does not exist
if (!this.PSTORE.propUtil) {
	this.PSTORE.propUtil = {};
}

// Create the property and origProperty object which resembles the property
// shown in the html page
if (!this.PSTORE.propUtil.property) {
	this.PSTORE.propUtil.property = {};
}

if (!this.PSTORE.propUtil.originalProperty) {
	this.PSTORE.propUtil.originalProperty = {};
}

(function () {
	// Define variables to make code more readable
	var propUtil = this.PSTORE.propUtil;
	var property = this.PSTORE.propUtil.property;
	var originalProperty = this.PSTORE.propUtil.originalProperty;

	// Although super properties of the property is a list, this methods just
	// adds a
	// new one to them
	propUtil.setSubPropertyOf = function (superProp) {
		property.subPropertyOf = superProp;
	};

	propUtil.setFunctional = function (bool) {
		property.isFunctional = bool;
	};

	propUtil.setTransitive = function (bool) {
		property.isTransitive = bool;
	};

	propUtil.setSymmetric = function (bool) {
		property.isSymmetric = bool;
	};

	propUtil.setInverseFunctional = function (bool) {
		property.isInverseFunctional = bool;
	};

	// Send the modified property to the server and refresh the page
	propUtil.post = function (uri) {
		var data = "";
		var xmlhttp;
		
		//Create request object
		if (window.XMLHttpRequest) {
			xmlhttp = new XMLHttpRequest();
		} else if (window.ActiveXObject) {
			xmlhttp = new ActiveXObject("Msxml2.XMLHTTP");
		}

		if (typeof property.isFunctional !== 'undefined') {
			data += "isFunctional=" + property.isFunctional + "&";
		}
		if (typeof property.isSymmetric !== 'undefined') {
			data += "isSymmetric=" + property.isSymmetric + "&";
		}
		if (typeof property.isTransitive !== 'undefined') {
			data += "isTransitive=" + property.isTransitive + "&";
		}
		if (typeof property.isInverseFunctional !== 'undefined') {
			data += "isInverseFunctional=" + property.isInverseFunctional + "&";
		}
		if (typeof property.subPropertyOf !== 'undefined') {
			data += "subPropertyOf=" + property.subPropertyOf + "&";
		}

		xmlhttp.open('POST', uri, false);
		xmlhttp.onreadystatechange = function () {
			if (xmlhttp.readyState != 4) {
				alert(xmlhttp.responseText);
			}
			window.location.reload('false');
		}
		xmlhttp.setRequestHeader("Content-Type",
				"application/x-www-form-urlencoded");
		xmlhttp.setRequestHeader("Accept", "application/xml");
		xmlhttp.send(data);

	};
}());