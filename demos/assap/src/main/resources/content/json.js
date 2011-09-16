var json_parse=(function(){
	var at,ch,escapee={'"':'"','\\':'\\','/':'/',b:'\b',f:'\f',n:'\n',r:'\r',t:'\t'},
	text,error=function(m){	
		throw{name:'SyntaxError',message:m,at:at,text:text}
	},
	next=function(c){
		if(c&&c!==ch){
			error("Expected '"+c+"' instead of '"+ch+"'")
		}
		ch=text.charAt(at);
		at+=1;
		return ch
	},
	number=function(){	
		var number,string='';
		if(ch==='-'){
			string='-';
			next('-')
		}
		while(ch>='0'&&ch<='9'){
			string+=ch;next()
		}
		if(ch==='.'){
			string+='.';
			while(next()&&ch>='0'&&ch<='9'){
				string+=ch}
			}
			if(ch==='e'||ch==='E'){
				string+=ch;
				next();
				if(ch==='-'||ch==='+'){
					string+=ch;next()
				}
				while(ch>='0'&&ch<='9'){
					string+=ch;next()
				}
			}
			number=+string;
			if(isNaN(number)){
				error("Bad number")
			}else{
				return number
			}
	},
	string=function(){
		var hex,i,string='',uffff;
		if(ch==='"'){
			while(next()){
				if(ch==='"'){
					next();
					return string
				}else if(ch==='\\'){
					next();
					if(ch==='u'){
						uffff=0;
						for(i=0;i<4;i+=1){
							hex=parseInt(next(),16);
							if(!isFinite(hex)){
								break
							}
							uffff=uffff*16+hex
						}
						string+=String.fromCharCode(uffff)
					}else if(typeof escapee[ch]==='string'){
						string+=escapee[ch]
					}else{
						break
					}
				}else{
					string+=ch}
				}
		}
		error("Bad string")
	},
	white=function(){
		while(ch&&ch<=' '){
			next()
		}
	},
	word=function(){
		switch(ch){
			case't':
				next('t');
				next('r');
				next('u');
				next('e');
				return true;
			case'f':
				next('f');
				next('a');
				next('l');
				next('s');
				next('e');
				return false;
			case'n':
				next('n');
				next('u');
				next('l');
				next('l');
				return null
		}
		error("Unexpected '"+ch+"'")
	},
	value,array=function(){
		var array=[];
		if(ch==='['){
			next('[');
			white();
			if(ch===']'){
				next(']');
				return array
			}
			while(ch){
				array.push(value());
				white();
				if(ch===']'){
					next(']');
					return array
				}
				next(',');
				white()
			}
		}
		error("Bad array")
	},
	object=function(){
		var key,object={};
		if(ch==='{'){
			next('{');
			white();
			if(ch==='}'){
				next('}');
				return object
			}
			while(ch){
				key=string();
				white();
				next(':');
				if(Object.hasOwnProperty.call(object,key)){
					error('Duplicate key "'+key+'"')
				}
				object[key]=value();
				white();
				if(ch==='}'){
					next('}');
					return object
				}
				next(',');
				white()
			}
		}
		error("Bad object")
	};
	value=function(){
		white();
		switch(ch){
			case'{':
				return object();
			case'[':
				return array();
			case'"':
				return string();
			case'-':
				return number();
			default:
				return ch>='0'&&ch<='9'?number():word()
		}
	};
	return function(source,reviver){
		var result;
		text=source;
		at=0;
		ch=' ';
		result=value();
		white();
		if(ch){
			error("Syntax error")
		}
		return typeof reviver==='function'?(function walk(holder,key){
			var k,v,value=holder[key];
			if(value&&typeof value==='object'){
				for(k in value){
					if(Object.hasOwnProperty.call(value,k)){
						v=walk(value,k);
						if(v!==undefined){
							value[k]=v
						}else{
							delete value[k]
						}
					}
				}
			}
			return reviver.call(holder,key,value)
		}
		({'':result},'')):result
	}
}());

var utf8={
	encode:function(string){
		string=string.replace(/\r\n/g,"\n");
		var utftext="";
		for(var n=0;n<string.length;n++){
			var c=string.charCodeAt(n);
			if(c<128){
				utftext+=String.fromCharCode(c);
			}
			else if((c>127)&&(c<2048)){
				utftext+=String.fromCharCode((c>>6)|192);
				utftext+=String.fromCharCode((c&63)|128);
			}
			else{
				utftext+=String.fromCharCode((c>>12)|224);
				utftext+=String.fromCharCode(((c>>6)&63)|128);
				utftext+=String.fromCharCode((c&63)|128);
			}
		}
		return utftext;
},

	decode:function(utftext){
		var string="";
		var i=0;
		var c=c1=c2=0;
		while(i<utftext.length){
			c=utftext.charCodeAt(i);
			if(c<128){
				string+=String.fromCharCode(c);
				i++;
			}
			else if((c>191)&&(c<224)){
				c2=utftext.charCodeAt(i+1);
				string+=String.fromCharCode(((c&31)<<6)|(c2&63));
				i+=2;
			}
			else{
				c2=utftext.charCodeAt(i+1);
				c3=utftext.charCodeAt(i+2);
				string+=String.fromCharCode(((c&15)<<12)|((c2&63)<<6)|(c3&63));
				i+=3;
			}
		}
		return string;
	}
}

function toJson(obj) {
	switch (typeof obj) {
	case 'object':
	   if (obj) {
	   	   var list = [];
	   	   if (obj instanceof Array) {
	   	   	   for (var i=0;i < obj.length;i++) {
	   	   	   	   list.push(toJson(obj[i]));
	   	   	   }
	   	   	   return '[' + list.join(',') + ']';
	   	   } else {
	   	   	   for (var prop in obj) {
	   	   	   	   list.push('"' + prop + '":' + toJson(obj[prop]));
	   	   	   }
	   	   	   return '{' + list.join(',') + '}';
	   	   }
	   } else {
	   	   return 'null';
	   }
   	case 'string': {
   		
   		obj = escape(obj);
   		obj = obj.replace(/(["'])/g, '\\$1');
   		obj = obj.replace(/%u..../g, '??')
   		return '"' + obj + '"';
   	}
   	case 'number':
   	case 'boolean':
   		return new String(obj);
 }
}

function json_parse_service(input){
	var _text="";
	var _type="";
	var _mention="";
	var output=new Array();
	var screen="";
	var analysis=json_parse(input,function(key,value){
		if(key=="text"){
			_text=value;
		}
		if(key=="type"){
			_type=value;
		}
		if(key=="mention"){
			_mention=value;
			var item=new Object();
			item.type=_type;
			item.mention=_mention;
			item.text=_text;
			output.push(item);
		}
	});
	return output;
}

