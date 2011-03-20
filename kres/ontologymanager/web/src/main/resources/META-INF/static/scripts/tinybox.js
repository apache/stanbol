var TINY={};

function T$(i){return document.getElementById(i)}

TINY.box=function(){
	var p,m,b,fn,ic,iu,iw,ih,ia,f=0;
	return{
		show:function(c,u,w,h,a,t){
			if(!f){
				p=document.createElement('div'); p.id='tinybox';
				m=document.createElement('div'); m.id='tinymask';
				b=document.createElement('div'); b.id='tinycontent';
				document.body.appendChild(m); document.body.appendChild(p); p.appendChild(b);
				m.onclick=TINY.box.hide; window.onresize=TINY.box.resize; f=1
				var useragent = navigator.userAgent;
				
				if(useragent.indexOf('MSIE')> -1){
					p.style.marginLeft = "50%";
					p.style.marginTop = "15%";
					p.style.position = "absolute";
				}
				else{
					p.style.position = "absolute";
				}
			}
			if(!a&&!u){
				p.style.width=w?w+'px':'auto'; p.style.height=h?h+'px':'auto';
				p.style.backgroundImage='none'; b.innerHTML=c
			}else{
				b.style.display='none'; p.style.width=p.style.height='100px'
			}
			this.mask();
			ic=c; iu=u; iw=w; ih=h; ia=a; this.alpha(m,1,80,3);
			if(t){setTimeout(function(){TINY.box.hide()},1000*t)}
		},
		showS:function(c,u,w,h,a,t){
			if(!f){
				p=document.createElement('div'); p.id='tinybox';
				m=document.createElement('div'); m.id='tinymask';
				document.body.appendChild(m); document.body.appendChild(p);
				m.onclick=TINY.box.hide; window.onresize=TINY.box.resize; f=1
				var useragent = navigator.userAgent;
				
				if(useragent.indexOf('MSIE')> -1){
					p.style.marginLeft = "50%";
					p.style.marginTop = "15%";
					p.style.position = "absolute";
				}
				else{
					p.style.position = "absolute";
				}
			}
			p.style.width=p.style.height='100px';
			this.mask();
			ic=c; iu=u; iw=w; ih=h; ia=a; this.alpha(m,1,80,3);
			if(t){setTimeout(function(){TINY.box.hide()},1000*t)}
		},
		showL:function(c,u,w,h,a,t){
			
			b=document.createElement('div'); b.id='tinycontent';
			p.appendChild(b);
			
			p.style.width=w?w+'px':'auto'; p.style.height=h?h+'px':'auto';
			p.style.backgroundImage='none'; b.innerHTML=c
			
		},
		fill:function(c,u,w,h,a){
			if(u){
				p.style.backgroundImage='';
				var x=window.XMLHttpRequest?new XMLHttpRequest():new ActiveXObject('Microsoft.XMLHTTP');
				x.onreadystatechange=function(){
					if(x.readyState==4&&x.status==200){TINY.box.psh(x.responseText,w,h,a)}
				};
				x.open('GET',c,1); x.send(null)
			}else{
				this.psh(c,w,h,a)
			}
		},
		psh:function(c,w,h,a){
			if(a){
				if(!w||!h){
					var x=p.style.width, y=p.style.height; b.innerHTML=c;
					p.style.width=w?w+'px':''; p.style.height=h?h+'px':'';
					b.style.display='';
					w=parseInt(b.offsetWidth); h=parseInt(b.offsetHeight);
					b.style.display='none'; p.style.width=x; p.style.height=y;
				}else{
					b.innerHTML=c
				}
				this.size(p,w,h,4)
			}else{
				p.style.backgroundImage='none'
			}
		},
		hide:function(){
			TINY.box.alpha(p,-1,0,3)
		},
		resize:function(){
			TINY.box.pos(); TINY.box.mask()
		},
		mask:function(){
			m.style.height=TINY.page.theight()+'px';
			m.style.width=''; m.style.width=TINY.page.twidth()+'px'
		},
		pos:function(){
			var t=(TINY.page.height()/2)-(p.offsetHeight/2); t=t<10?10:t;
			p.style.top=(t+TINY.page.top())+'px';
			p.style.left=(TINY.page.width()/2)-(p.offsetWidth/2)+'px'
		},
		alpha:function(e,d,a,s){
			clearInterval(e.ai);
			if(d==1){
				e.style.opacity=0; e.style.filter='alpha(opacity=0)';
				e.style.display='block'; this.pos()
			}
			e.ai=setInterval(function(){TINY.box.twalpha(e,a,d,s)},20)
		},
		twalpha:function(e,a,d,s){
			var o=Math.round(e.style.opacity*100);
			if(o==a){
				clearInterval(e.ai);
				if(d==-1){
					e.style.display='none';
					e==p?TINY.box.alpha(m,-1,0,2):b.innerHTML=p.style.backgroundImage=''
				}else{
					e==m?this.alpha(p,1,100,5):TINY.box.fill(ic,iu,iw,ih,ia)
				}
			}else{
				var n=o+Math.ceil(Math.abs(a-o)/s)*d;
				e.style.opacity=n/100; e.style.filter='alpha(opacity='+n+')'
			}
		},
		size:function(e,w,h,s){
			e=typeof e=='object'?e:T$(e); clearInterval(e.si);
			var ow=e.offsetWidth, oh=e.offsetHeight,
			wo=ow-parseInt(e.style.width), ho=oh-parseInt(e.style.height);
			var wd=ow-wo>w?-1:1, hd=(oh-ho>h)?-1:1;
			e.si=setInterval(function(){TINY.box.twsize(e,w,wo,wd,h,ho,hd,s)},20)
		},
		twsize:function(e,w,wo,wd,h,ho,hd,s){
			var ow=e.offsetWidth-wo, oh=e.offsetHeight-ho;
			if(ow==w&&oh==h){
				clearInterval(e.si); p.style.backgroundImage='none'; b.style.display='block'
			}else{
				if(ow!=w){e.style.width=ow+(Math.ceil(Math.abs(w-ow)/s)*wd)+'px'}
				if(oh!=h){e.style.height=oh+(Math.ceil(Math.abs(h-oh)/s)*hd)+'px'}
				this.pos()
			}
		},
		work:function(){
			var tinybox = document.getElementById("tinybox");
			if(tinybox != null){
				tinybox.style.background = "#fff url(img/loading.gif) no-repeat 50% 50%";
				tinybox.style.display = "block";
				var tinycontent = document.getElementById("tinycontent");
				if(tinycontent != null){
					tinycontent.innerHTML = "<br>";
					//tinycontent.style.background = "#fff url(img/loading.gif) no-repeat 40% 40%";
					//tinycontent.style.display = 'none';
				}
				
			}
		},
		searchShow:function(c,u,w,h,a,t){
			if(!f){
				p=document.createElement('div'); p.id='tinybox';
				m=document.createElement('div'); m.id='tinymask';
				document.body.appendChild(m); document.body.appendChild(p);
				m.onclick=TINY.box.hide; window.onresize=TINY.box.resize; f=1
				var useragent = navigator.userAgent;
				
				if(useragent.indexOf('MSIE')> -1){
					p.style.marginLeft = "50%";
					p.style.marginTop = "15%";
					p.style.position = "absolute";
				}
				else{
					p.style.position = "absolute";
				}
			}
			
			this.mask();
			ic=c; iu=u; iw=w; ih=h; ia=a; this.alpha(m,1,80,3);
			if(t){setTimeout(function(){TINY.box.hide()},1000*t)}
		}
	}
}();

