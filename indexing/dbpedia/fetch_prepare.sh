#!/usr/bin/env bash

# Licensed to the Apache Software Foundation (ASF) under one or more
# contributor license agreements.  See the NOTICE file distributed with
# this work for additional information regarding copyright ownership.
# The ASF licenses this file to You under the Apache License, Version 2.0
# (the "License"); you may not use this file except in compliance with
# the License.  You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.


INDEXING_JAR=`pwd`/target/org.apache.stanbol.entityhub.indexing.dbpedia-*-jar-with-dependencies.jar
WORKSPACE=/tmp/dbpedia-index
DBPEDIA=http://downloads.dbpedia.org/3.7
MAX_SORT_MEM=2G

# Turn on echoing and exit on error
set -x -e -o pipefail

# Ensure that the workspace exists
mkdir -p $WORKSPACE

# Create the folder structure under the workspace folder
cd $WORKSPACE
java -jar $INDEXING_JAR init

# Rank entities by popularity by counting the number of incoming links in the
# wikipedia graph: computing this takes around 2 hours
if [ ! -f $WORKSPACE/indexing/resources/incoming_links.txt ]
then
    curl $DBPEDIA/en/page_links_en.nt.bz2 \
        | bzcat \
        | sed -e 's/.*<http\:\/\/dbpedia\.org\/resource\/\([^>]*\)> ./\1/' \
        | sort -S $MAX_SORT_MEM \
        | uniq -c  \
        | sort -nr -S $MAX_SORT_MEM > $WORKSPACE/indexing/resources/incoming_links.txt
fi

# Download the RDF dumps:
cd $WORKSPACE/indexing/resources/rdfdata

# General attributes for all entities
wget -c $DBPEDIA/dbpedia_3.7.owl.bz2
wget -c $DBPEDIA/en/instance_types_en.nt.bz2
wget -c $DBPEDIA/ar/labels_ar.nt.bz2
wget -c $DBPEDIA/en/labels_en.nt.bz2
wget -c $DBPEDIA/es/labels_es.nt.bz2
wget -c $DBPEDIA/fr/labels_fr.nt.bz2
wget -c $DBPEDIA/he/labels_he.nt.bz2
wget -c $DBPEDIA/it/labels_it.nt.bz2
wget -c $DBPEDIA/ja/labels_ja.nt.bz2
wget -c $DBPEDIA/ru/labels_ru.nt.bz2
wget -c $DBPEDIA/tr/labels_tr.nt.bz2
wget -c $DBPEDIA/zh/labels_zh.nt.bz2
wget -c $DBPEDIA/en/short_abstracts_en.nt.bz2
#wget -c $DBPEDIA/en/long_abstracts_en.not.bz2

# special handling of the image file that has 5 corrupted entries
if [ ! -f images_en.nt ]
then
    wget -c $DBPEDIA/en/images_en.nt.bz2
    bzcat images_en.nt.bz2 \
      | sed 's/\\\\/\\u005c\\u005c/g;s/\\\([^u"]\)/\\u005c\1/g' > images_en.nt
    rm -f images_en.nt.bz2
fi

# same problem for german labels
if [ ! -f labels_de.nt ]
then
    wget -c $DBPEDIA/de/labels_de.nt.bz2
    bzcat labels_de.nt.bz2 \
      | sed 's/\\\\/\\u005c\\u005c/g;s/\\\([^u"]\)/\\u005c\1/g' > labels_de.nt
    rm -f labels_de.nt.bz2
fi

# Type specific attributes
wget -c $DBPEDIA/en/geo_coordinates_en.nt.bz2
wget -c $DBPEDIA/en/persondata_en.nt.bz2

# Category information
#wget -c $DBPEDIA/en/category_labels_en.nt.bz2
#wget -c $DBPEDIA/en/skos_categories_en.nt.bz2
#wget -c $DBPEDIA/en/article_categories_en.nt.bz2

# Redirects
wget -c $DBPEDIA/en/redirects_en.nt.bz2

set +xe

# Instruction to launch the indexing
echo "Preparation & data fetch done: edit config in $WORKSPACE/indexing/config/"
echo "Then launch indexing command:"
echo "(cd $WORKSPACE && java -jar $INDEXING_JAR index)"

