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
    VIE2.logLevels=[];
    $(document).ready(function(){
        VIE2.connectors['stanbol'].options({
            "enhancer_url" : "/engines/",
            "entityhub_url" : "/entityhub/"
        });

        $('article.active-enhancement').remove();
        
        // make the content area editable
        $('#content').hallo({
            plugins: {
              'halloformat': {}
            },
            editable: true
        });
        $('#content').annotate({
            connector: VIE2.connectors['stanbol'],
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
            }
        });
    });
    </script>
    <button class="enhanceButton">Enhance!</button>
    <article typeof="schema:CreativeWork" about="http://stanbol.apache.org/enhancertest">
        <div property="sioc:content" id="content">
Turkey, known officially as the Republic of Turkey, is a Eurasian country that stretches across the Anatolian peninsula in western Asia and Thrace in the Balkan region of southeastern Europe. Turkey is one of the six independent Turkic states. Turkey is bordered by eight countries: Bulgaria to the northwest; Greece to the west; Georgia to the northeast; Armenia, Azerbaijan and Iran to the east; and Iraq and Syria to the southeast. The Mediterranean Sea and Cyprus are to the south; the Aegean Sea to the west; and the Black Sea is to the north. The Sea of Marmara, the Bosphorus and the Dardanelles (which together form the Turkish Straits) demarcate the boundary between Eastern Thrace and Anatolia; they also separate Europe and Asia. Turks began migrating into the area now called Turkey ("land of the Turks") in the eleventh century. The process was greatly accelerated by the Seljuk victory over the Byzantine Empire at the Battle of Manzikert. Several small beyliks and the Seljuk Sultanate of Rûm ruled Anatolia until the Mongol Empire's invasion. Starting from the thirteenth century, the Ottoman beylik united Anatolia and created an empire encompassing much of Southeastern Europe, Western Asia and North Africa. After the Ottoman Empire collapsed following its defeat in World War I, parts of it were occupied by the victorious Allies. A cadre of young military officers, led by Mustafa Kemal Atatürk, organized a successful resistance to the Allies; in 1923, they would establish the modern Republic of Turkey with Atatürk as its first president. Turkey's location at the crossroads of Europe and Asia makes it a country of significant geostrategic importance. The predominant religion in Turkey is Islam with small minorities of Christianity and Judaism. The country's official language is Turkish, whereas Kurdish and Zazaki languages are spoken by Kurds and Zazas which comprise 18% of the population. Turkey is a democratic, secular, unitary, constitutional republic, with an ancient cultural heritage. Turkey has become increasingly integrated with the West through membership in organizations such as the Council of Europe, NATO, OECD, OSCE and the G-20 major economies. Turkey began full membership negotiations with the European Union in 2005, having been an associate member of the European Economic Community since 1963 and having reached a customs union agreement in 1995. Turkey has also fostered close cultural, political, economic and industrial relations with the Middle East, the Turkic states of Central Asia and the African countries through membership in organizations such as the Organisation of the Islamic Conference and Economic Cooperation Organization. Given its strategic location, large economy and army, Turkey is classified as a regional power.
Actress Angelina Jolie, a longtime goodwill ambassador for the United Nations' refugee agency, will be headed to Turkey this week to visit Syrian refugees, Turkey's Foreign Ministry said Wednesday.
Jolie is expected to arrive in Istanbul and head to Hatay on Friday, according to the ministry, which accepted an application for her visit on Wednesday.
More than 8,000 Syrians have fled their country for Turkey to escape violence, including a military offensive in the Jisr al-Shugur area.
Jolie was named a goodwill ambassador for the Office of the High Commissioner for Refugees in early 2001 and has visited more than 20 countries "to highlight the plight of millions of uprooted people and to advocate for their protection."
The office said her interest in "humanitarian affairs was piqued in 2000 when she went to Cambodia to film the adventure film 'Tomb Raider.' "
Jolie has won numerous acting awards, including a best supporting actress Academy Award for her performance in 1999's "Girl, Interrupted."
        </div>
    </article>
    <button class="enhanceButton">Enhance!</button>
    
<br/><br/><br/><br/><br/><br/><br/><br/><br/><br/>

    <hr/>
    <div class="panel">
        <h3>Featuring annotate.js</h3>
        <h3>Features</h3>
        <ul>
            <li>Text-Enhancement support</li>
            <li>Spell-checker - like interaction with annotation enhancements</li>
            <li>Write RDFa into your content</li>
            <li>Independent of any editor</li>
            <li>One-line integration</li>
            <li>Configurable Enhancement types</li>
        </ul>

        <h3>Goals</h3>
        <ul>
            <li>Provide Text enhancement directly in your content</li>
            <li>Provide another open source (MIT license), flexibly usable, easy to 
                integrate tool for (semi-)automatic and manual semantic enhancement.</li>
            <li>
                A tool that's fun to integrate 
                <pre>
    var stanbolConnector = new StanbolConnector({
        "enhancer_url" : "http://example.com/engines/",
        "entityhub_url" : "http://example.com/entityhub/"
    });
    $('#content').annotate({
        connector: stanbolConnector
    });
                </pre>
            </li>
        </ul>
        <h3>Additional planned features</h3>
        <ul>
            <li>Manual annotation - loosely coupled wysiwyg-editor enhancement?, 
                needs selection-support</li>
            <li>Connection to VIE - loosely coupled, easy integration</li>
            <li>Clean up decoupling from the stanbol backend - schema mapping</li>
            <li>Preview with client-side templating with jquery</li>
            <li>Support Microformat</li>
            <li>Edit relationships</li>
        <h3>Dependencies</h3>
        <ul>
            <li>jQuery 1.5</li>
            <li>jQuery UI 1.9m5</li>
            <li>Backbone.js</li>
            <li>a wysiwyg-editor with save() (here: <a href="https://github.com/bergie/hallo">hallo editor</a>)</li>
            <li>VIE, VIE^2 (optional)</li>
        <ul>
        
    </div>
</div>

</@common.page>
</#escape>
