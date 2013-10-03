# Freebase Disambiguation Engine #

The Freebase Disambiguation Engine is a Stanbol Enhancer Engine responsible of try to disambiguate entities depending on the context of such entities.
This engine uses an algorithm based on minimum distances between entities in the Freebase Graph (generated using the [Freebase graph importer][1]) 

The disambiguation algorithm should take into account a local disambiguation score (comparing in some way the document context with the contexts provided by Wikilinks resource) and a global disambiguation score computed by a graph based algorithm using the Freebase graph imported in a Neo4j database. Each disambiguation score would have a different weight in the final disambiguation store for each entity. The algorithm's steps, for each TextAnnotation, can be the following:

1. Local score: for each EntityAnnotation, retrieves from Wikilinks database all the contexts associated to the referenced entity. Compare (similarity, distance....) the mention context (selected-context) with the wikilinks contexts.

2. Global score: build a subgraph with all the possible entities and its relations in Freebase. Extract a set of possibles solutions from such graph (note: a solution should include only one entity annotation for each text annotation). Compute the Dijsktra distance between each pair of entities belonging to a possible solution.

3. Weights normalization and confidence values refinement. 

## Freebase Stanbol Enhancer Engine ##

This engine implements the above algorithm but the first point (local score) which is not implemented in this version.

The algorithm builds a subgraph from the whole Freebase graph only for the entities returned after the NLP and Entity linking process, and the relations between them.

Using the Entity Annotations for each Text Annotation, it builds all the possible solutions for the text to enhance. It means, all the possible tuples result of combining the entities in each set of entity annotations (for each text annotation).

The searched solution is the tuple minimizing the distance in the graph between every pair of entities in the tuple. Minimal distance means higher disambiguation score.

## How to use it ##

In order to use the engine, do the following:  

1. Download the code
2. Run `'mvn clean package'` command
3. In the *target/* directory, find the bundle called `gsoc-freebase-disambiguation-{version}-jar`
4. Install it in Stanbol using the Felix Web Console

**Note:** This bundle depends on blueprints-core` and `blueprints-neo4j-graph`. You have to download the source code from [Blueprints repository][2] and use the pom files located in *src/main/resources* folder of this project to convert them into bundles and install them in Stanbol

## Configuration ##

Once the bundle is deployed and active in Stanbol, go to configuration tab in Felix Web Console of Stanbol and configure the *FreebaseDisambiguatprEngine*:
* Name of the engine: default value is **freebase-disambiguation**
* Neo4j graph location: default value is empty. You must set the location of the graph and restart the component in the *Component* tab.

The last step is configure a new engine in the enhancement chain using the name set in the configuration (freebase-disambiguator).

## Jira ##

This tool is related to the [issue 1157](https://issues.apache.org/jira/browse/STANBOL-1157) of Stanbol Jira.  

## License

GSoC Freebase Disambiguation Engine is distributed under the terms of the [Apache License, 2.0](http://www.apache.org/licenses/LICENSE-2.0.html).

[1]: https://github.com/adperezmorales/gsoc-freebase-graph-importer/tree/master/gsoc-freebase-graph-importer
[2]: https://github.com/tinkerpop/blueprints

