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
extract such a taxonomy of topics you can use [dbpediakit][3] (you
will need python and postgresql for this to run):

    git clone https://github.com/ogrisel/dbpediakit
    cd dbpediakit

Create the dbpediakit database on the postgresql server by following the
instructions in:

    https://github.com/ogrisel/dbpediakit/blob/master/dbpediakit/postgres.py

You can now run the extraction (this will download the required dumps and load
them in postgresql hence can take a long time):

    python examples/topics/build_taxonomy.py --max-depth=2

Back in this folder, import the taxonomy and training set to Stanbol so
as to build the classifier model:

    python dbpediacategories.py
        /path/to/dbpediakit/dbpedia-taxonomy.tsv \
        /path/to/dbpediakit/dbpedia-examples.tsv.bz2 \
        http://localhost:8080/topic/model

[3] https://github.com/ogrisel/dbpediakit
