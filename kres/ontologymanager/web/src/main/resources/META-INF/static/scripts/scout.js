$(document).ready(function() {
	_ = {};
	_.labels = [];
	_.labels['http://www.cnr.it/ontology/cnr/istituti.owl#unitÌÄ OrganizzativaDiSupporto'] = 'UnitÌÄ  Organizzativa di supporto';
	_.labels['http://www.w3.org/2004/02/skos/core#subject'] = 'Subject';
	_.labels['http://localhost/cnr/categorie.owl#CategoriaDBpedia'] = 'Categoria DBpedia';
	_.labels['$default$'] = 'Altro';
	_.labels.get = function(uri) {
		if(!uri) return 'Label N/A';
		if(this[uri]) return this[uri].replace(/^./,function($0){return $0.toUpperCase()});
		else return uri.replace(/.+#([\w\d]+)$/,'$1').replace(/([A-Z])/g,' $1').replace(/^./,function($0){return $0.toUpperCase()});
	}
	_.rootId = 'http://www.cnr.it/ontology/cnr/individuo/unitaDiPersonaleInterno/MATRICOLA1582';
	_.rootId = window.rootId ? window.rootId : _.rootId;
	_.nodes = new Array();
	_.addNode = function(node){
		_.nodes[node.uri] = node;
	};

	_.nodeColors = {
		'http://www.cnr.it/ontology/cnr/categorie.owl#AreaDisciplinare' : '#90538A',
		'http://www.cnr.it/ontology/cnr/categorie.owl#AreaDiValutazione' : '#90538A',
		'http://www.cnr.it/ontology/cnr/categorie.owl#CategoriaDBpedia' : '#90538A',
		'http://localhost/cnr/categorie.owl#CategoriaDBpedia' : '#90538A',
		'http://www.cnr.it/ontology/cnr/categorie.owl#DisciplinaDiRiferimento' : '#90538A',

		'http://www.cnr.it/ontology/cnr/descrizionecommessa.owl#DescrizioneCollaborazioni' : '#FFFE12',
		'http://www.cnr.it/ontology/cnr/descrizionecommessa.owl#DescrizioneCommessa' : '#FFFE12',
		'http://www.cnr.it/ontology/cnr/descrizionecommessa.owl#DescrizioneStatoAvanzamentoAttivita' : '#FFFE12',
		'http://www.cnr.it/ontology/cnr/descrizionecommessa.owl#PrevisioneAttivita' : '#FFFE12',
		'http://www.cnr.it/ontology/cnr/descrizionedipartimento.owl#DescrizioneAttivita' : '#FFFE12',
		'http://www.cnr.it/ontology/cnr/descrizionedipartimento.owl#DescrizioneContesto' : '#FFFE12',
		'http://www.cnr.it/ontology/cnr/descrizionedipartimento.owl#DescrizioneDipartimento' : '#FFFE12',
		'http://www.cnr.it/ontology/cnr/descrizionedipartimento.owl#DescrizioneSettoreDiIntervento' : '#FFFE12',
		'http://www.cnr.it/ontology/cnr/descrizionemodulo.owl#DescrizioneModulo' : '#FFFE12',
		'http://www.cnr.it/ontology/cnr/descrizioneprogetto.owl#DescrizioneAttivita' : '#FFFE12',
		'http://www.cnr.it/ontology/cnr/descrizioneprogetto.owl#DescrizioneContesto' : '#FFFE12',
		'http://www.cnr.it/ontology/cnr/descrizioneprogetto.owl#DescrizioneObiettivi' : '#FFFE12',
		'http://www.cnr.it/ontology/cnr/descrizioni.owl#Descrizione' : '#FFFE12',
		'http://www.cnr.it/ontology/cnr/brevetti.owl#AreaTecnologica' : '#FFFE12',

		'http://www.cnr.it/ontology/cnr/istituti.owl#Direttore' : '#DE175A',
		'http://www.cnr.it/ontology/cnr/personale.owl#TipoDiRapporto' : '#DE175A',
		'http://www.cnr.it/ontology/cnr/personale.owl#UnitaDiPersonaleEsterno' : '#DE175A',
		'http://www.cnr.it/ontology/cnr/personale.owl#UnitaDiPersonaleInterno' : '#DE175A',
		'http://www.cnr.it/ontology/cnr/persone.owl#Persona' : '#DE175A',

		'http://www.cnr.it/ontology/cnr/personale.owl#PartecipazioneACommessa' : '#59E18F',
		'http://www.cnr.it/ontology/cnr/personale.owl#RapportoConCNR' : '#59E18F',


		'http://www.cnr.it/ontology/cnr/prodottidellaricerca.owl#RisultatoApplicativo' : '#008BDB',
		'http://www.cnr.it/ontology/cnr/prodottidellaricerca.owl#RisultatoProgettuale' : '#008BDB',
		'http://www.cnr.it/ontology/cnr/pubblicazioni.owl#Abstract' : '#008BDB',
		'http://www.cnr.it/ontology/cnr/pubblicazioni.owl#ArticoloConvegno' : '#008BDB',
		'http://www.cnr.it/ontology/cnr/pubblicazioni.owl#ArticoloISI' : '#008BDB',
		'http://www.cnr.it/ontology/cnr/pubblicazioni.owl#ArticoloNonISI' : '#008BDB',
		'http://www.cnr.it/ontology/cnr/pubblicazioni.owl#AttivitaEditoriale' : '#008BDB',
		'http://www.cnr.it/ontology/cnr/pubblicazioni.owl#Libro' : '#008BDB',
		'http://www.cnr.it/ontology/cnr/pubblicazioni.owl#Rapporto' : '#008BDB',
		'http://www.cnr.it/ontology/cnr/pubblicazioni.owl#Rivista' : '#008BDB',
		'http://www.cnr.it/ontology/cnr/pubblicazioni.owl#Tipologia' : '#008BDB',
		'http://www.cnr.it/ontology/cnr/brevetti.owl#Brevetto' : '#008BDB',

		'http://www.cnr.it/ontology/cnr/persone.owl#Organizzazione' : '#FFAE04',
		'http://www.cnr.it/ontology/cnr/istituti.owl#UnitaOrganizzativa' : '#FFAE04',
		'http://www.cnr.it/ontology/cnr/retescientifica.owl#Commessa' : '#FFAE04',
		'http://www.cnr.it/ontology/cnr/retescientifica.owl#Dipartimento' : '#FFAE04',
		'http://www.cnr.it/ontology/cnr/retescientifica.owl#Istituto' : '#FFAE04',
		'http://www.cnr.it/ontology/cnr/retescientifica.owl#Modulo' : '#FFAE04',
		'http://www.cnr.it/ontology/cnr/retescientifica.owl#Progetto' : '#FFAE04',
		'http://www.cnr.it/ontology/cnr/strutturacnr.owl#StrutturaGestionale' : '#FFAE04',

		'$default$':'#FFF'
	}



	_.nodeColors_old = {
		'http://www.cnr.it/ontology/cnr/personale.owl#UnitaDiPersonaleInterno' : '#CA5794',
		'http://www.cnr.it/ontology/cnr/personale.owl#UnitaDiPersonaleEsterno' : '#CA5794',
		'http://www.cnr.it/ontology/cnr/pubblicazioni.owl#ArticoloConvegno' : '#D0E332',
		'http://www.cnr.it/ontology/cnr/pubblicazioni.owl#Libro' : '#D0E332',
		'http://www.cnr.it/ontology/cnr/pubblicazioni.owl#ArticoloNonISI' : '#D0E332',
		'http://www.cnr.it/ontology/cnr/pubblicazioni.owl#ArticoloISI' : '#D0E332',
		'http://www.cnr.it/ontology/cnr/pubblicazioni.owl#AttivitaEditoriale' : '#D0E332',
		'http://www.cnr.it/ontology/cnr/retescientifica.owl#Istituto' : '#D8C090',
		'http://www.cnr.it/ontology/cnr/istituti.owl#UnitaOrganizzativa' : '#D8C090',
		'http://www.cnr.it/ontology/cnr/retescientifica.owl#Commessa' : '#B06A12',
		'http://www.cnr.it/ontology/cnr/personale.owl#RapportoConCNR' : '#E2B365',
		'http://www.cnr.it/ontology/cnr/personale.owl#PartecipazioneACommessa' : '#5796E6',
		'http://www.cnr.it/ontology/cnr/categorie.owl#CategoriaDBpedia' : '#EDCE35',
		'http://localhost/cnr/categorie.owl#CategoriaDBpedia' : '#EDCE35',
		'$default$':'lime'
	}
	_.H = {
		i : 0,
		items : [],
		push : function(item) {
			this.items.splice(this.items.length-this.i,this.i,item);
			this.i = 0;
			this.setbuttons();
		},
		go : function(delta){
			if(!this.test(delta)) return null;
			delta = -1*delta;
			this.i += delta;
			this.setbuttons();
			_.displayData(_.nodes[this.items[this.items.length-this.i-1]]);
			return this.items[this.items.length-this.i-1];
		},
		get : function(delta){
			if(!this.test(delta)) return null;
			delta = -1*delta;
			return this.items[this.items.length - this.i + delta - 1];
		},
		test : function (delta) {
			delta = -1*delta;
			if((this.i + delta)>=this.items.length || (this.i + delta<0)) return false;
			return true
		},
		setbuttons : function(){
			if(this.test(+1)) _.forwButton.button('enable');
			else _.forwButton.button('disable');
			if(this.test(-1)) _.backButton.button('enable');
			else _.backButton.button('disable');
		},
		clear : function(){
			this.i = 0;
			this.items = [];
			this.setbuttons();
		}
	}


	function init(){
		//init RGraph
		_.graph = new $jit.Hypertree({
		//_.graph = new $jit.RGraph({
			//Where to append the visualization
			injectInto: 'infovis',
			//Optional: create a background canvas that plots
			//concentric circles.
			/*
			background: {
				CanvasStyles: {
					strokeStyle: '#555'
				}
			},
			*/
			//Add navigation capabilities:
			//zooming by scrolling and panning.
			Navigation: {
				enable: true,
				panning: 'avoid nodes',
				zooming: 100,
//				type: 'auto'
			},
			transition: $jit.Trans.Quad.easeOut,
			duration: 500,
			fps: 60,
			Node: {
				dim: 4,
				color: _.nodeColors['$default$'],
				transform:false,
				overridable:true
			},
			Edge: {
				lineWidth: 1,
				color: _.nodeColors['$default$'],
				alpha : 1,
				overridable:true
			},

			onCreateLabel: function(domElement, node){
				$(domElement).html('<span class="short">'+node.name.substring(0,20)+((node.name).length>20 ? ' ...' : '')+'</span><span class="full">'+node.name+'</span>');
				$(domElement).attr('title',node.name+' [type: '+_.labels.get(node.data['http://www.w3.org/1999/02/22-rdf-syntax-ns#type'][0]['value'])+' | id: '+node.id+']');
//				$(domElement).css('color',_.nodeColors[node.data['http://www.w3.org/1999/02/22-rdf-syntax-ns#type'][0]['value']] || _.nodeColors['$default$']);
//				domElement.onclick = function(){  
//					_.graph.onClick(node.id);
//				};
				$(domElement).click(function(){
					if(node.id == _.H.get(0)) return;
//					$('#loader').fadeTo('slow', 0.8);
					_.graph.onClick(node.id);
					_.H.push(node.id);
					_.displayData(_.nodes[node.id]);
//					$('#loader').fadeOut();
				});
//				$(domElement).click(function(){alert('!')
//					if(node.id == _.H.get(0)) {
//						_.displayData(_.nodes[node.id]);
//						return;
//					}
//					_.H.push(node.id);
//					if(!_.nodes[node.id]) {
//						_.sum(node.id);
//					} else {
//						_.graph.onClick(node.id);
//					}
//					_.displayData(_.nodes[node.id]);
//				});
				$(domElement).mouseover(function(){
					$(domElement).addClass('nodeOver');
				});
				$(domElement).mouseout(function(){
					$(domElement).removeClass('nodeOver');
				});
			},
			onPlaceLabel: function(domElement, node){
				$(domElement).css({
					'display':'block',
					'cursor':'pointer'
				});
				$(domElement).attr('class','node dept-'+(node._depth < 3 ? node._depth : 'deep'));
				var style = domElement.style; 
				var left = parseInt(style.left);
				var w = domElement.offsetWidth;
				style.left = (left - w / 2) + 'px';
			},
			onBeforePlotNode:function(node){
				node.data['$color'] = _.nodeColors[node.data['http://www.w3.org/1999/02/22-rdf-syntax-ns#type'][0]['value']] || _.nodeColors['$default$'];
				node.data['$dim'] = node._depth == 0 ? 6 : (4-node._depth)>=0 ? 4-node._depth : 0
			},
			onBeforePlotLine:function(adj){
				adj.data['$color'] = _.nodeColors[adj.nodeTo.data['http://www.w3.org/1999/02/22-rdf-syntax-ns#type'][0]['value']] || _.nodeColors['$default$'];
			},

		});
	}
	/**
	 * Function initInterface()
	 */
	_.initInterface = function(){
		_.infovis = $('#infovis');
		_.w = _.infovis.width() - 400;
		_.h = _.infovis.height()-10;
		_.r = _.w<_.h ? _.w/2 : _.h/2;
	}

	_.onRelClick = function(o){
		o = document.getElementById(o);
		$(o).click();
	}
	_.onRelOver = function(o){
		o = document.getElementById(o);
		$(o).mouseover();
	}
	_.onRelOut = function(o){
		o = document.getElementById(o);
		$(o).mouseout();
	}

	_.sum = function(uri){
		$.getJSON('json.php?url='+uri,function(data){
			_.parseCurrentJSONData(uri,data);
			_.graph.op.sum(_.makeJSONTree(_.nodes[uri]),{
				type: 'fade:con',
				duration: 500,
				onComplete:function(){
				//					_.displayData(_.nodes[_.H.get(0)]);
				}
			});
		});
	}
	/*
	 * function setDataBoxLabel(node)
	 */
	_.setDataBoxLabel = function(node){
		$('#currentnodelabel').html(node.getLabel());
	}
	/*
	 * function setDataBoxMetatadataTab(node)
	 */
	_.setDataBoxMetatadataTab = function(node){
		//		var metadataboxDL = $('<dl/>').appendTo($('#metadatabox'));
		//		$.each(node.getMetadatas(),function(k,v){
		//			$('<dt/>').html(k).appendTo(metadataboxDL);
		//			$.each(v,function(kk,vv){
		//				$(metadataboxDL).append($('<dd/>').html(vv));
		//			})
		//		});
		$('#metadatabox').html('');
		$.each(node.getMetadatas(),function(k,v){
			$.each(v,function(kk,vv){
				$('#metadatabox').append('<div><strong>'+vv.label+'</strong>: '+vv.value+'</div>');
			})
		});
	}

	/*
	 * function setDataBoxRelationsTab(node)
	 */
	_.setDataBoxRelationsTab = function(node){
		$('#relationsbox').html('');
		var ul = $('<ul></ul>');
		$.each(node.getRelations(),function(k,v){
			var li = $('<li></li>');
			var toggler = $('<span class="toggler"/>').click(function(){
				$('ul',this.parentNode).slideToggle();
			}).css({
				'cursor' : 'pointer'
			}).appendTo(li);
			var massdownloader = $('<span class="massdowloader"/>').click(function(){
				$('[title="Download node"]',this.parentNode).click();
			}).css({
				'cursor' : 'pointer'
			}).appendTo(li);
			$('<span/>').css({
				'float': 'left',
				'margin-right': '0.3em'
			}).attr('class','opencloseButt ui-icon ui-icon-triangle-2-n-s').appendTo(toggler);
			$('<span/>').css({
				'float': 'left',
				'margin-right': '0.3em'
			}).attr('class','opencloseButt ui-icon ui-icon-arrowthickstop-1-s').appendTo(massdownloader);
			$('<span>'+_.labels.get(k)+' ('+v.size+')'+'</span>').appendTo(li);
			var subul = $('<ul class="subul"></ul>').appendTo(li);
//			$.each(v,function(kk,vv){
			for(kk in v) {
				var vv = v[kk];
				var currid = vv['uri'];
				var hasNode = _.graph.graph.getNode(currid) && true;
				var spanLabel = $('<span/>').html(vv['value']);
				var goButt = $('<span/>');
				goButt.css({
					'float': 'left',
					'margin-right': '0.3em',
					'cursor' : 'pointer'
				}).attr('class','ui-icon ' + (hasNode ? 'ui-icon-circle-triangle-e' : 'ui-icon-arrowthickstop-1-s')).click(function(){
					var targetId = $(this).parent().attr('id').replace(/^rel:\/\//,'');
					var hasNode = _.graph.graph.getNode(targetId);
					if(hasNode) _.onRelClick($(this).parent().attr('id').replace(/^rel:\/\//,''));
					else{
						//						_.H.push(targetId);
						_.sum(targetId);
					}
					$(this).removeClass('ui-icon-arrowthickstop-1-s').addClass('ui-icon-circle-triangle-e');
				}).attr('title',hasNode ? 'Show node' : 'Download node').html('&nbsp;');
				var infoButt = $('<span/>').css({
					'float': 'left',
					'margin-right': '0.3em',
					'cursor' : 'pointer'
				}).attr('class','ui-icon ui-icon-info').click(function(){
					var targetId = $(this).parent().attr('id').replace(/^rel:\/\//,'');
					var winTitle = $(this).parent().attr('title');
					_.scheda(targetId,winTitle);
				}).attr('title','More info').html('&nbsp;');
				var cartButt = $('<span/>').css({
					'float': 'left',
					'margin-right': '0.3em',
					'cursor' : 'pointer'
				}).attr('class','ui-icon ui-icon-cart').click(function(){
					var nodeToAdd = {
						id : $(this).parent().attr('id').replace(/^rel:\/\//,''),
						name : $(this).parent().attr('title')
					}
					_.basket.add(nodeToAdd);
					$(this).fadeOut(300).fadeIn(300);
				}).attr('title','Aggiungi al carrello').html('&nbsp;');
				var subli = $('<li/>').append(infoButt,goButt,cartButt,spanLabel).mouseover(function(){
					_.onRelOver((this.id).replace(/^rel:\/\//,''));
				}).mouseout(function(){
					_.onRelOut((this.id).replace(/^rel:\/\//,''));
				}).attr('id','rel://'+currid).attr('title',vv['value']);
				$(subul).append(subli);
			}
			$(ul).append(li);
		});
		$('#relationsbox').append(ul);
		//$('.subul').toggle(false);
	}
	_.displayData = function(node){
		_.setDataBoxLabel(node);
		_.setDataBoxMetatadataTab(node);
		_.setDataBoxRelationsTab(node);
	}
	_.makeJSONTree = function(node){
		var json = {
			'id' : node.getUri(),
			'name' : node.getLabel(),
			'data' : _.nodes[node.getUri()].data,//_.currentJSONData[node.getUri()],
			'children' : []
		};
		$.each(node.getRelations(),function(k,v){
			$.each(v,function(kk,vv){
				if(_.nodes[vv['uri']]) json.children.push({
					'id' : vv['uri'],
					'name' : vv['value'],
					'data' : _.currentJSONData[vv['uri']],
					'children' : []
				});
			})
		});
		return json;
	}
	_.basket = {
		items : {},
		add : function(node){
			if(!node) {
				var currentNodeId = _.H.get(0);
				node = _.graph.graph.getNode(currentNodeId);
			}
			if(this.items[node.id]) return;
			var div = $('<div></div>').appendTo('#basketitems');
			var spanLabel = $('<span/>').html(node.name)
			var buttonGo = $('<span/>').css({
				'float': 'left',
				'margin-right': '0.3em',
				'cursor' : 'pointer'
			}).attr('class','ui-icon ui-icon-circle-triangle-e').click(function(){
				var targetId = node.id;
				var hasNode = _.graph.graph.getNode(targetId);
				if(hasNode) _.onRelClick(targetId);
				else{
					_.H.push(targetId);
					_.sum(targetId);
				}
			/*
				_.H.push(node.id);
				if(in_array(rdfinfo.loaded,node.id)) _.graph.onClick(node.id);
				else _.sum(node.id);
*/
			}).attr('title',node.name);
			var buttonDel = $('<span/>').css({
				'float': 'left',
				'margin-right': '0.3em',
				'cursor' : 'pointer'
			}).attr('class','ui-icon ui-icon-trash').click(function(){
				delete(_.basket.items[node.id]);
				_.basketExportButton.button('disable');
				for(var k in _.basket.items) _.basketExportButton.button('enable');
				$(div).remove();
			}).attr('title','Remove');
			$(div).append(buttonGo,buttonDel,spanLabel);
			this.items[node.id] = node.name;
			_.basketExportButton.button('enable');
		}
	};
	_.scheda = function(uri,title){
		$('<div></div>').css({
			'text-align':'left',
			'font-size':'80%'
		}).attr('title',title).load('scheda.plain.php?url='+uri+' #scheda dl').dialog({
			width:500,
			height:document.getElementById('infovis').offsetHeight/2,
			position:['center',document.getElementById('infovis').offsetHeight/10]
		})
	}
	_.Node = function(uri){
		var _ = this;
		this.id = uri;
		this.uri = uri;
		this.label = '';
		this.name = '';
		this.data = {};
		this.metadatas = {};
		this.relations = {};
		this.setLabel = function(string) {
			_.name = _.label = string;
		}
		this.addMetaData = function(meta){
			if(!this.metadatas[meta.id]) this.metadatas[meta.id] = new Array();
			this.metadatas[meta.id].push({
				'id' : meta.id,
				'label' : meta.label,
				'value' : meta.value
			});
		}
		this.addRelation = function(rel){
			if(!this.relations[rel.id]) this.relations[rel.id] = {size:0};
			if(!this.relations[rel.id][rel.uri]) this.relations[rel.id].size++;
			this.relations[rel.id][rel.uri] = {
				id : rel.id,
				uri : rel.uri,
				types : rel.types,
				value : rel.value,
				label : rel.label
			}
		}
		this.getUri = function(){
			return this.uri
		};
		this.getLabel = function(){
			return this.label
		};
		this.getMetadatas = function(){
			return this.metadatas
		};
		this.getRelations = function(){
			return this.relations
		};
		if(this.data['http://www.w3.org/1999/02/22-rdf-syntax-ns#type']==undefined) this.data['http://www.w3.org/1999/02/22-rdf-syntax-ns#type'] = [{value:null}];
		return this;
	}	
	_.parseCurrentJSONData = function(rootId,data){
		_.currentJSONData = data;
		node = new _.Node(rootId);
		for(var i in data[rootId]) {
			node.data[i] = data[rootId][i];
			for(var j in data[rootId][i]) {
				if(data[rootId][i][j]['type']=='literal') {
					node.addMetaData({
						'id' : i,
						'label' : _.labels.get(i),
						'value' : data[rootId][i][j]['value']
					});
				}
				else{
					if(data[data[rootId][i][j]['value']])
						for(var k in data[data[rootId][i][j]['value']]['http://www.w3.org/2000/01/rdf-schema#label']){
							if(i == 'http://www.w3.org/1999/02/22-rdf-syntax-ns#type'){
								node.addMetaData({
									'id' : i,
									'label' : _.labels.get(i),
									'value' : data[data[rootId][i][j]['value']]['http://www.w3.org/2000/01/rdf-schema#label'][k]['value']
								});
							} else {
								var types = new Array();
								for(var n in _.currentJSONData[data[rootId][i][j]['value']]['http://www.w3.org/1999/02/22-rdf-syntax-ns#type']) {
									types.push({
										'type' : _.currentJSONData[data[rootId][i][j]['value']]['http://www.w3.org/1999/02/22-rdf-syntax-ns#type'][n]['value'],
										'value' : _.labels.get(_.currentJSONData[data[rootId][i][j]['value']]['http://www.w3.org/1999/02/22-rdf-syntax-ns#type'][n]['value'])
									});
								}
								node.addRelation({
									'id' : i,
									'label' : _.labels.get(i),
									'uri' : data[rootId][i][j]['value'],
									'types': types,
									'value' : data[data[rootId][i][j]['value']]['http://www.w3.org/2000/01/rdf-schema#label'][k]['value']
								})
							}
						}
				}
			}
		}
		node.addMetaData({
			id : 'uri',
			label : 'uri',
			value : rootId
		})
		if(node.metadatas && node.metadatas['http://www.w3.org/2000/01/rdf-schema#label'] && node.metadatas['http://www.w3.org/2000/01/rdf-schema#label'][0] &&  node.metadatas['http://www.w3.org/2000/01/rdf-schema#label'][0]['value'])
			node.setLabel(node.metadatas['http://www.w3.org/2000/01/rdf-schema#label'][0]['value']);
		else
			node.setLabel('Label N/A');
		_.addNode(node);
	//_.displayData(node);
	}	
	_.kickStart = function(){
		var rootId = _.rootId;
		$.getJSON('ontolabels.php',function(triples){
			for(i in triples) {
				_.labels[i] = triples[i]["http://www.w3.org/2000/01/rdf-schema#label"][0].value;
			}
			$.getJSON('json.php?url='+rootId,function(data){
				_.parseCurrentJSONData(rootId,data);
				_.graph.loadJSON(_.makeJSONTree(_.nodes[rootId]));
				_.graph.refresh();
				_.H.push(rootId);
				_.graph.onClick(rootId);
				_.displayData(_.nodes[rootId]);
			});
			for (i in _.nodeColors) {
				$('<div>').css('color',_.nodeColors[i]).html('<span>&#x2022;</span> '+_.labels[i]).appendTo($('#legenda'));
			}
		});
	}
	_.applyInterface = function(){
		_.infoButton = $('<button/>').button({
			label:'Info',
			text:true,
			icons:{
				primary:'ui-icon-info'
			}
		}).click(function(){
			var targetNode = _.nodes[_.H.get(0)];
			_.scheda(targetNode.uri,targetNode.label);
		});
		_.homeButton = $('<button/>').button({
			label:'Nodo iniziale',
			text:true,
			icons:{
				primary:'ui-icon-flag'
			}
		}).click(function(){
			if(_.H.get(0)==_.rootId) return;
			$(_.backButton).button('enable');
			_.H.push(_.rootId);
			_.graph.onClick(_.rootId);
		});
		_.backButton = $('<button/>').button({
			label:'Back',
			text:true,
			icons:{
				primary:'ui-icon-carat-1-w'
			}
		}).click(function(){
			var curr = _.H.go(-1);
			_.graph.onClick(curr);
		});
		_.forwButton = $('<button/>').button({
			label:'Forward',
			text:true,
			icons:{
				primary:'ui-icon-carat-1-e'
			}
		}).click(function(){
			var curr = _.H.go(1);
			_.graph.onClick(curr);
		});
		_.detailsButton = $('<button/>').button({
			label:'Schede',
			text:true,
			icons:{
				primary:'ui-icon-carat-2-n-s'
			}
		}).click(function(){
			$('#databox').slideToggle();
		});
		_.legendButton = $('<button/>').button({
			label:'Legenda',
			text:true,
			icons:{
				primary:'ui-icon-carat-2-n-s'
			}
		}).click(function(){
			$('#legenda').slideToggle();
		});



		_.basketButton = $('<button/>').button({
			label:'Aggiungi',
			text:true,
			icons:{
				primary:'ui-icon-cart'
			}
		}).click(function(){
			var currentNodeId = _.H.get(0);
			//			var node = _.graph.graph.getNode(currentNodeId);
			var node = _.nodes[currentNodeId];
			_.basket.add(node);
			$(this).fadeOut(300).fadeIn(300);
		});
		_.basketExportButton = $('<button/>').button({
			label:'Esporta il carrello',
			disabled:true,
			icons:{
				primary:'ui-icon-arrowreturnthick-1-s'
			}
		}).click(function(){
			uriList = new Array();
			for(var item in _.basket.items) uriList.push(item);
			uriList = uriList.join(',');
			window.open('basketexp.php?u='+uriList);
		}).appendTo('#basketui');
		$('body').append($('<div></div>').append(_.basketButton,_.infoButton,/*_.refreshButton,*/_.homeButton,_.backButton,_.forwButton,_.legendButton,_.detailsButton).attr('id','ui'));
		$('body').append($('<div></div>').attr('id','loader'));
		$('#databox').css({
			//			'width' : $('#infovis').innerWidth() - 2*_.r - 60,
			'width':'400px',
			'height':(_.h-50)+'px'
		});
		$('#metadatabox').css({
			'height':(_.h-160)+'px'
		});
		$('#relationsbox').css({
			'height':(_.h-160)+'px'
		});
		$("#databox-tabs").tabs();
		//		$('#currentnodelabel').click(function(){$("#databox-tabs").slideToggle()});
		$('#loader').ajaxSend(function(){
			$(this).fadeTo('slow', 0.8);
		});
		$('#loader').ajaxStop(function(){
			$(this).fadeOut();
		});
		$('#loader').ajaxError(function(){
			$(this).fadeOut();
			alert('Data loading error!');
		});
		$(window).error(function(){
			$('#loader').fadeOut();
			return true;
		});
	}
	
	
	_.initInterface();
	_.applyInterface();
	_.kickStart();
	init()
});
