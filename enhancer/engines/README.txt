Enhancement Engines
===================

This folder holds default implementations of enhancement engines
(available as OSGi services). The current engines mainly focus on semantic
lifting of unstructured content:

- autotagging:

  Given a content item of type text/plain, find a list of 3 suggestions of
  semantically  related entities or topics referenced in a Lucene index
  of entities such as DBpedia (see autotagging/README.txt for instructions
  to obtain such an index).

- opennlp-ner:

  Given a content item of type text/plain, identify names of persons, locations,
  organization, ... and there position in the text. If the autotagging service
  is registered, further try to find if those names can match known entities
  from the entity index or consider them as new entities otherwise.


