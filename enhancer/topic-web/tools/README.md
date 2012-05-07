# Helper Scripts to build training set and classifier models

Before using any of the following script you should configure a new
classifier model identified `model` for instance using the Felix System
Console at http://localhost:8080/system/console matching training set.
The HTTP API for that classifier model will be published at:

  http://localhost:8080/topic/model


## Using NewsML documents with IPTC subjects annotation

NewsML is standard XML file format used by major news agencies. The
topic of news articles can be categorized using a controlled vocabulary.

Such vocabulary can be loaded in the entityhub by copy the IPTC [zip
archive][1] in the `stanbol/datafiles` folder of a running server and
deploy the [referenced site definition jar][2] (for instance using the
Felix Console).

[1] http://dev.iks-project.eu/downloads/stanbol-indices/iptc.solrindex.zip
[2] http://dev.iks-project.eu/downloads/stanbol-indices/org.apache.stanbol.data.site.iptc-1.0.0.jar

If you have an archive of NewsML files at hand you can train a topic
classifier on by using the files to build the training set for the model
(you need Python 2.7 and lxml to run the script).

First import the RDF definition of the IPTC taxonomy into the model:

    TODO

Then import the data into the training set of the model:

    python newsmlimporter.py /path/to/newml/topleve/folder 10000 \
        http://localhost:8080/topic/model/trainingset

The second argument is the maximum number of news to import in the
training set.

You can then train the model with curl:

    curl -i -X POST http://localhost:8080/topic/model/trainer?incremental=false

The model can then be used as part of any enhancer engine chain to assign
IPTC topics to text documents.


## Using DBpedia categories

A subset of Wikipedia / DBpedia categories can be used as a classifier. To
extract such a taxonomy of topics you can use [dbpediakit][3]:

[3] https://github.com/ogrisel/dbpediakit

    python dbpediacategories.py topics.tsv examples.tsv \
        http://localhost:8080/topic/model
