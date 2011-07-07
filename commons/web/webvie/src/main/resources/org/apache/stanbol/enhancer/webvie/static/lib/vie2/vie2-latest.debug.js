// VIE&sup2; - Vienna IKS Editable Entities
// (c) 2011 Sebastian Germesin, IKS Consortium
// VIE&sup2; may be freely distributed under the MIT license.
// (see LICENSE.txt)
// For all details and documentation:
// http://wiki.iks-project.eu/index.php/VIE^2

(function() {
// Initial setup
// -------------
//
// The VIE&sup2; library is fully contained inside a VIE2 namespace.
var VIE2 = this.VIE2 = {};
    
//VIE&sup2; is the semantic enrichment layer on top of VIE.
//Its acronym stands for <b>V</b>ienna <b>I</b>KS <b>E</b>ditable <b>E</b>ntities.

//With the help of VIE&sup2;, you can bring entites in your
//content (aka. semantic lifting) and furthermore interact
//with this knowledge in a MVC manner - using Backbone JS models
//and collections. It is important to say that VIE&sup2; helps you to
//automatically annotate data but also let's you enable users
//to change/add/remove entities and their properties at the users
//wish.
//VIE&sup2; has two main principles: 

//*  Connectors:
//   Connecting VIE&sup2; with **backend** services, that
//   can either analyse and enrich the content sent to them (e.g., using
//   Apache Stanbol or Zemanta), can act as knowledge databases (e.g., DBPedia)
//   or as serializer (e.g., RDFa).
//*  Mappings:
//   In a mapping, a web developer can specify a mapping from ontological entities
//   to backbone JS models. The developer can easily add types of entities and
//   also default attributes that are automatically filled with the help of the 
//   available connectors.
// File:   core.js
// Author: <a href="mailto:sebastian.germesin@dfki.de">Sebastian Germesin</a>
//

(function($, undefined) {

//VIE&sup2; is implmented as a [jQuery UI widget](http://semantic-interaction.org/blog/2011/03/01/jquery-ui-widget-factory/). 
    $.widget('VIE2.vie2', {
        
        // default options
        options: {
            entities : []
        },
        
        //<strong>_create()</strong>: The private method **_create():** is called implicitly when
        //calling .vie2(); on any jQuery object.
        _create: function () {            
            //automatically scans for xmlns attributes in the **html** element
            //and adds them to the global VIE2.namespaces object
            jQuery.each(jQuery('html').xmlns(), function (k, v) {
                VIE2.namespaces[k] = v.toString();
                VIE2.globalCache.prefix(k, v);
            });
            
            //automatically scans for xmlns attributes in the **given** element
            //and adds them to the global VIE2.namespaces object
            try {
                jQuery.each(this.element.xmlns(), function(k, v){
                    VIE2.namespaces[k] = v.toString();
                    VIE2.globalCache.prefix(k, v);
                });
            } catch (ex) {
                //needs to be ignored when called on $(document);
                if (this.element.get(0) !== document) {
                    VIE2.log("warn", "VIE2.core#create()", "Could not retrieve namespaces from element: '" + e + "'!");
                }
            }
        },
        
        //<strong>analyze(callback,[options])</strong>: The analyze() method sends the element to all connectors and lets
        //them analyze the content. The connectors' methods are asynchronously called and once all connectors
        //returned the found enrichments in the form of **jQuery.rdf objects**, the *callback* method is
        //executed (in the scope of the callback function, *this* refers to the given element).<br />
        //The returned enrichments are written into the global Cache of VIE&sup2; (VIE2.globalCache).<br />
        //Furthermore, each found subject in the returned knowledge is checked whether there is a mapping to 
        //backbone JS available and if so, the entity is added to the corresponding backbone collection(s).
        //*options* can contain a 'connectors' field. If so, only these connectors will be used
        //for the analysis. If not specified, all connectors are used.
        analyze: function (callback, options) {
            if (!options) { options = {};}
            var that = this;
            
            //analyze() does not actually need a callback method, but it is usually good to use it 
            if (callback === undefined) {
                VIE2.log("warn", "VIE2.core#analyze()", "No callback method specified!");
            }
            
            VIE2.log("info", "VIE2.core#analyze()", "Started.");
                        
            //as the connectors work asynchronously, we need a queue to listen if all connectors are finished.
            var connectorQueue = [];
            jQuery.each(VIE2.connectors, function () {
                //fill queue of connectors with 'id's to have an overview of running connectors.
                //this supports the asynchronous calls.
                if (options.connectors) {
                    if (options.connectors.indexOf(this.id) !== -1) {
                        connectorQueue.push(this.id);
                    }
                } else {
                    connectorQueue.push(this.id);
                }
            });
            
            //iterate over all connectors
            jQuery.each(VIE2.connectors, function () {
                //the connector's success callback method
                var successCallback = function (elem) {
                    return function (rdf) {
                        VIE2.log("info", "VIE2.core#analyze()", "Received RDF annotation from connector '" + this.id + "'!");
                        
                        //we add all namespaces to the rdfQuery object. 
                        //Attention: this might override namespaces that were added by the connector!
                        //but needed to keep consistency through VIE&sup2;.
                        jQuery.each(VIE2.namespaces, function(k, v) {
                            rdf.prefix(k, v);
                        });

                        rdf.databank.triples().each(function () {
                            //add all triples to the global cache!
                            VIE2.globalCache.add(this);
                        });
                        
                        
                        // TODO Get only EntityEnhancement entities instead of subjectIndex.*
                        //register all subjects as backbone model
                        jQuery.each(rdf.databank.subjectIndex, function (subject, v) {
                            var subjStr = subject.toString();
                            if (that.options.entities.indexOf(subjStr) === -1) {
                                that.options.entities.push(subjStr);
                            }
                            
                            if (!VIE.EntityManager.getBySubject(subjStr)) {
                                VIE2.log("info", "VIE2.core#analyze()", "Register new entity (" + subjStr + ")!");
                                
                                VIE2.createEntity({
                                  id : subjStr
                                }, {backend: true});
                            } else {
                                VIE.EntityManager.getBySubject(subjStr).change();
                            }
                        });
                        VIE2.Util.removeElement(connectorQueue, this.id);
                        //everytime we receive annotations from each connector, we remove the connector's id from the
                        //queue and check whether the queue is empty.
                        if (connectorQueue.length === 0) {
                            //if the queue is empty, all connectors have successfully returned and we can execute the
                            //callback function.
                            VIE2.log("info", "VIE2.core#analyze()", "Finished! Global Cache holds now " + VIE2.globalCache.databank.triples().length + " triples!");
                            VIE2.log("info", "VIE2.core#analyze()", "Finished! Local element holds now "  + that.options.entities.length + " entities!");
                            //provide a status field in the callback object: status = {'ok', 'error'};
                            if (callback) {
                                callback.call(elem);
                            }
                        }
                    };
                } (that.element);
                
                //the connector's error callback method
                var errorCallback = function (reason) {
                    VIE2.log("error", "VIE2.core#analyze()", "Connector (" + this.id + ") returned with the following error: '" + reason + "'!");
                    VIE2.Util.removeElement(connectorQueue, this.id);
                };
                
                //check if we may need to filter for the connector
                if (options.connectors) {
                    if (options.connectors.indexOf(this.id) !== -1) {
                        //start analysis with the connector.
                         VIE2.log("info", "VIE2.core#analyze()", "Starting analysis with connector: '" + this.id + "'!");
                        this.analyze(that.element, {
                            success: successCallback,
                            error: errorCallback
                        });
                    }
                    else {
                        VIE2.log("info", "VIE2.core#analyze()", "Will not use connector " + this.id + " as it is filtered!");
                    }
                } else {
                    //start analysis with the connector.
                     VIE2.log("info", "VIE2.core#analyze()", "Starting analysis with connector: '" + this.id + "'!");
                    this.analyze(that.element, {
                        success: successCallback,
                        error: errorCallback
                    });
                }
            });
        },
                
        //<strong>uris()</strong>: Returns a list of all uris, that are within the scope of the current element!
        uris: function () {
            return this.options.entities;
        },
                
        //<strong>copy(tar)</strong>: Copies all local knowledge to the target element(s).
        //Basically calls: <pre>
        //$(tar).vie2().vie2('option', 'entities', this.options.entities);
        //</pre>
        copy: function (tar) {
            //copy all knowledge from src to target
            var that = this;
            if (!tar) {
                VIE2.log("warn", "VIE2.core#copy()", "Invoked 'copy()' without target element!");
                return;
            }
            VIE2.log("info", "VIE2.core#copy()", "Start.");
            VIE2.log("info", "VIE2.core#copy()", "Found " + this.options.entities.length + " entities for source.");
            
            jQuery(tar).vie2().vie2('option', 'entities', this.options.entities);
            VIE2.log("info", "VIE2.core#copy()", "Finished.");
            VIE2.log("info", "VIE2.core#copy()", "Target element has now " + jQuery(tar).vie2('option', 'entities') + " entities.");
            return this;
        },
        
        //<strong>clear()</strong>: Clears the local entities.
        clear: function () {
            this.options.entities = {};
            return this;
        }
        
    });
}(jQuery));

//The global <strong>VIE2 object</strong>. If VIE2 is already defined, the
//existing VIE2 object will not be overwritten so that the
//defined object is preserved.
if (typeof VIE2 == 'undefined' || !VIE2) {
    VIE2 = {};
}

//<strong>VIE2.namespaces</strong>: This map contains all namespaces known to VIE2.
//There are currently *no* default namespaces, though
//we might want to change this in a future release.
//Namespaces can be overridden directly using VIE2.namespaces[x] = y but
//are parsed from the &lt;html> tag's xmlns: attribute anyway during initialization.
VIE2.namespaces = {};

//<strong>VIE2.globalCache</strong>: The variable **globalCache** stores all knowledge in
//triples that were retrieved and annotated so far in one *rdfQuery object*. Though it is
//available via VIE2.globalCache, it is highly discouraged to access it directly.
VIE2.globalCache = jQuery.rdf({namespaces: VIE2.namespaces});

//<strong>VIE2.clearCache()</strong>: Static method to clear the global Cache.
VIE2.clearCache = function () {
    VIE2.globalCache = jQuery.rdf({namespaces: VIE2.namespaces});
};

//<strong>VIE2.getFromCache(uri, prop)</strong>: Retrive properties from the given
// *uri* directly from the global Cache. 
VIE2.getFromCache = function (parent, uri, prop) {
    //initialize collection
    var Collection = VIE2.ObjectCollection.extend({
        uri      : uri,
        property : prop,
        parent: parent
    });
    
    var ret = new Collection();
    
    VIE2.globalCache
    .where(jQuery.rdf.pattern(uri, prop, '?object', {namespaces: VIE2.namespaces}))
    .each(function () {
        if (this.object.type) {
            if (this.object.type === 'literal') {
                var inst = VIE2.createLiteral(this.object.value, {lang: this.object.lang, datatype: this.object.datatype, backend:true, silent:true});
                ret.add(inst, {backend:true, silent:true});
            } else if (this.object.type === 'uri' || this.object.type === 'bnode') {
                if (VIE.EntityManager.getBySubject(this.object.toString()) !== undefined) {
                    ret.add(VIE.EntityManager.getBySubject(this.object.toString()), {backend:true, silent:true});
                }
                else {
                    var inst = VIE2.createResource(this.object.value.toString(), {backend:true, silent:true});
                    ret.add(inst, {backend:true, silent:true});
                }
            }
        }
    });
    
    return ret;
};

VIE2.removeFromCache = function (uri, prop, val) {
    var pattern = jQuery.rdf.pattern(
    uri, 
    (prop)? prop : '?x',
    (val)? val : '?y', 
    {namespaces: VIE2.namespaces});
    VIE2.log("info", "VIE2.removeFromCache()", "Removing triples that match: '" + pattern.toString() + "'!");
    VIE2.log("info", "VIE2.removeFromCache()", "Global Cache now holds " + VIE2.globalCache.databank.triples().length + " triples!");
    VIE2.globalCache
    .where(pattern)
    .remove(pattern);
    VIE2.log("info", "VIE2.removeFromCache()", "Global Cache now holds " + VIE2.globalCache.databank.triples().length + " triples!");
};

//<strong>VIE2.lookup(uri, props, callback)</strong>: The query function supports querying for properties. The uri needs
//to be of type <code>jQuery.rdf</code> object or a simple string and the property is either an array of strings
//or a simple string. The function iterates over all connectors that have <code>query()</code>
//implemented and collects data in an object.
//The callback retrieves an object with the properties as keys and an array of results as their corresponding values.
VIE2.lookup = function (uri, props, callback) {
    VIE2.log("info", "VIE2.lookup()", "Start ('" + uri + "', '" + props + "')!");

    if (uri === undefined || typeof uri !== 'string' || props === undefined) {
        VIE2.log("warn", "VIE2.lookup()", "Invoked 'lookup()' with wrong/undefined argument(s)!");
        if (callback) {
            callback.call(uri, ret);
        }
        return;
    }
    
    if (!jQuery.isArray(props)) {
        VIE2.lookup(uri, [ props ], callback);
        return;
    }
    
    //initialize the returning object
    var ret = {};
    for (var i=0; i < props.length; i++) {
        ret[props[i]] = [];
    }
                
    var connectorQueue = [];
    jQuery.each(VIE2.connectors, function () {
        //fill queue of connectors with 'id's to have an overview of running connectors.
        //this supports the asynchronous calls.
        connectorQueue.push(this.id);
    });
    
    //look up for properties in the connectors that
    //implement/overwrite the query() method
    jQuery.each(VIE2.connectors, function () {
        VIE2.log("info", "VIE2.lookup()", "Start with connector '" + this.id + "' for uri '" + uri + "'!");
        var c = function (uri, ret, callback) {
            return function (data) {
                try {
                    VIE2.log("info", "VIE2.lookup()", ["Received query information from connector '" + this.id + "' for uri '" + uri + "'!", data]);
                    VIE2.globalCache.load(data);
                    VIE.EntityManager.getByRDFJSON(data);
                    VIE2.Util.removeElement(connectorQueue, this.id);
                    if (connectorQueue.length === 0) {
                        //if the queue is empty, all connectors have successfully returned and we can call the
                        //callback function.
                        jQuery.each(ret, function(k){
                            VIE2.globalCache.where(uri + ' ' + k + ' ?x').each(function(){
                                var valStr = this.x.toString();
                                if (ret[k].indexOf(valStr) === -1) {
                                    ret[k].push(valStr);
                                }
                            });
                        });
                        VIE2.log("info", "VIE2.lookup()", "Finished task: 'query()' for uri '" + uri + "'!");
                        VIE2.log("info", "VIE2.lookup()", "Global Cache now holds " + VIE2.globalCache.databank.triples().length + " triples!");
                        if (callback) {
                            callback.call(uri, ret);
                        }
                    }
                } catch (e) {
                    VIE2.log("error", "EEEEERRROR!");
                }
            };
        }(uri, ret, callback);
        this.query(uri, props, c);
    });
};

//<strong>VIE2.mappings</strong>: Contains for all registered mappings (mapping.id is the key), the
//following items:<br/>
//* VIE2.mappings[id].a -> an array of strings (curies) of the corresponding type.
//* VIE2.mappings[id].mapping -> the mapping itself
//* VIE2.mappings[id].collection -> the backbone JS collection, that has the Model registered. 
VIE2.mappings = {};

//<strong>VIE2.registerMapping(mapping)</strong>: Static method to register a mapping (is automatically called 
//during construction of mapping class. This allocates an object in *VIE2.mappings[mapping.id]*.
VIE2.registerMapping = function (mapping) {
    //first check if there is already 
    //a mapping with 'mapping.id' registered    
    if (!VIE2.mappings[mapping.id]) {
                
        var Collection = VIE2.EntityCollection.extend({model: VIE2.Entity});
        
        VIE2.mappings[mapping.id] = {
            "a" : (jQuery.isArray(mapping.types))? mapping.types : [mapping.types],
            "collection" : new Collection(),
            "mapping" : mapping
        };
        
        //trigger filling of collections!
        for (var i = 0; i < VIE2.entities.length; i++) {
            VIE2.entities.at(i).searchCollections();
        }
        
        
        VIE2.log("info", "VIE2.registerMapping()", "  Registered mapping '" + mapping.id + "'!");
    } else {
        VIE2.log("warn", "VIE2.registerMapping()", "Did not register mapping, as there is" +
                "already a mapping with the same id registered.");
    }
};

//<strong>VIE2.unregisterMapping(mappingId)</strong>: Unregistering of mappings. There is currently
//no usecase for that, but it wasn't that hard to implement ;)
VIE2.unregisterMapping = function (mappingId) {
    VIE2.mappings[mappingId] = undefined;
};

//<strong>VIE2.connectors</strong>: Static object of all registered connectors.
VIE2.connectors = {};

//<strong>VIE2.registerConnector(connector)</strong>: Static method to register a connector (is automatically called 
//during construction of connector class. If set, inserts connector-specific namespaces to the known Caches.
VIE2.registerConnector = function (connector) {
    //first check if there is already 
    //a connector with 'connector.id' registered
    if (!VIE2.connectors[connector.id]) {
        VIE2.connectors[connector.id] = connector;
        
        if (connector.options('namespaces')) {
            jQuery.each(connector.options('namespaces'), function(k, v) {
                VIE2.namespaces[k] = v;
                VIE2.globalCache.prefix(k, v);
                //also add to all known VIE&sup2; elements' Cache!
            });
        }
        VIE2.log("info", "VIE2.registerConnector()", "Registered connector '" + connector.id + "'");
        
    } else {
        VIE2.log("warn", "VIE2.registerConnector()", "Did not register connector, as there is" +
                "already a connector with the same id registered.");
    }
};

//<strong>VIE2.unregisterConnector(connectorId)</strong>: Unregistering of connectors. There is currently
//no usecase for that, but it wasn't that hard to implement ;)
VIE2.unregisterConnector = function (connectorId) {
    VIE2.connectors[connector.id] = undefined;
};

VIE2.logLevels = ["info", "warn", "error"];

//<strong>VIE2.log(level, component, message)</strong>: Static convenience method for logging.
VIE2.log = function (level, component, message) {
    if (VIE2.logLevels.indexOf(level) > -1) {
        switch (level) {
            case "info":
                console.info([component, message]);
                break;
            case "warn":
                console.warn([component, message]);
                break;
            case "error":
                console.error([component, message]);
                break;
        }
    }
};
// File:   collection.js
// Author: <a href="mailto:sebastian.germesin@dfki.de">Sebastian Germesin</a>
//

//just for convenience, should be removed in a later revision
VIE.EntityManager.initializeCollection();

//<strong>VIE2.EntityCollection</strong>: TODO: document me
VIE2.EntityCollection = VIE.RDFEntityCollection.extend({
    
    //overwrite the internal _add method
    _add: function (model, opts) {
        if (!opts) { opts = {};}
        VIE.RDFEntityCollection.prototype._add.call(this, model, opts);
        
        //if the annotation does *not* come from the analyze() method
        //it comes from the user and hence,
        //we need to add the subject to the internal triple store.
        if (!opts.backend) {
            var triple = jQuery.rdf.triple(
                model.get('id'), 
                'a', 
                'owl:Thing', 
                {namespaces: VIE2.namespaces}
            );
            VIE2.globalCache.add(triple);    
        }
        //in any case, we query all connectors for the types of the entity.
        VIE2.lookup(model.get('id'), ['a'], function (m) {
            return function () {
                m.trigger('change:a');
            };
        }(model));
    },
    
    _remove: function (model, opts) {
        if (!opts) { opts = {};}
        if (model) {
            //when removing the model from this collection, that means
            //that we remove all corresponding data from the cache as well.
            if (VIE2.entities === this) {
                //also remove from all other collections!
                jQuery.each(VIE2.mappings, function(k, v){
                    v.collection.remove(model);
                });
                
                VIE.EntityManager.entities.remove(model, opts);
                model.destroy();
            }
            VIE.RDFEntityCollection.prototype._remove.call(this, model, opts);
        }
    }
});

VIE2.entities = new VIE2.EntityCollection();

//<strong>VIE2.ObjectCollection</strong>: TODO: document me
VIE2.ObjectCollection = Backbone.Collection.extend({
        
    _add: function (model, opts) {
        //TODO: propagate event to parent model
        if (!opts) { opts = {};}
        
        //adding a back-reference to the model
        model.collection = this;
        Backbone.Collection.prototype._add.call(this, model, opts);
        
        if (!opts.backend) {
            var triple = jQuery.rdf.triple(
                this.uri, 
                this.property, 
                model.tojQueryRdf(), 
                {namespaces: VIE2.namespaces}
            );
            VIE2.globalCache.add(triple);
            if (this.parent) {
                this.parent.change();
            }
        }
    },
    
     _remove: function (model, opts) {
         if (model) {
             //remove corresponding triples
            VIE2.removeFromCache(this.uri, this.property, model.tojQueryRdf());
            
            Backbone.Collection.prototype._remove.call(this, model, opts);
             
            //update parent entity
            this.parent.change();
        }
     },
     
     getByValue: function (value, opts) {
         if (!opts) { opts = {}; }
         
         var found;
         $.each(this.models, function (i, model) {
             if (model.get('value') === value) {
                 if (opts.lang) {
                     if (opts.lang === model.get('lang')) {
                         found = model;
                         return false;
                     }
                 } else if (opts.datatype) {
                     if (opts.datatype === model.get('datatype')) {
                         found = model;
                         return false;
                     }
                 } else {
                     found = model;
                     return false;
                 }
             }
         });
         
         return found;
     }
});






// File:   model.js
// Author: <a href="mailto:sebastian.germesin@dfki.de">Sebastian Germesin</a>
//

//<strong>VIE2.Entity</strong>: The parent backbone entity class for all other entites.
//Inherits from VIE.RDFEntity.
VIE2.Entity = VIE.RDFEntity.extend({
    
    initialize: function (attrs, opts) {
        //if the type has changed, we need to search through the
        //mappings if the model needs to be inserted.
        this.bind('change:a', this.searchCollections);  
        
        if (!opts) { opts = {};}
        
        if (!opts.backend) {
            for (var attr in attrs) {
                var val = attrs[attr];
                if (attr !== 'id') {
                    if (jQuery.isArray(val)) {
                        for (var i = 0; i < val.length; i++) {
                            var triple = jQuery.rdf.triple(this.id, attr, val[i], {
                                namespaces: VIE2.namespaces
                            });
                            VIE2.globalCache.add(triple);
                        }
                    }
                    else {
                        var triple = jQuery.rdf.triple(this.id, attr, val, {
                            namespaces: VIE2.namespaces
                        });
                        VIE2.globalCache.add(triple);
                    }
                }
            }
        }
    },
    
    searchCollections: function () {
        var self = this;
        var types = VIE2.getFromCache(this, this.get('id'), 'a');
        
        jQuery.each(VIE2.mappings, function (i, mapping) {
            var belongsHere = false;
            
            for (var x = 0; x < types.length; x++) {
                var curie = types.at(x).get('value');
                if (!VIE2.Util.isCurie(curie)) {
                    curie = jQuery.createCurie(curie.replace(/^</, '').replace(/>$/, ''), {
                        namespaces: VIE2.namespaces,
                        charcase: 'lower'
                    }).toString();
                }
                if (mapping['a'].indexOf(curie) !== -1) {
                    belongsHere = true;
                    break;
                }
            }
            //entity needs to be registered with this mapping
            if (belongsHere) {
                //adding model instance to collection
                if (!mapping['collection'].get(self.id)) {
                    mapping['collection'].add(self, {backend: true});
                    VIE2.log("info", "VIE2.Entity.searchCollections()", "Added entity '" + self.get('id') + "' to collection of type '" + i + "'!");
                    
                    VIE2.log("info", "VIE2.Entity.searchCollections()", "Querying for default properties for entity '" + self.get('id') + "': [" + mapping['mapping'].defaults.join(", ") + "]!");
                    VIE2.lookup(self.get('id'), mapping['mapping'].defaults, function(defProps, model){
                        return function(){
                            VIE2.log("info", "VIE2.Entity.searchCollections()", "Finished querying for default properties for entity '" + model.get('id') + "': [" + defProps.join(", ") + "]!");
                            //trigger change when finished
                            for (var y = 0; y < defProps.length; y++) {
                                model.trigger('change:' + defProps[y]);
                            }
                            model.change();
                        };
                    }(mapping['mapping'].defaults, self));
                }
            }
        });
    },

    //overwritten to directly access the global Cache
    get: function (attr) {
        if (attr === 'id') {
            return VIE.RDFEntity.prototype.get.call(this, attr);
        }
        return VIE2.getFromCache(this, this.get('id'), attr);
    },
    
    save: function (attrs, options) {
        if (!options) { options = {};}
        if (attrs && !this.set(attrs, options)) return false;
            var model = this;
            var success = function(resp) {
                if (!model.set(model.parse(resp), options)) return false;
                if (options.success) options.success(model, resp);
            };
        var error = $.noop;
        var method = this.isNew() ? 'create' : 'update';
        this.sync(method, this, success, error, options);
        return this;
    },
    
    destroy: function (opts) {
        if (!opts) { opts = {};}
        
        var model = VIE.EntityManager.getBySubject(this.get('id'));
        if (model) {
            VIE2.entities.remove(model);
        }
        else {
            VIE2.removeFromCache(this.get('id'), '?x', '?y');
            
            var success = function(resp) {
                if (options.success) 
                    options.success(model, resp);
            };
            var error = $.noop;
            (this.sync || Backbone.sync)('delete', this, success, error);
        }
        return this;
    },
    
    sync: function (method, model, success, error, options) {
        if (!options) { options = {};}
        VIE2.log("info", "VIE2.Backbone#sync(" + model.get('id') + ")", "Start syncing!");
        
        var rdfTmp = jQuery.rdf({namespaces: VIE2.namespaces});
        
        VIE2.globalCache
        .where(model.get('id') + ' ?p ?o')
        .each(function (i, bindings, trs) {
            for (var j = 0; j < trs.length; j++) {
                rdfTmp.add(trs[j]);
            }
        });
        
        if (options.rules || options.props) {
            rdfTmp.reason(options.rules);
            //TODO: filter!
        }
        VIE2.log("info", "VIE2.Backbone#sync(" + model.get('id') + ")", "Found " + rdfTmp.length + " triples for serialization!");
            
        jQuery.each(VIE2.connectors, function (id, connector) {
            VIE2.log("info", "VIE2.Backbone#sync(" + model.get('id') + ")", "Using connector: '" + id + "'");
            connector.serialize(rdfTmp.databank, options);
        });
        
        //VIE.RDFEntity.prototype.sync.call(this, method, model, success, error);
        VIE2.log("info", "VIE2.Backbone#sync(" + model.get('id') + ")", "End syncing!");
    }
});

VIE2.createEntity = function (attrs, opts) {
    if (!('id' in attrs)) {
    	attrs.id = $.rdf.blank('[]').toString();
    }
    var model = new VIE2.Entity(attrs, opts);
    VIE2.entities.add(model, opts);
    return model;
};

VIE2.Object = Backbone.Model.extend({
    
    initialize: function (attrs, opts) {
        if (!opts) { opts = {};}
        
        this.isLiteral = opts.isLiteral;
    },
    
    set: function (attrs, opts) {
        if (!opts) { opts = {};}
        if (!attrs) return this;
        
        var oldValue = this.attributes['value'];
        var newValue = attrs['value'];
        
        if (oldValue !== undefined && oldValue !== newValue) {
            if (this.collection) {
                //TODO: Dear future me! This is a hack, please change that!
                //user driven change
                //add the new
                var inst = VIE2.createLiteral(newValue, {
                    lang: this.attributes['lang'],
                    datatype: this.attributes['datatype']
                });
                this.collection.add(inst);
                this.collection.parent.change();
                //remove the old one
                this.collection.remove(this);
            }   
        }
        return Backbone.Model.prototype.set.call(this, attrs, opts);
    },
    
    tojQueryRdf: function () {
        if (this.isLiteral) {
            return this._tojQueryRdfLit();
        } else {
            return this._tojQueryRdfRes();
        }
    },
    
    _tojQueryRdfLit: function () {
        var lang = (this.get('lang')) ? this.get('lang') : undefined;
        var datatype = (this.get('datatype')) ? this.get('datatype') : undefined;
        
        if (lang !== undefined && datatype !== undefined) {
            datatype = undefined;
        }
        return jQuery.rdf.literal(
            this.get('value'), {
                namespaces: VIE2.namespaces,
                datatype: datatype,
                lang: lang
        });
    },
    
    _tojQueryRdfRes: function () {
        return jQuery.rdf.resource(
            this.get('value'), {
                namespaces: VIE2.namespaces
        });
    }
});

VIE2.createLiteral = function (value, opts) {
    if (!opts) { opts = {};}
    return new VIE2.Object({
        value: value,
        isResource: false,
        lang: opts.lang,
        datatype: opts.datatype,
    }, jQuery.extend(opts, {isLiteral: true}));
};

VIE2.createResource = function (value, opts) {
     if (!opts) { opts = {};}
     return new VIE2.Object({
        value: value,
        isLiteral: false,
        isResource: true
    }, jQuery.extend(opts, {isLiteral: false}));
};// File:   connector.js
// Author: <a href="mailto:sebastian.germesin@dfki.de">Sebastian Germesin</a>
//

//The Connector class.
//So far, a connector has two main functionalities:
//1. analyze: Applies semantic lifting of the passed object
//2. query: Queries for properties of the given entity uri

//A connector needs an **id** of type string and an optional
//options object. The only option that is used in VIE&sup2; so far
//is options['namespaces'], which adds connector-specific 
//namespaces to VIE&sup2;. However, you may add other options,
//specific for this connector here.
//After registration, they can be changed with:
//<pre>
//   VIE2.connectors['<id>'].options({...});
//</pre>
VIE2.Connector = function(id, options) {
    //A connector needs an id of type string.    
    if (id === undefined || typeof id !== 'string') {
        throw "The connector constructor needs an 'id' of type 'string'!";
    }
    
    this.id = id;
    this._options = (options)? options : {};
    
    //registers the connector within VIE&sup2;. Also adds the given namespaces
    //to the global cache in VIE&sup2;.
    VIE2.registerConnector(this);
};

//setter and getter for options
VIE2.Connector.prototype.options = function(values) {
    if (typeof values === 'string') {
        //return the values
        return this._options[values];
    }
    else if (typeof values === 'object') {
        //extend options
        jQuery.extend(true, this._options, values);
    } else {
        //get options
        return this._options;
    }
};

//TODO: document me
VIE2.Connector.prototype.analyze = function (object, options) {
    VIE2.log("info", "VIE2.Connector(" + this.id + ")#analyze()", "Not overwritten!");
    if (options && options.success) {
        options.success.call(this, jQuery.rdf());
    }
};

//TODO: document me
VIE2.Connector.prototype.query = function (uri, properties, callback) {
    VIE2.log("info", "VIE2.Connector(" + this.id + ")#query()", "Not overwritten!");
    callback.call(this, {});
};

VIE2.Connector.prototype.serialize = function (rdf, options) {
    VIE2.log("info", "VIE2.Connector(" + this.id + ")#serialize()", "Not overwritten!");
    if (options && options.success) {
        options.success.call(this, {});
    }
};// File:   mapping.js
// Author: <a href="mailto:sebastian.germesin@dfki.de">Sebastian Germesin</a>
//

// A <code>Mapping</code> provides functionality to map cache knowledge
// to Backbone models

VIE2.Mapping = function(id, types, defaults, options) {
    if (id === undefined) {
        throw "The mapping constructor needs an 'id'!";
    }
    if (typeof id !== 'string') {
        throw "The mapping constructor needs an 'id' of type 'string'!";
    }
    if (types === undefined) {
        throw "The mapping constructor needs 'types'!";
    }
    
    this.id = id;
    
    this.options = (options)? options : {};
    
    //add given namespaces to VIE&sup2;'s namespaces
    if (this.options.namespaces) {
        jQuery.each(this.options.namespaces, function (k, v) {
           VIE2.namespaces[k] = v;
           VIE2.globalCache.prefix(k, v);
        });
    }
    
    //normalization to CURIEs (where needed)
    this.types = [];
    for (var i = 0; i < types.length; i++) {
        var type = types[i];
        if (!VIE2.Util.isCurie(type)) {
            type = jQuery.createCurie(type.replace(/^</, '').replace(/>$/, ''), {namespaces : VIE2.namespaces, charcase: 'lower'}).toString();
        }
        this.types.push(type);
    }

    //normalization to CURIEs (where needed)
    this.defaults = [];
    for (var i = 0; i < defaults.length; i++) {
        var d = defaults[i];
        if (!VIE2.Util.isCurie(d)) {
            d = jQuery.createCurie(d.replace(/^</, '').replace(/>$/, ''), {namespaces : VIE2.namespaces, charcase: 'lower'}).toString();
        }
        this.defaults.push(d);
    }
    
    //automatically registers the mapping in VIE^2.
    VIE2.registerMapping(this);
};// File:   util.js
// Author: <a href="mailto:sebastian.germesin@dfki.de">Sebastian Germesin</a>
//

VIE2.Util = {};

// <strong>VIE2.Util.(haystack, needle)</strong>: Removes the *needle* from the *haystack* array.<br>
// <code>return void</code> 
VIE2.Util.removeElement = function (haystack, needle) {
    //First we check if haystack is indeed an array.
    if (jQuery.isArray(haystack)) {
        //iterate over the array and check for equality.
        jQuery.each(haystack, function (index) {
            if (haystack[index] === needle) {
                //remove the one element and
                haystack.splice(index, 1);
                //break the iteration.
                return false;
            }
        });
    }
};

// <strong>VIE2.Util.isCurie(str)</strong>: Checks whether the given string is a curie.<br>
// <code>return boolean</code> 
VIE2.Util.isCurie = function (str) {
    return !str.substring(0, 1).match(/^<$/) && !(str.substring(0, 7).match(/^http:\/\/$/));
}

// <strong>VIE2.Util.isLiteral(str)</strong>: Checks whether the given string is a literal.<br>
// <code>return boolean</code> 
VIE2.Util.isLiteral = function (str) {
    try {
        jQuery.rdf.resource(str, {namespaces: VIE2.namespaces});
        return false;
    } catch (e) {
        try {
            jQuery.rdf.blank(str, {namespaces: VIE2.namespaces});
            return false;
        } catch (f) {
            try {
                jQuery.rdf.literal(str, {namespaces: VIE2.namespaces});
                return true;
            } catch (g) {
                return false;
            }
        }
    }
};

// <strong>VIE2.Util.isLiteral(str)</strong>: Checks whether the given string is a blank.<br>
// <code>return boolean</code> 
VIE2.Util.isBlank = function (str) {
    try {
        jQuery.rdf.resource(str, {namespaces: VIE2.namespaces});
        return false;
    } catch (h) {
        try {
            jQuery.rdf.blank(str, {namespaces: VIE2.namespaces});
            return true;
        } catch (i) {
            return false;
        }
    }
};

//calling this once for convenience
jQuery(document).vie2();


}).call(this);
