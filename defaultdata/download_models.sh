#/bin/sh

OPENNLP_DATA=src/main/resources/org/apache/stanbol/defaultdata/opennlp
MODELS_URL="http://opennlp.sourceforge.net/models-1.5"
DBPEDIDA_SOLR_DATA=src/main/resources/org/apache/stanbol/defaultdata/site/dbpedia/index
DBPEDIA_SOLR_URL="http://dl.dropbox.com/u/5743203/IKS/dbpedia/3.6/dbpedia_43k.solrindex.zip"

rm -rf $OPENNLP_DATA/*.bin

(cd $OPENNLP_DATA && wget $MODELS_URL/en-sent.bin)
(cd $OPENNLP_DATA && wget $MODELS_URL/en-pos-perceptron.bin)
(cd $OPENNLP_DATA && wget $MODELS_URL/en-chunker.bin)
(cd $OPENNLP_DATA && wget $MODELS_URL/en-ner-person.bin)
(cd $OPENNLP_DATA && wget $MODELS_URL/en-ner-location.bin)
(cd $OPENNLP_DATA && wget $MODELS_URL/en-ner-organization.bin)


rm -rf $DBPEDIDA_SOLR_DATA/*.zip

(cd $DBPEDIDA_SOLR_DATA && wget $DBPEDIA_SOLR_URL)
