Geoname.org based Location Enhancement Engine for Apache Stanbol Enhancer

This engine creates fise:EntityAnnotations based on the http://geonames.org 
dataset. It does not directly work on the parsed content, but processes named 
entities extracted by some NLP (natural language processing) engine. This engine 
creates EnityAnnotations for Features found for named entities in the 
geonames.org data set. In addition it adds EntityAnnotations for the continent, 
country and administrative regions for entities with an high confidence level.

Processed Annotations (Input)

This engine consumes fise:TextAnnotations of type dbpedia:Place. More concrete 
it filters for enhancements that confirm to the following two requirements and 
consumes the text selected by the TextAnnotations:

 ?textAnnotation rdf:type fise:TextAnnotation .
 ?textAnnotation dc:type dbpedia:Place
 ?textAnnotation fise:selected-text ?text

Here an example for such an TextAnnotations selecting the text "Vienna" form the 
content "The community Workshop will take place in Vienna".
  
  urn:enhancement:text-enhancement:id1
     a       fise:TextAnnotation , fise:Enhancement ;
     dc:type
             dbpedia:Place ;
     fise:selected-text
             "Vienna"^^xsd:string ;
     fise:selection-context
             "The community Workshop will take place in Vienna"^^xsd:string ;
     fise:start
             "46"^^xsd:int ;
     fise:end
             "52"^^xsd:int ;
     fise:confidence
             "0.9773640902587215"^^xsd:double ;
     fise:extracted-from
             urn:content-item:id1 .

Typically such enhancements are created by engines that provide named entity 
extraction based on some natural language processing framework.


Created Enhancements (Output)

The LocationEnhancementEngine creates two types of EntityAnnotations. First it 
suggests Entities for processed TextAnnotations and second it creates 
EntityAnnotations for the hierarchy of regions the suggested Entities are 
located in. Suggested Entities are connected with the "dc:relation" attribute 
to the TextAnnotation they enhance. EntityAnnotations representing the hierarchy
define a dc:requires attribute to the EntityAnnotation.


Entity Suggestions

Entity suggestions are EntityEnhancements that suggest Features of the 
geonames.org dataset for an processed TextAnnotation. This suggestions are 
currently only calculated based on the fise:selected-text of the TextAnnotation. 
The following example shows three EntityAnnotations for the TextAnnotation used 
in the above example. See the fise:relation statements at the end of each of the 
two EntityAnnotations.

The first Entity found in the geonames.orf dataset is the capital city in 
Austria with an confidence level of 1.0:

  urn:enhancement:entity-enhancement:id1
     a      fise:EntityAnnotation , fise:Enhancement ;
     fise:confidence
             "1.0"^^xsd:double ;
     fise:entity-label
             "Vienna"^^xsd:string ;
     fise:entity-reference
             http://sws.geonames.org/2761369/ ;
     fise:entity-type
             geonames:Feature , dbpedia:Place , dbpedia:Settlement , dbpedia:PopulatedPlace , geonames:P.PPLC ;
     fise:extracted-from
             urn:content-item:id1 ;
    dc:relation
             urn:enhancement:text-enhancement:id1 .

With lower confidence levels there are a lot of other populated places with the 
name "Vienna" found in the geonames.org dataset.

  urn:enhancement:entity-enhancement:id2
     a      fise:EntityAnnotation , fise:Enhancement ;
     fise:confidence
             "0.42163702845573425"^^xsd:double ;
     fise:entity-label
             "Vienna"^^xsd:string ;
     fise:entity-reference
             http://sws.geonames.org/4496671/ ;
     fise:entity-type
             geonames:Feature , dbpedia:Place , dbpedia:Settlement , dbpedia:PopulatedPlace , geonames:P.PPL ;
     fise:extracted-from
             urn:content-item:id1 ;
    dc:relation
             urn:enhancement:text-enhancement:id1 .

  urn:enhancement:entity-enhancement:id3
     a      fise:EntityAnnotation , fise:Enhancement ;
     fise:confidence
             "0.42163702845573425"^^xsd:double ;
     fise:entity-label
             "Vienna"^^xsd:string ;
     fise:entity-reference
             http://sws.geonames.org/4825976/ ;
     fise:entity-type
             geonames:Feature , dbpedia:Place , dbpedia:Settlement , dbpedia:PopulatedPlace , geonames:P.PPL ;
     fise:extracted-from
             urn:content-item:id1 ;
    fdc:relation
             urn:enhancement:text-enhancement:id1 .


Entity Hierarchy Enhancements

Entity Hierarchy Enhancements describe the regions that contain suggested 
Features based on the geonames.org dataset. Enhancements describing this 
hierarchy are added for all suggested entities with a confidence level above 
the value of "eu.iksproject.fise.engines.geonames.locationEnhancementEngine.min-hierarchy-score". 
The default value for this property is 0.7. The hierarchy web service provided 
by geonames.org is used to calculate the regions:
The following example shows the entity hierarchy enhancements for the suggested 
entity for Vienna (Autria). Please note the dc:requires relation to this 
EntityAnnotation at the end of each of the following enhancement.
First the enhancement for the continent Europe:

  urn:enhancement:entity-hierarchy-enhancement:id1
     a      fise:EntityAnnotation , fise:Enhancement ;
     fise:confidence
             "0.42163702845573425"^^xsd:double ;
     fise:entity-label
             "Europe"^^xsd:string ;
     fise:entity-reference
             http://sws.geonames.org/6255148/ ;
     fise:entity-type
             geonames:Feature , dbpedia:Place, geonames:L.CONT ;
     fise:extracted-from
             urn:content-item:id1 ;
    dc:requires
             urn:enhancement:entity-enhancement:id1 .

