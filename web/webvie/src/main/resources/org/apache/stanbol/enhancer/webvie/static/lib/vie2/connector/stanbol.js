// File:   stanbol.js
// Author: <a href="mailto:sebastian.germesin@dfki.de">Sebastian Germesin</a>
//

// Ontology structure:
//type == http://fise.iks-project.eu/ontology/TextAnnotation
// => fise:start
// => fise:end
// => fise:selected-text
// => fise:selection-context
//type == http://fise.iks-project.eu/ontology/EntityAnnotation
// => fise:entity-reference
// => entity-label
// => fise:entity-type
//type == http://fise.iks-project.eu/ontology/Enhancement    
// => fise:confidence <float>
// => dc:type


// The stanbol connector needs to be initialized like this:
//$.VIE2.getConnector('stanbol').options({
//    "proxy_url" : "../utils/proxy/proxy.php",
//    "enhancer_url" : "http://stanbol.iksfordrupal.net:9000/engines/",
//    "entityhub_url" : "http://stanbol.iksfordrupal.net:9000/entityhub/"
//});

new VIE2.Connector('stanbol', {
    namespaces: {
        semdesk : "http://www.semanticdesktop.org/ontologies/2007/03/22/nfo#",
        owl : "http://www.w3.org/2002/07/owl#",
        gml : "http://www.opengis.net/gml/_",
        geonames : "http://www.geonames.org/ontology#",
        fise : "http://fise.iks-project.eu/ontology/",
        rick: "http://www.iks-project.eu/ontology/rick/model/"
    }
});

VIE2.connectors['stanbol'].analyze = function (object, options) {
    var rdf = jQuery.rdf();
    
    //rules to add backwards-relations to the triples
    //this makes querying for entities a lot easier!
    var rules = jQuery.rdf.ruleset()
    .prefix('fise', 'http://fise.iks-project.eu/ontology/')
    .prefix('dc', 'http://purl.org/dc/terms/')
    .add(['?subject a <http://fise.iks-project.eu/ontology/EntityAnnotation>',
          '?subject fise:entity-type ?type',
          '?subject fise:confidence ?confidence',
          '?subject fise:entity-reference ?entity',
          '?subject dc:relation ?relation',
          '?relation a <http://fise.iks-project.eu/ontology/TextAnnotation>',
          '?relation fise:selected-text ?selected-text',
          '?relation fise:selection-context ?selection-context',
          '?relation fise:start ?start',
          '?relation fise:end ?end'],
          ['?entity a ?type',
           '?entity fise:hasTextAnnotation ?relation',
           '?entity fise:hasEntityAnnotation ?subject']);
    
    if (object === undefined) {
        VIE2.log ("warn", "VIE2.Connector('" + this.id + "')", "Given object is undefined!");
        if (options && options.error) {
            options.error("Given object is undefined!");
        }
    } else if (typeof object === 'object') {
        var self = this; 
        //stanbol cannot deal with embedded HTML, so we remove that.
        //--> hack!
        var text = self.extractText(object);
        //the AJAX callback function
        var callback = function (rdfc) {
            //adding all new found triples to the main rdfQuery object
            rdfc.databank.triples().each(function () {
                rdf.add(this);
            });
            //let's see if there are children to be enhanced.
            VIE2.log("info", "VIE2.Connector(" + self.id + ")", "Start reasoning '" + (rdf.databank.triples().length) + "'");
            //rdf.reason(rules);    
            VIE2.log("info", "VIE2.Connector(" + self.id + ")", "End   reasoning '" + (rdf.databank.triples().length) + "'");
            if (options && options.success) {
                options.success.call(self, rdf);
            } else {
                VIE2.log("warn", "VIE2.Connector(" + self.id + ")", "No success callback given. How do you think this should gonna work?'");
            }
        };
        this.enhance(text, callback);
    } else {
        VIE2.log("error", "VIE2.Connector(" + this.id + ")", "Expected element of type 'object', found: '" + (typeof object) + "'");
        if (options && options.error) {
            options.error.call(this, "Expected element of type 'object', found: '" + (typeof object) + "'");
        }
    }
};

VIE2.connectors['stanbol'].extractText = function (obj) {
    if (obj.get(0) && 
            obj.get(0).tagName && 
            (obj.get(0).tagName == 'TEXTAREA' ||
            obj.get(0).tagName == 'INPUT' && obj.attr('type', 'text'))) {
        return obj.get(0).val();
    }
    else {
        return obj
            .text()    //get the text of element
            .replace(/\s+/g, ' ') //collapse multiple whitespaces
            .replace(/\0\b\n\r\f\t/g, '').trim(); // remove non-letter symbols
    }
};

VIE2.connectors['stanbol'].enhance = function (text, callback) {
    if (text.length === 0) {
        VIE2.log("warn", "VIE2.Connector(" + this.id + ")", "Empty text.");
        callback(jQuery.rdf());
    }
    else {
        var that = this;
        var c = function(data) {
            if (data) {
                if(typeof data == "string")
                    data = JSON.parse(data);
                var rdf;
                try {
                    rdf = jQuery.rdf().load(data, {});
                } 
                catch (e) {
                    VIE2.log("error", "VIE2.Connector(" + that.id + ")", e);
                    VIE2.log("error", "VIE2.Connector(" + that.id + ")", data);
                    rdf = jQuery.rdf();
                }
                setTimeout(function(){
                    callback(rdf)
                }, 1);
            }
        };
        this.queryEnhancer(text, c);
    }
};

