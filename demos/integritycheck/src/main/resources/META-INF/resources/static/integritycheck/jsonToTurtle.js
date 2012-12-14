/**
 * Constructor: ex) var json = new JsonTurtle;
 */
function JsonTurtle(){
	this.rdfprop = 0;
	return this;
}

/**
 * Get JSON data from a field, convert it to Turtle, and put into another field
 * @param	jfld : id of textarea that stores JSON data
 * @param	tfld : id of textarea to put Turtle serialization
 */
JsonTurtle.prototype.j2t = function(jfld, tfld){
	var j, t;
	j = "{'safeval':" + document.getElementById(jfld).value + "}";
	try{
		eval('var d = ' + j);
		document.getElementById(tfld).value = this.parsej(d.safeval,"\n",'');
	}catch(e){
		alert("Error. Possibly dangerous code:\n" + e);
	}
}

/**
 * Converts a (JSON) object to RDF/Turtle triples.
 * @param	obj : data object
 * @param	sep : triple separator (usually \n)
 * @param	pre : indent string. also indicates nest level.
 * @param	called_by : indicates whether parent 'element' is a Class.
 * @return	RDF/Turtle triples.
 */
JsonTurtle.prototype.parsej = function (obj, sep, pre, called_by){
	var res = ''; //result
	var arg = {
		'pfdcl' : '', // prefix decl
		'caller': called_by,
		's' : '', // subject of triple
		'ctype' : '' // classtype
	}; 
	var s, p, o; // triple s,p,o
	var key; // JSON object key
	if(pre == '')
		this.rdfprop = 0; // capture whether rdf: property is used

	switch(typeof(obj)){
	case "string":
		return this.set_object(obj);
	case "boolean":
		return '"' + obj + '"';
	case "number":
		return obj;
	case "undefined":
		return '""';
	}
	// without abov switch, {"p":["abc"]} will be 
	// [:p [rdf:_1 "a"; rdf:_2 "b"; rdf:_3 "c"]] .
	// so, case object:
	for(key in obj){
		//-- skip RDF syntax vocab.
		switch(key){
		case 'rdf:resource':
			return '<' + obj[key] + '>';
		case 'rdf:parseType':
			continue;
		case 'rdf:RDF':
			return this.parsej(obj[key],sep,'',key);
		case 'rdf:Description':
			return this.prepare_pfx(arg, sep) + this.parsej(obj[key],sep,'',key);
		case '#text':
			return '"' + obj[key] + '"';
		}
		
		//-- property name to QName
		p = this.set_qname(key);

		//-- set appropriate triple according to object type
		switch(typeof(obj[key])){
		case "object":
			res += this.proc_object(obj[key], p, pre, sep, arg);
			break;
		case "string":
			o = this.set_object(obj[key]);
			res += this.set_po(key, p, o, pre, sep, arg);
			break;
		case "number":
		//case "boolean":
			res += pre + p + ' ' + obj[key] + ';' + sep;
			break;
		default:
			res += pre + p + ' "' + obj[key] + '";' + sep;
		}
	}

	//-- add bNode [] or collection (), or regard as continuing p-o seqence.
	res = res ? res.slice(0,-2) : (p ? p : ":_undefined") + ' ""';
	res = (arg.s && pre=='') ? arg.s + res :
		(arg.ctype == 'Collection' ? 
			'(' + sep + res + sep + pre + ')' :
		 	((arg.caller == 'C' || (pre == '' && arg.ctype)) ? sep + res:
			'[' + sep + res + sep + pre + ']')
		);
	return pre ? res : 
		this.prepare_pfx(arg, sep) + res + ' .';
}


/**
 * processes value tyep 'object' for JSON data
 * @param	val : JSON proprerty value (obj[key])
 * @param	p : property QName
 * @param	sep : triple separator (usually \n)
 * @param	pre : indent string. also indicates nest level.
 * @param	arg : object of pfdcl, caller, s, ctype, by reference (set val)
 * @return	resulting triple
 */
JsonTurtle.prototype.proc_object = function(val, p, pre, sep, arg){
	var res, reslist = '', triple, s, ep, pp;
	var o = {'ctype' : ''};
	if(val == null)
		res = pre + p + ' "";' + sep;

	// case Array: treat array as repeated property, not a container
	else if(val instanceof Array){//val.constructor == Array
		var i;
		for(i=0; i<val.length; i++){
			pp = (p == 'rdf:li') ? 'rdf:_' + (i+1) : p;
			triple = this.proc_obj_cp(val[i], pp, pre, sep, ' ', arg.caller, o);
			reslist += o.ctype ? 
				pre + '[' + triple + '] ' + sep :
				triple;
		}
		// Loop here in order to have repeated property for array
		// rather than RDF container model. e.g. for
		// {p1: [ {p2: "v1"}, {p2: "v2"}]}
		// not [:p1 [ rdf:_1 [:p2 "v1"]; rdf:_2 [:p2 "v2"]]] . (1)
		// but [:p1 [:p2 "v1"]; p1 [:p2 "v2"]] . (2)
		// bellow commented code generates (1)
		/*triple = this.proc_obj_cp(val, p, pre, sep, ' ', arg.caller, o);
		reslist = o.ctype ? 
			pre + '[' + triple + '] ' + sep :
			triple;*/

		// o.ctype is set 'a :Classname' by proc_obj_cp
		if(o.ctype){// repeated Class -> Collection
			if(pre == ''){
				arg.s = '[]';
				res = sep + ':has_a (' + sep + reslist + '); '
			}else{
				o.ctype = 'Collection';
				//res = '(' + sep + reslist + pre + ')  ';
				res = reslist;
			}
		}else
			res = reslist;
	
	// case Hash object:
	}else{
		if(pre == ''){
			ep = '.';
			arg.caller = 'Root';
		}else
			ep = ';';
		res = this.proc_obj_cp(val, p, pre, sep, ep, arg.caller, o);
		if(pre == '' && o.ctype) arg.caller = 'Root';
	}
	arg.ctype = o.ctype;
	return res;
}


