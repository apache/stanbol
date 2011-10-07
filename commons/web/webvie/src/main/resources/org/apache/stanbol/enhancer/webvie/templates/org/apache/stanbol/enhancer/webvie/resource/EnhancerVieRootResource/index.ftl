<#import "/imports/common.ftl" as common>
<#escape x as x?html>
<@common.page title="Enhancer VIE" hasrestapi=true> 

<style>
article {
    padding: 10px;
}
span.entity,
a[typeof][about] {
    z-index: -1;
    margin: -3px;
    padding: 1px;
    background-color: #E0E0E0;
    /* box-shadow: 2px 2px 5px grey;*/
    border-radius: 3px;
    border: outset rgba(0, 0, 0, 0.1);
    white-space: nowrap;
    border-width:2px;
}
a[typeof][about] {border-radius:1px;border-width:1px;}
a[typeof][about] {color: black}
.entity.withSuggestions {border-color: rgba(0, 0, 0, 0.5);}

.entity.person, 
a[typeof][about].person       {background-color: #ffe;}

.entity.place,
a[typeof][about].place        {background-color: #fef;}

.entity.organisation,
a[typeof][about].organisation {background-color: #eff;}

#loadingDiv {
    position: absolute;
    top: 10px;
    right: 10px;
}

/*
.entity.concept,
a[typeof][about].concept {background-color: #eef;}

.entity.acknowledged.person       {background-color: #ff9;}
.entity.acknowledged.place        {background-color: #f9d;}
.entity.acknowledged.organisation {background-color: #9ff;}
*/
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
        z.use(new z.StanbolService({url : "/"}));

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
