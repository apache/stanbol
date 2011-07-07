new VIE2.Connector('zemanta', {
    namespaces: {
        z: "http://s.zemanta.com/ns#"
    }
});

VIE2.connectors['zemanta'].analyze = function (object, options) {
    var rdf = jQuery.rdf();
    
    var rules = jQuery.rdf.ruleset()
    .prefix('z', 'http://s.zemanta.com/ns#')
    .add([], []);
    
    if (object === undefined) {
        VIE2.log ("warn", "VIE2.Connector('" + this.id + "')", "Given object is undefined!");
        if (options && options.error) {
            options.error("Given object is undefined!");
        }
    } else if (typeof object === 'object') {
        var self = this; 
        //zemanta cannot deal with embedded HTML, so we remove that.
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

VIE2.connectors['zemanta'].extractText = function (obj) {
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

VIE2.connectors['zemanta'].enhance = function (text, callback) {
    if (text.length === 0) {
        VIE2.log("warn", "VIE2.Connector(" + this.id + ")", "Empty text.");
        callback(jQuery.rdf());
    }
    else {
        var c = function(data) {
            if (data && data.status === 200) {
                try {
                    var responseData = data.responseText
                    .replace(/<z:signature>.*?<\/z:signature>/, '');
                    var rdf = jQuery.rdf().load(responseData, {});
                    callback(rdf);
                } 
                catch (e) {
                    VIE2.log("error", "VIE2.Connector(" + this.id + ")", "Could not connect to zemanta enhancer.");
                    VIE2.log("error", "VIE2.Connector(" + this.id + ")", data);
                    callback(jQuery.rdf());
                }
            }
        };
        this.queryZemanta(this.prepareZemantaData(text), c);
    }
};

VIE2.connectors['zemanta'].prepareZemantaData = function (text) {
    return {
        method: 'zemanta.suggest_markup',
        format: 'rdfxml',
        api_key: this._options.zemanta_api_key,
        text: text,
        return_rdf_links: 1
        // for more options check http://developer.zemanta.com/docs/suggest/
    };
};

VIE2.connectors['zemanta'].queryZemanta = function (data, callback) {

    var proxy = this._options.proxy_url;
    var zemanta_url = this._options.zemanta_url;

    if (proxy) {
        jQuery.ajax({
            async: true,
            success: callback,
            error: callback,
            type: "POST",
            url: proxy,
            data: {
                proxy_url: zemanta_url, 
                content: data,
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
            url: zemanta_url,
            data: data,
            dataType: "application/rdf+json"
        });
    }
};