/**
 * Processes object type data as class or property. If the property name 
 * starts with a capital letter, regard it as a Class
 * @param	obj : a data object
 * @param	p : property whose value is obj
 * @param	pre : indent string. also indicates nest level.
 * @param	sep : triple separator (usually \n)
 * @param	ep : end point of a triple in case a Class (usually '.')
 * @param	caller : indicates whether parent 'element' is a Class.
 * @param	o : object of ctype (to return value)
 * @return	composed Turle
 */
JsonTurtle.prototype.proc_obj_cp = function(obj, p, pre, sep, ep, caller, o){
	var res;
	
	if((p.split(':')[1]).match(/^[A-Z]/) && caller != 'C'){
		// if local name starts with a capital, regard as Class
		// and treat nested object as p/o of the same subject
		o.ctype = ' a ' + p + ';';
		res = pre + o.ctype + this.parsej(obj, "\n", '  ' + pre, 'C');
		if(caller == 'Root') 
			// list of classes called by root will be each independent
			res = '[' + res + sep + ']' + ep + sep;
		else
			res += ep + sep;
		// (only top level can have '.' for class, i.e. parallel triples)
	}else{
		// else treat as bNode with predicateObjectList
		res = pre + p + ' ' + this.parsej(obj, "\n", '  ' + pre) + ';' + sep;
	}
	return res;
}


/**
 * set appropriate QName for property
 * @param	key : JSON proprerty name (key in obj)
 * @return	QName
 */
JsonTurtle.prototype.set_qname = function(key){
	var p;
	if(key.indexOf(':') > -1)
		// if contains ':' regard it as Qname -- maybe need clean up
		p =  key;
	else if(key.match(/^\d+$/)){
		// Array has 0,1,2... as key --> RDF container member prop.
		p =  'rdf:_' + (key*1+1);
		this.rdfprop++;
	}else
		// treat as default namespaced name
		p = ':' + key; 
		
	// clean up property name
	p = p.replace(/[^-:\w\d\u00c0-\ueffff]/g,'_'); // pseudo namechar
	p = p.replace(/:([-\d])/,':_'+RegExp.$1); // local name start char

	return p;
}


/**
 * set appropriate object for JSON data value
 * @param	val : JSON proprerty value (obj[key])
 * @return	object in Turtle triple
 */
JsonTurtle.prototype.set_object = function(val){
	var o;
	// if uri ref
	if(val.match(/^<([-_\w\n\.~\?\&\/\@=;,#%:]+)>$/))
		o = "<" + RegExp.$1 + ">";
	//need this ? maybe should not ?
	else if(val.match(/^http:([-_\w\n\.~\?\&\/\@=;,#%]+)$/))
		o = "<http:" + RegExp.$1 + ">";
	// or bNode
	else if(val == "[]")
		o = "[]";
	// or QName
	else if(val.match(/^\w*:\w+$/))
		o = val;
	// or literal
	else{
		o = val.replace(/\n/g,"\\n");
		o = o.replace(/\"/g,"\\\"");
		o = '"' + o + '"';
	}

	return o;
}


/**
 * set property - object, or @prefix or subject, according to object value
 * @param	key : JSON object key (key in obj)
 * @param	p : property QName
 * @param	o : object
 * @param	pre : indent string. also indicates nest level.
 * @param	sep : triple separator (usually \n)
 * @param	arg : object of pfdcl, caller, s, ctype (set value for pfdcl, s)
 * @return	resulting part of triple
 */
JsonTurtle.prototype.set_po = function(key, p, o, pre, sep, arg){
	var res = '', pfx;
	if(key.match(/^(\@prefix|xmlns)/)){  // ns prefix decl
		pfx = key.split(':')[1];
		if(pfx == undefined) pfx = '';
		arg.pfdcl += "@prefix " + pfx + ": " + o + " ." + sep;
	}else if(key.match(/^(\@|rdf:)about/))  // subject node
		arg.s = o + sep;
	else if(p == ':a') // a (rdf:type)
		res = pre + 'a ' + o + ";" + sep;
	else
		res = pre + p + ' ' + o + ";" + sep;
	
	return res;
}


/**
 * Prepares ns prefix declarations. If no @prefix, add an default ns.
 * If model includes constructs from RDF ns and not ns ready, add one
 * @param	arg : object of pfdcl, caller, s, ctype
 * @param	sep : triple separator (usually \n)
 * @return	prepared @prefix directives
 */
JsonTurtle.prototype.prepare_pfx = function(arg, sep){
	var rdfns = '<http://www.w3.org/1999/02/22-rdf-syntax-ns#>';
	if(arg.pfdcl == '') /*&& arg.caller != 'Root'*/
		arg.pfdcl= "@prefix : <http://purl.org/net/ns/jsonrdf/> ." + sep;
	if(this.rdfprop && arg.pfdcl.indexOf(rdfns) == -1)
		arg.pfdcl += "@prefix rdf: " + rdfns + " ." + sep;
	return arg.pfdcl;
}
