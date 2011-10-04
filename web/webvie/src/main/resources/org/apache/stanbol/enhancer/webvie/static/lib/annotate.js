(function() {
  var __bind = function(fn, me){ return function(){ return fn.apply(me, arguments); }; };
  (function(jQuery) {
    var ANTT, Stanbol, ns;
    ns = {
      rdf: 'http://www.w3.org/1999/02/22-rdf-syntax-ns#',
      enhancer: 'http://fise.iks-project.eu/ontology/',
      dc: 'http://purl.org/dc/terms/',
      rdfs: 'http://www.w3.org/2000/01/rdf-schema#',
      skos: 'http://www.w3.org/2004/02/skos/core#'
    };
    ANTT = ANTT || {};
    Stanbol = Stanbol || {};
    Stanbol.getTextAnnotations = function(enhList) {
      var res;
      res = _(enhList).filter(function(e) {
        return e.isof("<" + ns.enhancer + "TextAnnotation>");
      });
      res = _(res).sortBy(function(e) {
        var conf;
        if (e.get("enhancer:confidence")) {
          conf = Number(e.get("enhancer:confidence"));
        }
        return -1 * conf;
      });
      return _(res).map(function(enh) {
        return new Stanbol.TextEnhancement(enh, enhList);
      });
    };
    Stanbol.getEntityAnnotations = function(enhList) {
      return _(enhList).filter(function(e) {
        return e.hasType("<" + ns.enhancer + "EntityAnnotation>");
      });
    };
    ANTT.getRightLabel = function(entity) {
      var cleanLabel, label, labelMap, userLang, _i, _len, _ref;
      labelMap = {};
      _ref = _(entity["" + ns.rdfs + "label"]).flatten();
      for (_i = 0, _len = _ref.length; _i < _len; _i++) {
        label = _ref[_i];
        cleanLabel = label.value;
        if (cleanLabel.lastIndexOf("@") === cleanLabel.length - 3) {
          cleanLabel = cleanLabel.substring(0, cleanLabel.length - 3);
        }
        labelMap[label["xml:lang"] || "_"] = cleanLabel;
      }
      userLang = window.navigator.language.split("-")[0];
      return labelMap[userLang] || labelMap["_"] || labelMap["en"];
    };
    Stanbol.TextEnhancement = function(enhancement, enhList) {
      this._enhancement = enhancement;
      this._enhList = enhList;
      return this.id = this._enhancement.getSubject();
    };
    Stanbol.TextEnhancement.prototype = {
      getSelectedText: function() {
        return this._vals("enhancer:selected-text");
      },
      getConfidence: function() {
        return this._vals("enhancer:confidence");
      },
      getEntityEnhancements: function() {
        var rawList;
        rawList = this._enhancement.get("entityAnnotation");
        if (!rawList) {
          return [];
        }
        rawList = _.flatten([rawList]);
        return _(rawList).map(__bind(function(ee) {
          return new Stanbol.EntityEnhancement(ee, this);
        }, this));
      },
      getType: function() {
        return this._uriTrim(this._vals("dc:type"));
      },
      getContext: function() {
        return this._vals("enhancer:selection-context");
      },
      getStart: function() {
        return Number(this._vals("enhancer:start"));
      },
      getEnd: function() {
        return Number(this._vals("enhancer:end"));
      },
      getOrigText: function() {
        var ciUri;
        ciUri = this._vals("enhancer:extracted-from");
        return this._enhList[ciUri]["http://www.semanticdesktop.org/ontologies/2007/01/19/nie#plainTextContent"][0].value;
      },
      _vals: function(key) {
        return this._enhancement.get(key);
      },
      _uriTrim: function(uriRef) {
        var bbColl, mod, _i, _len, _ref, _results;
        if (!uriRef) {
          return [];
        }
        if (uriRef instanceof Backbone.Model || uriRef instanceof Backbone.Collection) {
          bbColl = uriRef;
          _ref = bbColl.models;
          _results = [];
          for (_i = 0, _len = _ref.length; _i < _len; _i++) {
            mod = _ref[_i];
            _results.push(mod.get("@subject").replace(/^<|>$/g, ""));
          }
          return _results;
        } else {

        }
        return _(_.flatten([uriRef])).map(function(ur) {
          return ur.replace(/^<|>$/g, "");
        });
      }
    };
    Stanbol.EntityEnhancement = function(ee, textEnh) {
      this._enhancement = ee;
      this._textEnhancement = textEnh;
      return this;
    };
    Stanbol.EntityEnhancement.prototype = {
      getLabel: function() {
        return this._vals("enhancer:entity-label");
      },
      getUri: function() {
        return this._uriTrim(this._vals("enhancer:entity-reference"))[0];
      },
      getTextEnhancement: function() {
        return this._textEnhancement;
      },
      getTypes: function() {
        return this._uriTrim(this._vals("enhancer:entity-type"));
      },
      getConfidence: function() {
        return Number(this._vals("enhancer:confidence"));
      },
      _vals: function(key) {
        var res;
        res = this._enhancement.get(key);
        if (!res) {
          return [];
        }
        if (res.pluck) {
          return res.pluck("@subject");
        } else {
          return res;
        }
      },
      _uriTrim: function(uriRef) {
        var bbColl, mod, _i, _len, _ref, _results;
        if (!uriRef) {
          return [];
        }
        if (uriRef instanceof Backbone.Collection) {
          bbColl = uriRef;
          _ref = bbColl.models;
          _results = [];
          for (_i = 0, _len = _ref.length; _i < _len; _i++) {
            mod = _ref[_i];
            _results.push(mod.getSubject().replace(/^<|>$/g, ""));
          }
          return _results;
        } else if (uriRef instanceof Backbone.Model) {
          uriRef = uriRef.getSubject();
        }
        return _(_.flatten([uriRef])).map(function(ur) {
          return ur.replace(/^<|>$/g, "");
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
      var curData, events, i, internalKey, l, oldData, type;
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
          for (type in events) {
            i = 0;
            l = events[type].length;
            while (i < l) {
              jQuery.event.add(dest, type + (events[type][i].namespace ? "." : "") + events[type][i].namespace, events[type][i], events[type][i].data);
              i++;
            }
          }
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
              /*
                                      widget.options.vie.load({entity: uri}).using('stanbol').execute().success (entity) ->
                                          if not entity.status
                                              if entity.id isnt uri
                                                  widget._logger.warn "wrong callback", uri, entity.id
                                              cache._entities[uri].entity = entity
                                              cache._entities[uri].status = "done"
                                              $(cache._entities[uri]).trigger "done", entity
                                          else
                                              widget._logger.warn "error getting entity", uri, entity
                                      */
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
        return this.options.vie.analyze({
          element: this.element
        }).using(this.options.vieServices).execute().success(__bind(function(enhancements) {
          return _.defer(__bind(function() {
            var entAnn, entityAnnotations, textAnn, textAnnotations, _i, _len;
            entityAnnotations = Stanbol.getEntityAnnotations(enhancements);
            for (_i = 0, _len = entityAnnotations.length; _i < _len; _i++) {
              entAnn = entityAnnotations[_i];
              textAnn = entAnn.get("dc:relation");
              if (!(textAnn instanceof Backbone.Model)) {
                textAnn = entAnn.vie.entities.get(textAnn);
              }
              if (!textAnn) {
                continue;
              }
              _(_.flatten([textAnn])).each(function(ta) {
                return ta.set({
                  "entityAnnotation": entAnn.getSubject()
                });
              });
            }
            textAnnotations = Stanbol.getTextAnnotations(enhancements);
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
          }, this));
        }, this)).fail(function(xhr) {
          cb(false);
          return console.error("analyze failed", xhr.responseText, xhr);
        });
      },
      disable: function() {
        return $(':IKS-annotationSelector', this.element).each(function() {
          if ($(this).data().annotationSelector) {
            return $(this).annotationSelector('disable');
          }
        });
      },
      acceptAll: function(reportCallback) {
        var report;
        report = {
          updated: [],
          accepted: 0
        };
        $(':IKS-annotationSelector', this.element).each(function() {
          var res;
          if ($(this).data().annotationSelector) {
            res = $(this).annotationSelector('acceptBestCandidate');
            if (res) {
              report.updated.push(this);
              return report.accepted++;
            }
          }
        });
        return reportCallback(report);
      },
      processTextEnhancement: function(textEnh, parentEl) {
        var eEnh, eEnhUri, el, options, sType, type, widget, _i, _j, _len, _len2, _ref;
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
        sType = textEnh.getType() || "Other";
        widget = this;
        el.addClass('entity');
        for (_i = 0, _len = sType.length; _i < _len; _i++) {
          type = sType[_i];
          el.addClass(ANTT.uriSuffix(type).toLowerCase());
        }
        if (textEnh.getEntityEnhancements().length) {
          el.addClass("withSuggestions");
        }
        _ref = textEnh.getEntityEnhancements();
        for (_j = 0, _len2 = _ref.length; _j < _len2; _j++) {
          eEnh = _ref[_j];
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
        this.element.click(__bind(function(e) {
          console.log("click", e, e.isDefaultPrevented());
          e.preventDefault();
          if (!this.dialog) {
            this._createDialog();
            setTimeout((__bind(function() {
              return this.dialog.open();
            }, this)), 220);
            this.entityEnhancements = this._getEntityEnhancements();
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
      _getEntityEnhancements: function() {
        var eEnhancements, enhancement, textEnh, _i, _j, _len, _len2, _ref, _ref2, _tempUris;
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
        return _(eEnhancements).sortBy(function(e) {
          return -1 * e.getConfidence();
        });
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
        if (!src) {
          console.warn("No source");
        }
        if (!src) {
          return "";
        }
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
      annotate: function(entityEnhancement, options) {
        var entityClass, entityHtml, entityType, entityUri, newElement, rel, sType;
        entityUri = entityEnhancement.getUri();
        entityType = entityEnhancement.getTextEnhancement().getType() || "";
        entityHtml = this.element.html();
        sType = entityEnhancement.getTextEnhancement().getType();
        if (!sType.length) {
          sType = ["Other"];
        }
        rel = options.rel || ("" + ns.skos + "related");
        entityClass = 'entity ' + ANTT.uriSuffix(sType[0]).toLowerCase();
        newElement = $("<a href='" + entityUri + "'                about='" + entityUri + "'                typeof='" + entityType + "'                rel='" + rel + "'                class='" + entityClass + "'>" + entityHtml + "</a>");
        ANTT.cloneCopyEvent(this.element[0], newElement[0]);
        this.linkedEntity = {
          uri: entityUri,
          type: entityType,
          label: entityEnhancement.getLabel()
        };
        this.element.replaceWith(newElement);
        this.element = newElement.addClass(options.styleClass);
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
      acceptBestCandidate: function() {
        var eEnhancements;
        eEnhancements = this._getEntityEnhancements();
        if (!eEnhancements.length) {
          return;
        }
        if (this.isAnnotated()) {
          return;
        }
        this.annotate(eEnhancements[0], {
          styleClass: "acknowledged"
        });
        return eEnhancements[0];
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
            this.annotate(ui.item.data('enhancement'), {
              styleClass: 'acknowledged'
            });
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
        type = this._typeLabels(eEnhancement.getTypes()).toString() || "Other";
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
            return widget.options.vie.find({
              term: "" + req.term + (req.term.length > 3 ? '*' : '')
            }).using('stanbol').execute().fail(function(e) {
              return widget._logger.error("Something wrong happened at stanbol find:", e);
            }).success(function(entityList) {
              return _.defer(__bind(function() {
                var limit, res;
                widget._logger.info("resp:", _(entityList).map(function(ent) {
                  return ent.id;
                }));
                limit = 10;
                entityList = _(entityList).filter(function(ent) {
                  if (ent.id === "http://www.iks-project.eu/ontology/rick/query/QueryResultSet") {
                    return false;
                  }
                  return true;
                });
                res = _(entityList.slice(0, limit)).map(function(entity) {
                  return {
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
                  };
                });
                return resp(res);
              }, this));
            });
          },
          select: __bind(function(e, ui) {
            this.annotate(ui.item, {
              styleClass: "acknowledged"
            });
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
