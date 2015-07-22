<!-- 
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

# Entity Co-reference Engine

The Entity co-reference Engine performs co-reference resolution of Named Entities in a given text. 
The co-references will be noun phrases which refer to those Named Entities by having a minimal set of attributes which match 
contextual information (rdf:type of the entity and spatial and object function giving info) from entity repositories
such as Dbpedia and Yago for that Named Entity.

We have the following text as an example : "Microsoft has posted its 2013 earnings. The software company did better than expected. ... The Redmond-based company will hire 500 new developers this year."
The enhancement engine will link "Microsoft" with "The software company" and "The Redmond-based company".

## (1) Configuring the Engine
TODO

## (2) Running the Entity co-reference engine in Stanbol.

In order to run the engine you need to add it to a chain that also contains the following engine types:
- a language detection engine
- a sentence detection engine (like opennlp-sentence)
- a token detection engine (like opennlp-token)
- a NER detection engine (like opennlp-ner)
- a noun phrase detection engine (like pos-chunker)

The default data bundle which contains dbpedia and yago data with which the coreferencing is done is the entity-coref-dbpedia data bundle.
You can find the bundle in data/sites/entity-coref-dbpedia. Install this bundle into Stanbol.
You can create your own data bundle but be sure to input the correct attributes when configuring the engine(see point no 1).
