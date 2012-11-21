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

    <script>
    $(document).ready(function(){
        $('#loadingDiv')
        .hide()  // hide it initially
        .ajaxStart(function() {
            $(this).show();
        })
        .ajaxStop(function() {
            $(this).hide();
        });
        var z = new VIE();
        z.use(new z.StanbolService({
            // remove "/enhancervie" or "/enhancervie/" from the end of the uri.
            // it.publicBaseUri gives back http://localhost:8080 or so if redirected so it's not useful.
            url : window.location.href.replace(/\/[a-z]*\/?$/, ""), //"${it.publicBaseUri}",
            proxyDisabled: true
        }));

        // make the content element editable
        $('#content').hallo({
            plugins: {
              'halloformat': {}
            },
            editable: true
        });
        $('#content').annotate({
            vie: z,
            vieServices: ["stanbol"],
            showTooltip: true,
            debug: true,
            decline: function(event, ui){
                console.info('decline event', event, ui);
            },
            select: function(event, ui){
                console.info('select event', event, ui);
            },
            remove: function(event, ui){
                console.info('remove event', event, ui);
            }

        });

        $('.acceptAllButton')
        .button()
        .hide()
        .click(function(){
            $('#content')
            .annotate('acceptAll', function(report){
                console.log('AcceptAll finished with the report:', report);
            });
            $('.acceptAllButton')
            .button('disable');
        });

        $('.enhanceButton')
        .button({enhState: 'passiv'})
        .click(function(){
            // Button with two states
            var oldState = $(this).button('option', 'enhState');
            var newState = oldState === 'passiv' ? 'active' : 'passiv';
            $('.enhanceButton').button('option', 'enhState', newState);
            if($(this).button('option', 'enhState') === 'active'){
                // annotate.enable()
                try {
                    $('#content').annotate('enable', function(success){
                        if(success){
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
                    $('.enhanceButton')
                    .button('disable')
                    .button('option', 'label', 'in progress...')
                } catch (e) {
                    alert(e);
                }

            } else {
                // annotate.disable()
                $('#content').annotate('disable');
                $('.enhanceButton').button('option', 'label', 'Enhance!');
                $('.acceptAllButton')
                .hide()
            }
        });
    });
    </script>
    <button class="enhanceButton">Enhance!</button>
    <button class="acceptAllButton" style="display:none;">Accept all</button>
    <article typeof="schema:CreativeWork" about="http://stanbol.apache.org/enhancertest">
        <div property="sioc:content" id="content">
Text here...
        </div>
    </article>
    <button class="enhanceButton">Enhance!</button>
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
