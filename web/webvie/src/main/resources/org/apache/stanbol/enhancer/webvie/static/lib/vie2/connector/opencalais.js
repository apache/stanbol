new VIE2.Connector('opencalais', {
    namespaces: {
        c:  "http://s.opencalais.com/1/pred/",
        cr: "http://s.opencalais.com/1/type/er/",
        cm: "http://s.opencalais.com/1/type/em/e/",
        cl: "http://s.opencalais.com/1/type/lid/",
        cs: "http://s.opencalais.com/1/type/sys/",
        cc: "http://s.opencalais.com/1/type/cat/",
        foaf : "http://xmlns.com/foaf/0.1/"
    }
});

VIE2.connectors['opencalais'].analyze = function (object, options) {
    var rdf = jQuery.rdf();
    
    var rules = this.createReasoningRules();
    
    if (object === undefined) {
        VIE2.log ("warn", "VIE2.Connector('" + this.id + "')", "Given object is undefined!");
        if (options && options.error) {
            options.error("Given object is undefined!");
        }
    } else if (typeof object === 'object') {
        var self = this; 
        //opencalais can in fact deal with embedded HTML
        var text = self.extractText(object);
        //the AJAX callback function
        var callback = function (rdfc) {
            //adding all new found triples to the main rdfQuery object
            rdfc.databank.triples().each(function () {
                rdf.add(this);
            });
            //let's see if there are children to be enhanced.
            VIE2.log("info", "VIE2.Connector(" + self.id + ")", "Start reasoning '" + (rdf.databank.triples().length) + "'");
            rdf.reason(rules);    
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

VIE2.connectors['opencalais'].createReasoningRules = function () {
    var rules = jQuery.rdf.ruleset();
    
    jQuery.each(this._options.namespaces, function (k, v) {
        rules.prefix(k, v);
    })
    
    rules.add(['?subject a cm:Person',
               '?subject c:name ?name',
               '?subject c:commonname ?commonname'], 
              ['?subject foaf:name ?name',
               '?subject foaf:name ?commonname'
              ]);
              
   return rules;
}

VIE2.connectors['opencalais'].extractText = function (obj) {
    if (obj.get(0) && 
            obj.get(0).tagName && 
            (obj.get(0).tagName == 'TEXTAREA' ||
            obj.get(0).tagName == 'INPUT' && obj.attr('type', 'text'))) {
        return obj.get(0).val();
    }
    else {
        return obj
            .html()    //get the html of element
            .replace(/\s+/g, ' ') //collapse multiple whitespaces
            .replace(/\0\b\n\r\f\t/g, '').trim(); // remove non-letter symbols
    }
};

VIE2.connectors['opencalais'].enhance = function (text, callback) {
    if (text.length === 0) {
        VIE2.log("warn", "VIE2.Connector(" + this.id + ")", "Empty text.");
        callback(jQuery.rdf());
    }
    else {
        var c = function(data) {
            if (data) {
                try {
                    
                    var rdf = jQuery.rdf().load(data, {});
                    callback(rdf);
                } 
                catch (e) {
                    VIE2.log("error", "VIE2.Connector(" + this.id + ")", "Could not connect to opencalais enhancer.");
                    VIE2.log("error", "VIE2.Connector(" + this.id + ")", data);
                    callback(jQuery.rdf());
                }
            }
        };
        this.queryOpencalais(this.prepareOpencalaisData(text), c);
    }
};

VIE2.connectors['opencalais'].prepareOpencalaisData = function (text) {
    return {
        licenseID: this._options.opencalais_api_key,
        calculareRelevanceScore: "true",
        enableMetadataType: "GenericRelations,SocialTags",
        contentType: "text/html",
        content: text
        // for more options check http://developer.opencalais.com/docs/suggest/
    };
};

VIE2.connectors['opencalais'].queryOpencalais = function (data, callback) {

    var proxy = this._options.proxy_url;
    var opencalais_url = this._options.opencalais_url;

    if (proxy) {
        jQuery.ajax({
            async: true,
            success: callback,
            error: callback,
            type: "POST",
            url: proxy,
            data: {
                proxy_url: opencalais_url, 
                content: data,
                verb: "POST",
                format: "text/xml"//application/x-www-form-urlencoded"
            }
        });
    } else {
        jQuery.ajax({
            async: true,
            success: callback,
            error: callback,
            type: "POST",
            url: opencalais_url,
            data: data,
            dataType: "text/xml"
        });
    }
};