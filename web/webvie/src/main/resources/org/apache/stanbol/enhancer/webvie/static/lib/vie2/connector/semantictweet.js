// File:   semantictweet.js
// Author: Rene Kapusta
//

//The semantictweet connector needs to be initialized like this:
//VIE2.getConnector('semantictweet').options({
//    "proxy_url" : "../utils/proxy/proxy.php"
//});
new VIE2.Connector('semantictweet');

VIE2.connectors['semantictweet'].query = function (uri, props, callback) {
    if (uri instanceof jQuery.rdf.resource &&
            uri.type === 'uri') {
        this.query(uri.toString(), props, callback);
        return;
    }
    if (!jQuery.isArray(props)) {
        return this.query(uri, [props], callback);
        return;
    }
    if ((typeof uri != 'string')) {
        VIE2.log ("warn", "VIE2.Connector('" + this.id + "')", "Query does not support the given URI!");
        callback.call(this, {});
        return;
    }
    var uri = uri.replace(/^</, '').replace(/>$/, '');
    
    if (!uri.match(/^http\:\/\/semantictweet.com\/.*/)) {
        VIE2.log ("warn", "VIE2.Connector('" + this.id + "')", "Query does not support the given URI!");
        callback.call(this, {});
        return;
    }
    
    //var url = uri.replace('resource', 'data') + ".jrdf";
    var url = uri;
    var c = function (conn, u, ps) {
        return function (data) {
            //initialize the returning object
            var ret = {};
            
            if (data && data.status === 200) {
                try {
                    //var json = jQuery.parseJSON(data.responseText);
                    var rdf_xml = data.responseText;
                    if (rdf_xml) {
                        var rdfc = jQuery.rdf().load(rdf_xml);
                        jQuery.each(VIE2.namespaces, function(k, v) {
                            rdfc.prefix(k, v);
                        });
                        
                        for (var i=0; i < ps.length; i++) {
                            var prop = props[i].toString();
                            ret[prop] = [];
                            
                            rdfc
                            .where(jQuery.rdf.pattern('<' + u + '>', prop, '?object', { namespaces: VIE2.namespaces}))
                            .each(function () {
                                ret[prop].push(this.object);
                            });
                        }
                    }
                } catch (e) {
                    VIE2.log ("warn", "VIE2.Connector('semantictweet')", "Could not query for uri '" + uri + "' because of the following parsing error: '" + e.message + "'!");
                }
            }
            callback.call(this, ret);
        };
    }(this, uri, props);
    
    this.querySemantictweet(url, c);
};

VIE2.connectors['semantictweet'].querySemantictweet = function (url, callback) {
    var proxy = this.options().proxy_url;
    
    if (proxy) {
        jQuery.ajax({
            async: true,
            complete : callback,
            type: "POST",
            url: proxy,
            data: {
                proxy_url: url, 
                content: "",
                verb: "GET"
            }
        });
    } else {
        data = jQuery.ajax({
            async: true,
            complete : callback,
            type: "GET",
            'url': url
        });
    }
};