TINY.page=function(){
	return{
		top:function(){return document.body.scrollTop||document.documentElement.scrollTop},
		width:function(){return self.innerWidth||document.documentElement.clientWidth},
		height:function(){return self.innerHeight||document.documentElement.clientHeight},
		theight:function(){
			var d=document, b=d.body, e=d.documentElement;
			return Math.max(Math.max(b.scrollHeight,e.scrollHeight),Math.max(b.clientHeight,e.clientHeight))
		},
		twidth:function(){
			var d=document, b=d.body, e=d.documentElement;
			return Math.max(Math.max(b.scrollWidth,e.scrollWidth),Math.max(b.clientWidth,e.clientWidth))
		}
	}
}();


function popupCentrata(req, parameter) {
	
	var ebhclibrary = readCookie("ebhclibrary");
	var auth;
	var remember = false;
	if(ebhclibrary != null){
		auth = ebhclibrary.split(";");
		auth = auth[0].split(" ");
		remember = true;
	}
	
	var content2;
	
	if(remember){
		content2 = "<div id=\"popupbox\">" +
					"<form name=\"login\" action=\"\" method=\"post\">" +
					"<center>Username:</center>" +
					"<center><input id=\"username\" name=\"username\" value=\""+auth[0]+"\" size=\"14\" /></center>" +
					"<center>Password:</center>" +
					"<center><input id=\"password\" name=\"password\" type=\"password\" value=\""+auth[1]+"\" size=\"14\" /></center>";
	}
	else{
		content2 = "<div id=\"popupbox\">" +
					"<form name=\"login\" action=\"\" method=\"post\">" +
					"<center>Username:</center>" +
					"<center><input id=\"username\" name=\"username\" size=\"14\" /></center>" +
					"<center>Password:</center>" +
					"<center><input id=\"password\" name=\"password\" type=\"password\" size=\"14\" /></center>";
	}
					
	
	if(req != null){
		req = "'"+req+"'";
	}
	if(parameter != null){
		parameter = "'"+parameter+"'";
	}
	
	if(remember){
		content2 += "<center><input id=\"rememberme\" type=\"checkbox\" name=\"rememberme\" value=\"rememberme\" checked/>Remember me</center></form>";
	}
	else{
		content2 += "<center><input id=\"rememberme\" type=\"checkbox\" name=\"rememberme\" value=\"rememberme\"/>Remember me</center></form>";
	}
	
	content2 += "<center><input type=\"button\" name=\"submit\" value=\"login\" onclick=\"javascript:loggin("+req+", "+parameter+")\"/></center></form>" +
				"<center><span id=\"loginincorrect\" style=\"color:red\"></span></center>";
	
	//var content2 = "<img src='img/full-text.gif' width='298' height='373' alt='' />";
	
	//TINY.box.show(content2,0,0,0,1);
	TINY.box.showL(content2,0,0,0,1);

 }

