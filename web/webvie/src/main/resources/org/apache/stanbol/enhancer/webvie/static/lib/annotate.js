(function() {
  var __bind = function(fn, me){ return function(){ return fn.apply(me, arguments); }; };
  (function(jQuery) {
    var ANTT, Stanbol, ns;
    ns = {
      rdf: 'http://www.w3.org/1999/02/22-rdf-syntax-ns#',
      enhancer: 'http://fise.iks-project.eu/ontology/',
      dc: 'http://purl.org/dc/terms/',
      rdfs: 'http://www.w3.org/2000/01/rdf-schema#'
    };
    ANTT = ANTT || {};
    Stanbol = Stanbol || {};
    Stanbol.getTextAnnotations = function(enhRdf) {
      var res;
      res = _(enhRdf).map(function(obj, key) {
        obj.id = key;
        return obj;
      }).filter(function(e) {
        return e["" + ns.rdf + "type"].map(function(x) {
          return x.value;
        }).indexOf("" + ns.enhancer + "TextAnnotation") !== -1;
      });
      res = _(res).sortBy(function(e) {
        var conf;
        if (e["" + ns.enhancer + "confidence"]) {
          conf = Number(e["" + ns.enhancer + "confidence"][0].value);
        }
        return -1 * conf;
      });
      return _(res).map(function(s) {
        return new Stanbol.TextEnhancement(s, enhRdf);
      });
    };
    Stanbol.getEntityAnnotations = function(enhRdf) {
      return _(enhRdf).map(function(obj, key) {
        obj.id = key;
        return obj;
      }).filter(function(e) {
        return e["" + ns.rdf + "type"].map(function(x) {
          return x.value;
        }).indexOf("" + ns.enhancer + "EntityAnnotation") !== -1;
      });
    };
    ANTT.getRightLabel = function(entity) {
      var cleanLabel, label, labelMap, userLang, _i, _len, _ref;
      labelMap = {};
      _ref = _(entity["" + ns.rdfs + "label"]).flatten();
      for (_i = 0, _len = _ref.length; _i < _len; _i++) {
        label = _ref[_i];
        cleanLabel = label.value;
        if (cleanLabel.lastIndexOf("@" === cleanLabel.length - 3)) {
          cleanLabel = cleanLabel.substring(0, cleanLabel.length - 3);
        }
        labelMap[label["xml:lang"] || "_"] = cleanLabel;
      }
      userLang = window.navigator.language.split("-")[0];
      return labelMap[userLang] || labelMap["_"] || labelMap["en"];
    };
    Stanbol.TextEnhancement = function(enhancement, enhRdf) {
      this._enhancement = enhancement;
      this._enhRdf = enhRdf;
      return this.id = this._enhancement.id;
    };
    Stanbol.TextEnhancement.prototype = {
      getSelectedText: function() {
        return this._vals("" + ns.enhancer + "selected-text")[0];
      },
      getConfidence: function() {
        return this._vals("" + ns.enhancer + "confidence")[0];
      },
      getEntityEnhancements: function() {
        var rawList;
        rawList = _(Stanbol.getEntityAnnotations(this._enhRdf)).filter(__bind(function(ann) {
          var relations;
          relations = _(ann["" + ns.dc + "relation"]).map(function(e) {
            return e.value;
          });
          if ((relations.indexOf(this._enhancement.id)) !== -1) {
            return true;
          } else {
            return false;
          }
        }, this));
        return _(rawList).map(__bind(function(ee) {
          return new Stanbol.EntityEnhancement(ee, this);
        }, this));
      },
      getType: function() {
        return this._vals("" + ns.dc + "type")[0];
      },
      getContext: function() {
        return this._vals("" + ns.enhancer + "selection-context")[0];
      },
      getStart: function() {
        return Number(this._vals("" + ns.enhancer + "start")[0]);
      },
      getEnd: function() {
        return Number(this._vals("" + ns.enhancer + "end")[0]);
      },
      getOrigText: function() {
        var ciUri;
        ciUri = this._vals("" + ns.enhancer + "extracted-from")[0];
        return this._enhRdf[ciUri]["http://www.semanticdesktop.org/ontologies/2007/01/19/nie#plainTextContent"][0].value;
      },
      _vals: function(key) {
        return _(this._enhancement[key]).map(function(x) {
          return x.value;
        });
      }
    };
    Stanbol.EntityEnhancement = function(ee, textEnh) {
      this._textEnhancement = textEnh;
      return $.extend(this, ee);
    };
    Stanbol.EntityEnhancement.prototype = {
      getLabel: function() {
        return this._vals("" + ns.enhancer + "entity-label")[0];
      },
      getUri: function() {
        return this._vals("" + ns.enhancer + "entity-reference")[0];
      },
      getTextEnhancement: function() {
        return this._textEnhancement;
      },
      getTypes: function() {
        return this._vals("" + ns.enhancer + "entity-type");
      },
      getConfidence: function() {
        return Number(this._vals("" + ns.enhancer + "confidence")[0]);
      },
      _vals: function(key) {
        return _(this[key]).map(function(x) {
          return x.value;
        });
      }
    };
    ANTT.getOrCreateDomElement = function(element, text, options) {
      var domEl, len, nearest, nearestPosition, newElement, occurrences, pos, start, textContentOf, textToCut;
      if (options == null) {
        options = {};
      }
      occurrences = function(str, s) {
        var last, next, res, _results;
        res = [];
        last = 0;
        _results = [];
        while (str.indexOf(s, last + 1) !== -1) {
          next = str.indexOf(s, last + 1);
          res.push(next);
          _results.push(last = next);
        }
        return _results;
      };
      nearest = function(arr, nr) {
        return _(arr).sortedIndex(nr);
      };
      nearestPosition = function(str, s, ind) {
        var arr, d0, d1, i0, i1;
        arr = occurrences(str, s);
        i1 = nearest(arr, ind);
        if (arr.length === 1) {
          return arr[0];
        } else if (i1 === arr.length) {
          return arr[i1 - 1];
        } else {
          i0 = i1 - 1;
          d0 = ind - arr[i0];
          d1 = arr[i1] - ind;
          if (d1 > d0) {
            return arr[i0];
          } else {
            return arr[i1];
          }
        }
      };
      domEl = element;
      textContentOf = function(element) {
        return $(element).text().replace(/\n/g, " ");
      };
      if (textContentOf(element).indexOf(text) === -1) {
        throw "'" + text + "' doesn't appear in the text block.";
        return $();
      }
      start = options.start + textContentOf(element).indexOf(textContentOf(element).trim());
      start = nearestPosition(textContentOf(element), text, start);
      pos = 0;
      while (textContentOf(domEl).indexOf(text) !== -1 && domEl.nodeName !== '#text') {
        domEl = _(domEl.childNodes).detect(function(el) {
          var p;
          p = textContentOf(el).lastIndexOf(text);
          if (p >= start - pos) {
            return true;
          } else {
            pos += textContentOf(el).length;
            return false;
          }
        });
      }
      if (options.createMode === "existing" && textContentOf($(domEl).parent()) === text) {
        return $(domEl).parent()[0];
      } else {
        pos = start - pos;
        len = text.length;
        textToCut = textContentOf(domEl).substring(pos, pos + len);
        if (textToCut === text) {
          domEl.splitText(pos + len);
          newElement = document.createElement(options.createElement || 'span');
          newElement.innerHTML = text;
          $(domEl).parent()[0].replaceChild(newElement, domEl.splitText(pos));
          return $(newElement);
        } else {
          return console.warn("dom element creation problem: " + textToCut + " isnt " + text);
        }
      }
    };
    ANTT.uriSuffix = function(uri) {
      var res;
      res = uri.substring(uri.lastIndexOf("#") + 1);
      return res.substring(res.lastIndexOf("/") + 1);
    };
    ANTT.cloneCopyEvent = function(src, dest) {
      var curData, events, internalKey, oldData;
      if (dest.nodeType !== 1 || !jQuery.hasData(src)) {
        return;
      }
      internalKey = $.expando;
      oldData = $.data(src);
      curData = $.data(dest, oldData);
      if (oldData = oldData[internalKey]) {
        events = oldData.events;
        curData = curData[internalKey] = jQuery.extend({}, oldData);
        if (events) {
          delete curData.handle;
          curData.events = {};
          for ( var type in events ) {
                    for ( var i = 0, l = events[ type ].length; i < l; i++ ) {
                        jQuery.event.add( dest, type + ( events[ type ][ i ].namespace ? "." : "" ) + events[ type ][ i ].namespace, events[ type ][ i ], events[ type ][ i ].data );
                    }
                };
        }
        return null;
      }
    };
    jQuery.widget('IKS.annotate', {
      __widgetName: "IKS.annotate",
      options: {
        autoAnalyze: false,
        debug: false,
        ns: {
          dbpedia: "http://dbpedia.org/ontology/",
          skos: "http://www.w3.org/2004/02/skos/core#"
        },
        getTypes: function() {
          return [
            {
              uri: "" + this.ns.dbpedia + "Place",
              label: 'Place'
            }, {
              uri: "" + this.ns.dbpedia + "Person",
              label: 'Person'
            }, {
              uri: "" + this.ns.dbpedia + "Organisation",
              label: 'Organisation'
            }, {
              uri: "" + this.ns.skos + "Concept",
              label: 'Concept'
            }
          ];
        },
        getSources: function() {
          return [
            {
              uri: "http://dbpedia.org/resource/",
              label: "dbpedia"
            }, {
              uri: "http://sws.geonames.org/",
              label: "geonames"
            }
          ];
        }
      },
      _create: function() {
        var widget;
        widget = this;
        this._logger = this.options.debug ? console : {
          info: function() {},
          warn: function() {},
          error: function() {}
        };
        this.entityCache = window.entityCache = {
          _entities: {},
          get: function(uri, scope, cb) {
            var cache;
            if (this._entities[uri] && this._entities[uri].status === "done") {
              cb.apply(scope, [this._entities[uri].entity]);
            } else if (!this._entities[uri]) {
              this._entities[uri] = {
                status: "pending",
                uri: uri
              };
              cache = this;
              widget.options.connector.queryEntityHub(uri, function(entity) {
                if (!entity.status) {
                  if (entity.id !== uri) {
                    widget._logger.warn("wrong callback", uri, entity.id);
                  }
                  cache._entities[uri].entity = entity;
                  cache._entities[uri].status = "done";
                  return $(cache._entities[uri]).trigger("done", entity);
                } else {
                  return widget._logger.warn("error getting entity", uri, entity);
                }
              });
            }
            if (this._entities[uri] && this._entities[uri].status === "pending") {
              return $(this._entities[uri]).bind("done", function(event, entity) {
                return cb.apply(scope, [entity]);
              });
            }
          }
        };
        if (this.options.autoAnalyze) {
          return this.enable();
        }
      },
      enable: function(cb) {
        var analyzedNode;
        analyzedNode = this.element;
        return this.options.connector.analyze(this.element, {
          success: __bind(function(rdf) {
            var rdfJson, textAnnotations;
            rdfJson = rdf.databank.dump();
            textAnnotations = Stanbol.getTextAnnotations(rdfJson);
            textAnnotations = _(textAnnotations).filter(function(textEnh) {
              if (textEnh.getSelectedText && textEnh.getSelectedText()) {
                return true;
              } else {
                return false;
              }
            });
            _(textAnnotations).each(__bind(function(s) {
              this._logger.info(s._enhancement, 'confidence', s.getConfidence(), 'selectedText', s.getSelectedText(), 'type', s.getType(), 'EntityEnhancements', s.getEntityEnhancements());
              return this.processTextEnhancement(s, analyzedNode);
            }, this));
            this._trigger("done", true);
            if (typeof cb === "function") {
              return cb(true);
            }
          }, this)
        });
      },
      disable: function() {
        return $(':IKS-annotationSelector', this.element).each(function() {
          return $(this).annotationSelector('disable');
        });
      },
      processTextEnhancement: function(textEnh, parentEl) {
        var eEnh, eEnhUri, el, options, sType, widget, _i, _len, _ref;
        if (!textEnh.getSelectedText()) {
          console.warn("textEnh", textEnh, "doesn't have selected-text!");
          return;
        }
        el = $(ANTT.getOrCreateDomElement(parentEl[0], textEnh.getSelectedText(), {
          createElement: 'span',
          createMode: 'existing',
          context: textEnh.getContext(),
          start: textEnh.getStart(),
          end: textEnh.getEnd()
        }));
        sType = textEnh.getType();
        widget = this;
        el.addClass('entity').addClass(ANTT.uriSuffix(sType).toLowerCase());
        if (textEnh.getEntityEnhancements().length) {
          el.addClass("withSuggestions");
        }
        _ref = textEnh.getEntityEnhancements();
        for (_i = 0, _len = _ref.length; _i < _len; _i++) {
          eEnh = _ref[_i];
          eEnhUri = eEnh.getUri();
          this.entityCache.get(eEnhUri, eEnh, function(entity) {
            if (this.getUri() !== entity.id) {
              return widget._logger.warn("wrong callback", entity.id, this.getUri());
            }
          });
        }
        options = {
          cache: this.entityCache
        };
        $.extend(options, this.options);
        return el.annotationSelector(options).annotationSelector('addTextEnhancement', textEnh);
      }
    });
    ANTT.annotationSelector = jQuery.widget('IKS.annotationSelector', {
      __widgetName: "IKS.annotationSelector",
      options: {
        ns: {
          dbpedia: "http://dbpedia.org/ontology/",
          skos: "http://www.w3.org/2004/02/skos/core#"
        },
        getTypes: function() {
          return [
            {
              uri: "" + this.ns.dbpedia + "Place",
              label: 'Place'
            }, {
              uri: "" + this.ns.dbpedia + "Person",
              label: 'Person'
            }, {
              uri: "" + this.ns.dbpedia + "Organisation",
              label: 'Organisation'
            }, {
              uri: "" + this.ns.skos + "Concept",
              label: 'Concept'
            }
          ];
        },
        getSources: function() {
          return [
            {
              uri: "http://dbpedia.org/resource/",
              label: "dbpedia"
            }, {
              uri: "http://sws.geonames.org/",
              label: "geonames"
            }
          ];
        }
      },
      _create: function() {
        this.element.click(__bind(function() {
          var eEnhancements, enhancement, textEnh, _i, _j, _len, _len2, _ref, _ref2, _tempUris;
          if (!this.dialog) {
            this._createDialog();
            setTimeout((__bind(function() {
              return this.dialog.open();
            }, this)), 220);
            eEnhancements = [];
            _ref = this.textEnhancements;
            for (_i = 0, _len = _ref.length; _i < _len; _i++) {
              textEnh = _ref[_i];
              _ref2 = textEnh.getEntityEnhancements();
              for (_j = 0, _len2 = _ref2.length; _j < _len2; _j++) {
                enhancement = _ref2[_j];
                eEnhancements.push(enhancement);
              }
            }
            _tempUris = [];
            eEnhancements = _(eEnhancements).filter(function(eEnh) {
              var uri;
              uri = eEnh.getUri();
              if (_tempUris.indexOf(uri) === -1) {
                _tempUris.push(uri);
                return true;
              } else {
                return false;
              }
            });
            this.entityEnhancements = eEnhancements;
            this._createSearchbox();
            if (this.entityEnhancements.length > 0) {
              if (this.menu === void 0) {
                return this._createMenu();
              }
            }
          } else {
            return this.searchEntryField.find('.search').focus(100);
          }
        }, this));
        return this._logger = this.options.debug ? console : {
          info: function() {},
          warn: function() {},
          error: function() {}
        };
      },
      _destroy: function() {
        if (this.menu) {
          this.menu.destroy();
          this.menu.element.remove();
          delete this.menu;
        }
        if (this.dialog) {
          this.dialog.destroy();
          this.dialog.element.remove();
          this.dialog.uiDialogTitlebar.remove();
          return delete this.dialog;
        }
      },
      _typeLabels: function(types) {
        var allKnownPrefixes, knownMapping, knownPrefixes;
        knownMapping = this.options.getTypes();
        allKnownPrefixes = _(knownMapping).map(function(x) {
          return x.uri;
        });
        knownPrefixes = _.intersect(allKnownPrefixes, types);
        return _(knownPrefixes).map(__bind(function(key) {
          var foundPrefix;
          foundPrefix = _(knownMapping).detect(function(x) {
            return x.uri === key;
          });
          return foundPrefix.label;
        }, this));
      },
      _sourceLabel: function(src) {
        var sourceObj, sources;
        sources = this.options.getSources();
        sourceObj = _(sources).detect(function(s) {
          return src.indexOf(s.uri) !== -1;
        });
        if (sourceObj) {
          return sourceObj.label;
        } else {
          return src.split("/")[2];
        }
      },
      _createDialog: function() {
        var dialogEl, label, widget;
        label = this.element.text();
        dialogEl = $("<div><span class='entity-link'></span></div>").attr("tabIndex", -1).addClass().keydown(__bind(function(event) {
          if (!event.isDefaultPrevented() && event.keyCode && event.keyCode === $.ui.keyCode.ESCAPE) {
            this.close(event);
            return event.preventDefault();
          }
        }, this)).bind('dialogblur', __bind(function(event) {
          this._logger.info('dialog dialogblur');
          return this.close(event);
        }, this)).bind('blur', __bind(function(event) {
          this._logger.info('dialog blur');
          return this.close(event);
        }, this)).appendTo($("body")[0]);
        widget = this;
        dialogEl.dialog({
          width: 400,
          title: label,
          close: __bind(function(event, ui) {
            return this.close(event);
          }, this),
          autoOpen: false,
          open: function(e, ui) {
            return $.data(this, 'dialog').uiDialog.position({
              of: widget.element,
              my: "left top",
              at: "left bottom",
              collision: "none"
            });
          }
        });
        this.dialog = dialogEl.data('dialog');
        this.dialog.uiDialogTitlebar.hide();
        this._logger.info("dialog widget:", this.dialog);
        this.dialog.element.focus(100);
        window.d = this.dialog;
        this._insertLink();
        this._updateTitle();
        return this._setButtons();
      },
      _insertLink: function() {
        if (this.isAnnotated() && this.dialog) {
          return $("Annotated: <a href='" + this.linkedEntity.uri + "' target='_blank'>                " + this.linkedEntity.label + " @ " + (this._sourceLabel(this.linkedEntity.uri)) + "</a><br/>").appendTo($('.entity-link', this.dialog.element));
        }
      },
      _setButtons: function() {
        return this.dialog.element.dialog('option', 'buttons', {
          rem: {
            text: this.isAnnotated() ? 'Remove' : 'Decline',
            click: __bind(function(event) {
              return this.remove(event);
            }, this)
          },
          Cancel: __bind(function() {
            return this.close();
          }, this)
        });
      },
      remove: function(event) {
        var el;
        el = this.element.parent();
        if (!this.isAnnotated() && this.textEnhancements) {
          this._trigger('decline', event, {
            textEnhancements: this.textEnhancements
          });
        } else {
          this._trigger('remove', event, {
            textEnhancement: this._acceptedTextEnhancement,
            entityEnhancement: this._acceptedEntityEnhancement,
            linkedEntity: this.linkedEntity
          });
        }
        this.destroy();
        if (this.element.qname().name !== '#text') {
          return this.element.replaceWith(document.createTextNode(this.element.text()));
        }
      },
      disable: function() {
        if (!this.isAnnotated() && this.element.qname().name !== '#text') {
          return this.element.replaceWith(document.createTextNode(this.element.text()));
        }
      },
      isAnnotated: function() {
        if (this.element.attr('about')) {
          return true;
        } else {
          return false;
        }
      },
      annotate: function(entityEnhancement, styleClass) {
        var entityClass, entityHtml, entityType, entityUri, newElement, sType;
        entityUri = entityEnhancement.getUri();
        entityType = entityEnhancement.getTextEnhancement().getType();
        entityHtml = this.element.html();
        sType = entityEnhancement.getTextEnhancement().getType();
        entityClass = 'entity ' + ANTT.uriSuffix(sType).toLowerCase();
        newElement = $("<a href='" + entityUri + "'                about='" + entityUri + "'                typeof='" + entityType + "'                class='" + entityClass + "'>" + entityHtml + "</a>");
        ANTT.cloneCopyEvent(this.element[0], newElement[0]);
        this.linkedEntity = {
          uri: entityUri,
          type: entityType,
          label: entityEnhancement.getLabel()
        };
        this.element.replaceWith(newElement);
        this.element = newElement.addClass(styleClass);
        this._logger.info("created annotation in", this.element);
        this._updateTitle();
        this._insertLink();
        this._acceptedTextEnhancement = entityEnhancement.getTextEnhancement();
        this._acceptedEntityEnhancement = entityEnhancement;
        return this._trigger('select', null, {
          linkedEntity: this.linkedEntity,
          textEnhancement: entityEnhancement.getTextEnhancement(),
          entityEnhancement: entityEnhancement
        });
      },
      close: function() {
        return this.destroy();
      },
      _updateTitle: function() {
        var title;
        if (this.dialog) {
          if (this.isAnnotated()) {
            title = "" + this.linkedEntity.label + " <small>@ " + (this._sourceLabel(this.linkedEntity.uri)) + "</small>";
          } else {
            title = this.element.text();
          }
          return this.dialog._setOption('title', title);
        }
      },
      _createMenu: function() {
        var ul;
        ul = $('<ul></ul>').appendTo(this.dialog.element);
        this._renderMenu(ul, this.entityEnhancements);
        return this.menu = ul.menu({
          select: __bind(function(event, ui) {
            this._logger.info("selected menu item", ui.item);
            this.annotate(ui.item.data('enhancement'), 'acknowledged');
            return this.close(event);
          }, this),
          blur: __bind(function(event, ui) {
            return this._logger.info('menu.blur()', ui.item);
          }, this),
          focus: __bind(function(event, ui) {
            this._logger.info('menu.focus()', ui.item);
            return this._entityPreview(ui.item);
          }, this)
        }).bind('blur', function(event, ui) {
          return this._logger.info('menu blur', ui);
        }).bind('menublur', function(event, ui) {
          return this._logger.info('menu menublur', ui.item);
        }).focus(150).data('menu');
      },
      _renderMenu: function(ul, entityEnhancements) {
        var enhancement, _i, _len;
        entityEnhancements = _(entityEnhancements).sortBy(function(ee) {
          return -1 * ee.getConfidence();
        });
        for (_i = 0, _len = entityEnhancements.length; _i < _len; _i++) {
          enhancement = entityEnhancements[_i];
          this._renderItem(ul, enhancement);
        }
        return this._logger.info('rendered menu for the elements', entityEnhancements);
      },
      _renderItem: function(ul, eEnhancement) {
        var active, label, source, type;
        label = eEnhancement.getLabel().replace(/^\"|\"$/g, "");
        type = this._typeLabels(eEnhancement.getTypes());
        source = this._sourceLabel(eEnhancement.getUri());
        active = this.linkedEntity && eEnhancement.getUri() === this.linkedEntity.uri ? " class='ui-state-active'" : "";
        return $("<li" + active + "><a href='#'>" + label + " <small>(" + type + " from " + source + ")</small></a></li>").data('enhancement', eEnhancement).appendTo(ul);
      },
      _createSearchbox: function() {
        var sugg, widget;
        this.searchEntryField = $('<span style="background: fff;"><label for="search">Search:</label> <input id="search" class="search"></span>').appendTo(this.dialog.element);
        sugg = this.textEnhancements[0];
        widget = this;
        $('.search', this.searchEntryField).autocomplete({
          source: function(req, resp) {
            widget._logger.info("req:", req);
            return widget.options.connector.findEntity("" + req.term + (req.term.length > 3 ? '*' : void 0), function(entityList) {
              var entity, i, res;
              widget._logger.info("resp:", _(entityList).map(function(ent) {
                return ent.id;
              }));
              res = (function() {
                var _results;
                _results = [];
                for (i in entityList) {
                  entity = entityList[i];
                  _results.push({
                    key: entity.id,
                    label: "" + (ANTT.getRightLabel(entity)) + " @ " + (widget._sourceLabel(entity.id)),
                    _label: ANTT.getRightLabel(entity),
                    getLabel: function() {
                      return this._label;
                    },
                    getUri: function() {
                      return this.key;
                    },
                    _tEnh: sugg,
                    getTextEnhancement: function() {
                      return this._tEnh;
                    }
                  });
                }
                return _results;
              })();
              return resp(res);
            });
          },
          select: __bind(function(e, ui) {
            this.annotate(ui.item, "acknowledged");
            return this._logger.info("autocomplete.select", e.target, ui);
          }, this),
          focus: __bind(function(e, ui) {
            this._logger.info("autocomplete.focus", e.target, ui);
            return this._entityPreview(ui.item);
          }, this)
        }).focus(200).blur(__bind(function(e, ui) {
          return this._dialogCloseTimeout = setTimeout((__bind(function() {
            return this.close();
          }, this)), 200);
        }, this));
        return this._logger.info("show searchbox");
      },
      _entityPreview: _.throttle((function(item) {
        return this._logger.info("Show preview for", item);
      }), 1500),
      addTextEnhancement: function(textEnh) {
        this.options.textEnhancements = this.options.textEnhancements || [];
        this.options.textEnhancements.push(textEnh);
        return this.textEnhancements = this.options.textEnhancements;
      }
    });
    return window.ANTT = ANTT;
  })(jQuery);
}).call(this);
