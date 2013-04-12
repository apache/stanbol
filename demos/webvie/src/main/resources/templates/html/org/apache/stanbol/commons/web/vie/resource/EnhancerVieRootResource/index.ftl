<#--
  Licensed to the Apache Software Foundation (ASF) under one or more
  contributor license agreements.  See the NOTICE file distributed with
  this work for additional information regarding copyright ownership.
  The ASF licenses this file to You under the Apache License, Version 2.0
  (the "License"); you may not use this file except in compliance with
  the License.  You may obtain a copy of the License at

      http://www.apache.org/licenses/LICENSE-2.0

  Unless required by applicable law or agreed to in writing, software
  distributed under the License is distributed on an "AS IS" BASIS,
  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  See the License for the specific language governing permissions and
  limitations under the License.
-->
<#import "/imports/common.ftl" as common>
<#escape x as x?html>
<@common.page title="Enhancer VIE" hasrestapi=false> 

<style>
article {
    padding: 10px;
}

#loadingDiv {
    position: absolute;
    top: 10px;
    right: 10px;
}

</style>
<div class="panel" id="webview"
     xmlns:sioc="http://rdfs.org/sioc/ns#"
     xmlns:schema="http://www.schema.org/"
     xmlns:enhancer="http://fise.iks-project.eu/ontology/"
     xmlns:dc="http://purl.org/dc/terms/">

    <div>
        <label>
            Enhancement chain:
            <select id="chain" value="default">
                <option>not loaded yet</option>
            </select>
        </label>
    </div>

    <script>
    $(document).ready(function(){
        // var stanbolUrl = "http://dev.iks-project.eu:8081";
        var stanbolUrl = window.location.href.replace(/\/[a-z]*\/?$/, "");
        console.info("${it.publicBaseUri}");
        $('#loadingDiv')
                .hide()  // hide it initially
                .ajaxStart(function () {
                    $(this).show();
                })
                .ajaxStop(function () {
                    $(this).hide();
                });
        function getChain() {
            return $("#chain").val();
        }

        // Whenever the chain selector changes, tell annotate.js about it.
        $("#chain").bind('change', function (e) {
            var z = new VIE();
            var chain = getChain();
            z.use(new z.StanbolService({
                // remove "/enhancervie" or "/enhancervie/" from the end of the uri.
                // it.publicBaseUri gives back http://localhost:8080 or so if redirected so it's not useful.
                url: stanbolUrl,
                enhancer: {chain: chain}
            }));

            $('#content').annotate('option', 'vie', z);
            var state = $('.enhanceButton').prop('checked');
            if (state) {
                $('#content').annotate('disable');
                _.defer(function () {
                    $('#content').annotate('enable');
                });
            }
        });

        // make the content element editable
        $('#content').hallo({
            plugins: {
//              'halloformat': {}
            },
            editable: true
        });

        function enable() {
            $('#content')
                    .each(function () {
                        $(this)
                                .annotate('enable', function (success) {
                                    if (success) {
                                        $('.enhanceButton')
                                                .button('enable')
                                                .button('option', 'label', 'Done');

                                        $('.acceptAllButton')
                                                .show()
                                                .button('enable')
                                    } else {
                                        $('.enhanceButton')
                                                .button('enable')
                                                .button('option', 'label', 'error, see the log.. Try to enhance again!');
                                    }
                                });
                    });
        }

        function instantiate() {
            var z = new VIE();
            z.use(new z.StanbolService({
                url: stanbolUrl
                // enhancer: {chain: getChain()}
            }));
            $('#content').annotate({
                vie: z,
                // typeFilter: ["http://dbpedia.org/ontology/Place", "http://dbpedia.org/ontology/Organisation", "http://dbpedia.org/ontology/Person"],
                debug: true,
                //autoAnalyze: true,
                showTooltip: true,
                decline: function (event, ui) {
                    console.info('decline event', event, ui);
                },
                select: function (event, ui) {
                    console.info('select event', event, ui);
                },
                remove: function (event, ui) {
                    console.info('remove event', event, ui);
                },
                success: function (event, ui) {
                    console.info('success event', event, ui);
                },
                error: function (event, ui) {
                    console.info('error event', event, ui);
                    alert(ui.message);
                }
            });
        }

        instantiate();

        $('.acceptAllButton')
                .button()
                .hide()
                .click(function () {
                    $('#content')
                            .each(function () {
                                $(this)
                                        .annotate('acceptAll', function (report) {
                                            console.log('AcceptAll finished with the report:', report);
                                        });
                            })
                    $('.acceptAllButton')
                            .button('disable');
                });

        $('.enhanceButton')
                .button()
                .change(function (e) {
                    var button = $(e.target);
                    var state = button.prop('checked');
                    $('.enhanceButton').prop('checked', state).button('refresh');

                    if (state) {
                        $('.enhanceButton').button('option', 'label', 'Done');
                        enable();
                        $('.acceptAllButton')
                                .show();
                    } else {
                        // annotate.disable()
                        $('#content').annotate('disable');
                        $('.enhanceButton').button('option', 'label', 'Enhance!');
                        $('.acceptAllButton')
                                .hide();
                    }

                });

        function getChains(stanbolUri, cb) {
            var query = "PREFIX enhancer: <http://stanbol.apache.org/ontology/enhancer/enhancer#> \n" +
                    "PREFIX rdfs:     <http://www.w3.org/2000/01/rdf-schema#> \n" +
                    "SELECT distinct ?name ?chain " +
                    "WHERE { " +
                    "?chain a enhancer:EnhancementChain. \n" +
                    "?chain rdfs:label ?name .\n" +
                    "} " +
                    "ORDER BY ASC(?name) ";

            function success(res) {
                var chains = $('binding[name=name] literal', res).map(function () {
                    return this.textContent;
                }).toArray();
                if (_(chains).indexOf('default') != -1) {
                    chains = _.union(['default'], chains);
                }
                cb(null, chains);
            }

            function error(xhr) {
                cb(xhr);
            }

            var uri = stanbolUri + "/enhancer/sparql";

            $.ajax({
                type: "POST",
                url: uri,
                data: {query: query},
                // accepts: ["application/json"],
                accepts: {'application/json': 'application/sparql-results+json'},
                // dataType: "application/json",
                success: success,
                error: error
            });
            // var xhr = $.getJSON(uri,success);xhr.error(error);
        }

        function fillChainList(cb) {
            var v = new VIE();
            v.use(new v.StanbolService({url: stanbolUrl}));
            getChains(stanbolUrl, function (err, chains) {
                if (err) {
                    console.info(err);
                    $('#chain').html("<option>Error loading chains</option>");
                    $('#error').html('Error loading list of chains from ' + stanbolUrl);
                } else {
                    console.info('Chains:', chains);
                    $('#chain').html('');
                    for (i in chains) {
                        var chain = chains[i];
                        $('#chain').append("<option value='" + chain + "'>" + chain + "</option>");
                    }
                    cb();
                }
            });

        }

        // Fill in the chain list, then initialize the annotate.js widget.
        fillChainList(instantiate);
    });
    </script>
    <input type="checkbox" class="enhanceButton" id="enhanceButton1"/><label for="enhanceButton1">Enhance!</label>
    <button class="acceptAllButton" style="display:none;">Accept all</button>
    <article typeof="schema:CreativeWork" about="http://stanbol.apache.org/enhancertest">
        <div property="sioc:content" id="content">
Text here...
        </div>
    </article>
    <input type="checkbox" class="enhanceButton" id="enhanceButton2"/><label for="enhanceButton2">Enhance!</label>
    <button class="acceptAllButton" style="display:none;">Accept all</button>
    
<br/><br/><br/><br/><br/><br/><br/><br/><br/><br/>

    <hr/>
    <div class="panel">
        <p>
            Powered by <a href="https://github.com/szabyg/annotate.js">annotate.js</a> and
            <a href="https://github.com/bergie/VIE">VIE</a>
        </p>
    </div>
</div>
<div id="loadingDiv"><img src="static/enhancervie/spinner.gif"/></div>

</@common.page>
</#escape>
