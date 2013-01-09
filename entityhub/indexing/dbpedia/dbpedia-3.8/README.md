DBpedia 3.8
===========

This folder contains work-in-progress scrips for

* creating incoming links file for different language versions of dbpedia
* downloading and pre-processing of dbpedia dump files

entityrankings.sh
-----------------

This will compute incoming links for the parsed language. In addition it will copy over the number of incoming links of the main DBpedia page to all pages that redirect to this page.

### usage

    ./entityrankings.sh {lang}

{lang} ... the language of the DBpedia dump to use (e.g. "en", "de" ...) 


fetch_data_**.sh
----------------

In DBpedia 3.8 most files are affected by the UTF-8 encoding issues that causes Errors during import of the data into Jena TDB. Because of that this scripts corrects all files downloaded.

The list of the downloaded files is specified in an array at the begin of the script. Users will need to edit this list based on their demands

Two examples are given: (1) DBpedia index based on the english dbpedia version with international labels (2) DBpedia index for german with english labels

Note that for non english DBpedia version one needs also to use the "copy_en_values.ldpath" LDPath program during indexing

copy_en_values.ldpath
---------------------

This is needed when indexin non english dbpedia dumps.

Make sure to copy this file into "indexing/config" and to configure 

    entityProcessor=org.apache.stanbol.entityhub.indexing.core.processor.FieldValueFilter,config:entityTypes;org.apache.stanbol.entityhub.indexing.core.processor.LdpathSourceProcessor,ldpath:copy_en_values.ldpath;org.apache.stanbol.entityhub.indexing.core.processor.FiledMapperProcessor

in your indexing.properties file.

Depending on your use case users might also want to add additional properties to the "copy_en_values.ldpath" program

The pattern is

    {property} = dbp-ont:wikiPageInterLanguageLink/{property};

