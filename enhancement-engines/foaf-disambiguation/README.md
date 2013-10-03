Stanbol Entity Disambiguation using FOAF Correlation
======================================================

This is the Stanbol enhacement engine developed as part of the GSoC 2013 project [1]. <br/>
This engine uses FOAF correlation techniques to disambiguate suggested entities in a content.
The engine's main functionality is to increase the confidence of Entity-Annotations identified from previous engines, by using 2 fundamental techniques; <br/>
1. Processing correlating URI references in the Entities to detect connected-ness<br/>
2. Processing foaf:name comparison with fise:selected-text

Disambiguation Process in Detail
--------------------------------
In this project, 2 main algorithms are used as given above and finally the disambiguation-confidence calculated from both algorithms are merged as below; <br/>

original-confidence = oc<br/>
original-confidence-weight = ocw<br/>
correlation-disambiguation-score = cds<br/>
foaf-name-disambigiation-score = fds<br/>
foaf-name-disambiguation-weight = fdw<br/>

<code>disambiguated-confidence = (oc * ocw) + (cds * cdw) + (fds * fdw)</code>

Here the default weights used are as below;<br/>
<code>ocw : cdw : fdw = 1 : 2 : 2</code> <br/>

###Correlation based disambiguation

The main objective is to identify correlated URIs between entities and increase the confidence of the most 'connected' entity from the suggested entities. FOAF entities are connected to other similar entities via URI references like foaf:knows, owl:sameAs, rdf:seeAlso. These URI references can be used as correlation factors to cluster related FOAF entities and use it effectively for disambiguation. <br/>
Not only FOAF, any other types of entities including dbpedia-ont:Person, dbpedia-ont:Organization can be connected with eachother via URI references. In this project, correlation references between entities are used as a main factor for entity disambiguation. All URI/Reference type fields of the entities are extracted and processed to find correlations with other entities suggested. The most connected entity will have the most number of URI correlations.<br/>

The correlation based disambiguation algorithm basically follows below steps;<br/>
1. Process all entities suggested for the content and extract all unique URIReferences as keys and the entities linked to them as values in a Map. <br/>
2. For each URI reference, increase the correlation score of entity linked to it, relative to the number of entities linked.<br/>
3. Calculate the correlation-disambiguation-confidence based on the correlation-score and add it to the total disambiguated confidence. <br/>


###FOAF Name based disambiguation
The second technique used is literal matching of foaf:name field of the entity with the fise:selected-texts in the content. Each entity suggested for the content will be checked for the foaf:name property and it will be matched with the list of selected-texts. With an exact match, the disambiguated-confidence will be increased.<br/> 

Finally the cumulative disambiguated-confidence is calculated based on a weighted scale.<br/>

How to execute the engine
--------------------------
This engine requires Entity-Annotations extracted from previous engines, and entityhub pre-configured with FOAF entities. 
The entityhub-site: <code>foaf-site</code> created by indexing the btc2012 dataset including substantial amount of FOAF data can be found at [2]. <br/>
Please go through the steps in the project's README to configure the 'foaf-site' in Stanbol entityhub and use it in the foaf-site-chain enhancement-chain. The new disambiguation-foaf engine will be used to extend the functionality of this enhancement-chain in this project.<br/>

After configuring the 'foaf-site' with sufficient a FOAF dataset you can install and use the new engine by following below steps; <br/>
1. Build the maven project using command : <code>mvn clean install</code> <br/>
2. Start the Stanbol engine and install the bundle: <code>org.apache.stanbol.enhancer.engines.disambiguation.foaf-1.0-SNAPSHOT.jar</code><br/> 
3. Configure the foaf-site-chain with the new disambiguation engine

The new engine is identified by : <code>disambiguation-foaf</code>
Please note that in addition to the foaf-site I have also used entitylinking with dbpedia in the foaf-site-chain to increase the amount of entitiies for disambiguation.
Therefore after configuring the enhancement-chain successfully the foaf-site-chain should look like below; <br/>
<pre>
Engines: langdetect, opennlp-sentence, opennlp-token, opennlp-pos, foaf-site-linking, opennlp-ner, dbpediaLinking, disambiguation-foaf
</pre>

[1] http://www.google-melange.com/gsoc/proposal/review/google/gsoc2013/dileepaj/1 <br/>
[2] https://github.com/dileepajayakody/FOAFSite
