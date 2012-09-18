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

# Data files for the DBPedia.org Site shipped (by default) with the Stanbol distributions

This source repository only holds the pom.xml file and folder structure to build
the org.apache.stanbol.data.site.dbpedia artifact that includes a small
local solr index including the 43k dbpedia entities with the most incomming wiki
links on wikipedia.

This included index can be seen as preview data of bigger indexes with more
Entities and will be replaced by copying a bigger index into the 

    /sling/datafiles
    
directory within you Stanbol installation.

Such bigger indexes can be created by using the dbpedia indexing tool 
({stanbol-trunk}/entityhub/indexing/dbpedia) or downloaded via the web e.g.
from the [IKS demo server](http://dev.iks-project.eu/downloads/stanbol-indices/);

## Notes about the included DBPedia.org index

This bundle needs to include a small local index of DBPedia.org that includes the 43k entities with the most incoming Wiki links.

This index is not in the subversion but is downloaded from `http://www.salzburgresearch.at/~rwesten/stanbol/dbpedia_43k.solrindex.zip` by the maven build
and stored at`./src/main/resources/org/apache/stanbol/data/site/dbpedia/default/index/dbpedia_43k.solrindex.zip`.