VIE2.connectors['stanbol'].queryEnhancer = function (text, callback) {

    var proxy = this._options.proxy_url;
    var enhancer_url = this._options.enhancer_url;
    
    if (!this._options.enhancer_url) {
        VIE2.log("warn", "VIE2.connectors(" + this.id + ")", "No URL found for enhancer hub!");
        throw "VIE2.connector.stanbol.enhancer_url is empty";
        return;
    }

    if (proxy) {
        jQuery.ajax({
            async: true,
            success: callback,
            error: callback,
            type: "POST",
            url: proxy,
            data: {
                proxy_url: enhancer_url, 
                content: text,
                verb: "POST",
                format: "application/rdf+json"
            }
        });
    } else {
        jQuery.ajax({
            async: true,
            success: callback,
            error: callback,
            type: "POST",
            url: enhancer_url,
            data: text,
            dataType: "application/rdf+json",
            contentType: "text/plain",
            accepts: {"application/rdf+json": "application/rdf+json"}
        });
    }
};


//////////////////////

VIE2.connectors['stanbol'].query = function (uri, props, callback) {
    if (uri instanceof jQuery.rdf.resource &&
            uri.type === 'uri') {
        this.query(uri.toString().replace(/^</, '').replace(/>$/, ''), props, callback);
        return;
    }
    if (!jQuery.isArray(props)) {
        this.query(uri, [props], callback);
        return;
    }
    if ((typeof uri !== 'string') || uri.match(/^<urn:.*/) || uri.match(/^_:.*/)) {
        VIE2.log ("warn", "VIE2.Connector(" + this.id + ")", "Query does not support the given URI '" + uri + "'!");
        callback.call(this, {});
        return;
    }
    var uri = uri.replace(/^</, '').replace(/>$/, '');
    //initialize the returning object
    var ret = {};
    var that = this;
    
    var c = function (data) {
        if (data && data.status === 200) {
            try {
                var json = jQuery.parseJSON(data.responseText);
                var rdfc = jQuery.rdf().load(json);

                jQuery.each(VIE2.namespaces, function(k, v) {
                    rdfc.prefix(k, v);
                });
                
                for (var i=0; i < props.length; i++) {
                    var prop = props[i].toString();
                    ret[prop] = [];
                    
                    rdfc
                    .where(jQuery.rdf.pattern('<' + uri + '>', prop, '?object', { namespaces: VIE2.namespaces}))
                    .each(function () {
                        ret[prop].push(this.object);
                    });
                }
            } catch (e) {
                VIE2.log ("warn", "VIE2.Connector(" + that.id + ")", "Could not query for uri '" + uri + "' because of the following parsing error: '" + e.message + "'!");
            }
            callback.call(that, ret);
        } else {
            //we need to send back something in order to clear the queue.
            callback.call(that, {});
        }
    };
    
    this.queryEntityHub(uri, c);
};

VIE2.connectors['stanbol'].queryEntityHub = function (uri, callback) {
    var proxy = this._options.proxy_url;
    
    if (!this._options.entityhub_url) {
        VIE2.log("warn", "VIE2.connectors(" + this.id + ")", "No URL found for entity hub!");
        throw "VIE2.connector.stanbol.entityhub_url is empty";
        return;
    }
    
    var entityhub_url = this._options.entityhub_url.replace(/\/$/, '');
    
    if (proxy) {
        jQuery.ajax({
            async: true,
            type: "POST",
            success: callback,
            error: callback,
            url: proxy,
            dataType: "application/rdf+json",
            data: {
                proxy_url: entityhub_url + "/sites/entity?id=" + escape(uri), 
                content: '',
                verb: "GET",
                format: "application/rdf+json"
            }
        });
    } else {
        jQuery.ajax({
            async: true,
            success: callback,
            error: callback,
            type: "GET",
            url: entityhub_url + "/sites/entity?id=" + escape(uri),
            data: '',
            dataType: "application/rdf+json"
        });
    }
};

VIE2.connectors['stanbol'].findEntity = function (term, callback, limit, offset) {
    // curl -X POST -d "name=Bishofsh&limit=10&offset=0" http://localhost:8080/entityhub/sites/find
    var proxy = this._options.proxy_url;
    
    if (offset == null) {
        offset = 0;
    }
    
    if (limit == null) {
        limit = 10;
    }
    
    if (!this._options.entityhub_url) {
        VIE2.log("warn", "VIE2.connectors(" + this.id + ")", "No URL found for entity hub!");
        throw "VIE2.connector.stanbol.entityhub_url is empty";
        return;
    }
    
    var entityhub_url = this._options.entityhub_url.replace(/\/$/, '');
    
    function findResultTransform(findResponse){
        console.info(findResponse);
        return findResponse.results;
    }
    
    if (proxy) {
        // TODO test with proxy
        jQuery.ajax({
            async: true,
            type: "POST",
            success: callback,
            error: callback,
            url: proxy,
            dataType: "application/rdf+json",
            data: {
                proxy_url: entityhub_url + "/sites/find", 
                content: "name=" + term + "&limit=" + offset + "&limit=" + offset,
                verb: "POST",
                format: "application/rdf+json"
            }
        });
    } else {
        jQuery.ajax({
            async: true,
            success: function(response){
                callback(findResultTransform(response))
            },
            error: callback,
            type: "POST",
            url: entityhub_url + "/sites/find",
            data: "name=" + term + "&limit=" + offset + "&limit=" + offset,
            dataType: "application/rdf+json"
        });
    }
    
};
jQuery.ajaxSetup({
    converters: {"text application/rdf+json": function(s){return JSON.parse(s);}}
});

