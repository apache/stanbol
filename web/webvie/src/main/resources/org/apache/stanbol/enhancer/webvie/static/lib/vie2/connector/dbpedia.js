// File:   dbpedia.js
// Author: <a href="mailto:sebastian.germesin@dfki.de">Sebastian Germesin</a>
//

//The dbpedia connector needs to be initialized like this:
//VIE2.getConnector('dbpedia').options({
//    "proxy_url" : "../utils/proxy/proxy.php"
//});
new VIE2.Connector('dbpedia', {
    namespaces: {
        'owl'    : "http://www.w3.org/2002/07/owl#",
        'yago'   : "http://dbpedia.org/class/yago/",
        'dbonto' : 'http://dbpedia.org/ontology/'
    }
});

VIE2.connectors['dbpedia'].query = function (uri, props, callback) {    
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
    if (!uri.match(/^http\:\/\/dbpedia.org\/.*/)) {
        VIE2.log ("warn", "VIE2.Connector('" + this.id + "')", "Query does not support the given URI!");
        callback.call(this, {});
        return;
    }
    
    var url = uri.replace('resource', 'data') + ".jrdf";
    var c = function (conn, u, ps) {
        return function (data) {
            //initialize the returning object
            var ret = {};
            
            if (data && data.status === 200) {
                try {
                    var json = jQuery.parseJSON(data.responseText);
                    if (json) {
                        var rdfc = jQuery.rdf().load(json);
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
                    VIE2.log ("warn", "VIE2.Connector('dbpedia')", "Could not query for uri '" + uri + "' because of the following parsing error: '" + e.message + "'!");
                }
            }
            callback.call(conn, ret);
        };
    }(this, uri, props);
    
    this.queryDBPedia(url, c);
};

VIE2.connectors['dbpedia'].queryDBPedia = function (url, callback) {
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