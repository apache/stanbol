#/bin/sh

OPENNLP_DATA=src/main/resources/opennlp
MODELS_URL="http://opennlp.sourceforge.net/models-1.5"


rm -rf $OPENNLP_DATA/*.bin

(cd $OPENNLP_DATA && wget $MODELS_URL/en-token.bin)
(cd $OPENNLP_DATA && wget $MODELS_URL/en-ner-person.bin)
(cd $OPENNLP_DATA && wget $MODELS_URL/en-ner-location.bin)
(cd $OPENNLP_DATA && wget $MODELS_URL/en-ner-organization.bin)