function poplog(req, parameter) {
	
	var ebhclibrary = readCookie("ebhclibrary");
	var auth;
	var remember = false;
	if(ebhclibrary != null){
		auth = ebhclibrary.split(";");
		auth = auth[0].split(" ");
		remember = true;
	}
	
	var content2;
	
	if(remember){
		content2 = "<div id=\"popupbox\">" +
					"<form name=\"login\" action=\"\" method=\"post\">" +
					"<center>Username:</center>" +
					"<center><input id=\"username\" name=\"username\" value=\""+auth[0]+"\" size=\"14\" /></center>" +
					"<center>Password:</center>" +
					"<center><input id=\"password\" name=\"password\" type=\"password\" value=\""+auth[1]+"\" size=\"14\" /></center>";
	}
	else{
		content2 = "<div id=\"popupbox\">" +
					"<form name=\"login\" action=\"\" method=\"post\">" +
					"<center>Username:</center>" +
					"<center><input id=\"username\" name=\"username\" size=\"14\" /></center>" +
					"<center>Password:</center>" +
					"<center><input id=\"password\" name=\"password\" type=\"password\" size=\"14\" /></center>";
	}
					
	
	if(req != null){
		req = "'"+req+"'";
	}
	if(parameter != null){
		parameter = "'"+parameter+"'";
	}
	
	if(remember){
		content2 += "<center><input id=\"rememberme\" type=\"checkbox\" name=\"rememberme\" value=\"rememberme\" checked/>Remember me</center></form>";
	}
	else{
		content2 += "<center><input id=\"rememberme\" type=\"checkbox\" name=\"rememberme\" value=\"rememberme\"/>Remember me</center></form>";
	}
	
	content2 += "<center><input type=\"button\" name=\"submit\" value=\"login\" onclick=\"javascript:loggin("+req+", "+parameter+")\"/></center></form>" +
				"<center><span id=\"loginincorrect\" style=\"color:red\"></span></center>";
	
	//var content2 = "<img src='img/full-text.gif' width='298' height='373' alt='' />";
	
	TINY.box.show(content2,0,0,0,1);

 }


function readCookie(name) {
	var nameEQ = name + "=";
	var ca = document.cookie.split(';');
	for(var i=0;i < ca.length;i++) {
		var c = ca[i];
		while (c.charAt(0)==' ') c = c.substring(1,c.length);
		if (c.indexOf(nameEQ) == 0) return c.substring(nameEQ.length,c.length);
	}
	return null;
}

function createCookie(name,value,days) {
	if (days) {
		var date = new Date();
		date.setTime(date.getTime()+(days*24*60*60*1000));
		var expires = "; expires="+date.toGMTString();
	}
	else var expires = "";
	
	document.cookie = name+"="+value+expires+"; path=/";
}

function eraseCookie(name) {
	createCookie(name,"",-1);
}