Next the enhancement for the country "Austria", classified as an independent 
political entry within geonames.org

  urn:enhancement:entity-hierarchy-enhancement:id2
     a      fise:EntityAnnotation , fise:Enhancement ;
     fise:confidence
             "0.42163702845573425"^^xsd:double ;
     fise:entity-label
             "Austria"^^xsd:string ;
     fise:entity-reference
             http://sws.geonames.org/2782113/ ;
     fise:entity-type
             geonames:Feature , dbpedia:Place, dbpedia: AdministrativeRegion, geonames:A.PCLI ;
     fise:extracted-from
             urn:content-item:id1 ;
    dc:requires
             urn:enhancement:entity-enhancement:id1 .

Now three enhancement describing the different hierarchies of administrative 
regions within Austria. First the "Bundesland", next the "Stadtteil" and last 
the "Gemeindebezirk".

  urn:enhancement:entity-hierarchy-enhancement:id3
     a      fise:EntityAnnotation , fise:Enhancement ;
     fise:confidence
             "0.42163702845573425"^^xsd:double ;
     fise:entity-label
             "Vienna"^^xsd:string ;
     fise:entity-reference
             http://sws.geonames.org/2761367/ ;
     fise:entity-type
             geonames:Feature , dbpedia:Place, dbpedia: AdministrativeRegion, geonames:A.ADM1 ;
     fise:extracted-from
             urn:content-item:id1 ;
    dc:requires
             urn:enhancement:entity-enhancement:id1 .
  urn:enhancement:entity-hierarchy-enhancement:id4
     a      fise:EntityAnnotation , fise:Enhancement ;
     fise:confidence
             "0.42163702845573425"^^xsd:double ;
     fise:entity-label
             "Politischer Bezirk Wien (Stadt)"^^xsd:string ;
     fise:entity-reference
             http://sws.geonames.org/2761333/ ;
     fise:entity-type
             geonames:Feature , dbpedia:Place, dbpedia: AdministrativeRegion, geonames:A.ADM2 ;
     fise:extracted-from
             urn:content-item:id1 ;
    dc:requires
             urn:enhancement:entity-enhancement:id1 .
  urn:enhancement:entity-hierarchy-enhancement:id5
     a      fise:EntityAnnotation , fise:Enhancement ;
     fise:confidence
             "0.42163702845573425"^^xsd:double ;
     fise:entity-label
             "Gemeindebezirk Innere Stadt"^^xsd:string ;
     fise:entity-reference
             http://sws.geonames.org/2775259/ ;
     fise:entity-type
             geonames:Feature , dbpedia:Place, dbpedia: AdministrativeRegion, geonames:A.ADM3 ;
     fise:extracted-from
             urn:content-item:id1 ;
    dc:requires
             urn:enhancement:entity-enhancement:id1 .

The last two hierarchy levels are no longer valid for the meaning of "Vienna" as 
selected by the TextAnnotation, but added, because the geonames.org dataset 
locations the Feature of cities exactly in the center. However if the 
TextAnnotation would describe a precise address such hierarchy levels would 
completely make sense.
Configuration

The LocationEnhancementEngine provides currently six configurations

The first three can be used to optimise the behaviour of the Engine
 - Minimum score (default = 0.33): The minimum score (confidence) that is required
       for entity suggestions
 - Maximum Locations (default = 3): The maximum numbers of entity 
       suggestions added (regardless if there would be more results with a 
       score > min-score.
 - Maximum Locations (default = 0.7): The minimum score (confidence) that is 
       required that hierarchy enhancements are added for an suggested entity. 
       To add hierarchy enhancements for all suggested entities 
       min-hierarchy-score needs to be set to a value smaller equals 
       than min-score.
       
The other three are used to configure the configured geonames.org server
 - geonames.org Server: The URL of the geonames.org service. The default is the
       free geonames.org webserver that works without user authentication. There
       is a second free server at http://api.geonames.org/ that requires to setup
       a free user account. Users with a premium account will require to add here
       there own URL
 - User Name: Thats the name of the account (can be empty if the configured
       server does not require user authentication
 - Token: The token is usually the password of the user account.
 
 
 HOWTO setup a free user account: 
 
 Such an account is required to be able to use the http://api.geonames.org/ server
 that should support better performance and higher uptime than the default
 free server available at http://ws.geonames.org/.
 
To setup the free account:
(1) go to www.geonames.org. In the right top corner you will find a "login" link
    that is also used to create new accounts
(2) choose a username and pwd. You will get an confirmation mail at the provided 
    email address. When choosing the password consider, that it will be sent
    unencrypted (as token) with every webservice Request. Therefore it is
    strongly suggested to do not use an password that is used for any other
    account!  
(3) confirm the account
(4) IMPORTANT: You need to activate the free web service for the account via 
    http://www.geonames.org/manageaccount. Log in first, go back to this site. 
    At the botton you should find the text "the account is not yet enabled to 
    use the free web services. Click here to enable"

If you do not complete step (4) requests with your account will result in 
IOExceptions with the message
 "user account not enabled to use the free webservice. Please enable it on your account page: http://www.geonames.org/manageaccount"
 