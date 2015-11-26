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

# Build the jar
mvn package

# Get the JAR path
INDEXING_JAR=$PWD/`ls target/org.apache.stanbol.entityhub.indexing.dbpedia*jar | grep -v sources`
WORKSPACE=/tmp/dbpedia-index
DBPEDIA=http://downloads.dbpedia.org/current
MAX_SORT_MEM=2G
DBP_I18N=core-i18n

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
    echo "NB!: Downloading and parsing this file will take several hours"
    curl $DBPEDIA/$DBP_I18N/en/page-links_en.nt.bz2 \
        | bzcat \
        | sed -e 's/.*<http\:\/\/dbpedia\.org\/resource\/\([^>]*\)> ./\1/' \
        | sort -S $MAX_SORT_MEM \
        | uniq -c  \
        | sort -nr -S $MAX_SORT_MEM > $WORKSPACE/indexing/resources/incoming_links.txt
fi

# Download the RDF dumps:
cd $WORKSPACE/indexing/resources/rdfdata

# General attributes for all entities
DBP_MAIN=`curl -s http://downloads.dbpedia.org/current/ | grep owl.bz2 | cut -f2 -d"\""`
wget -c $DBPEDIA/$DBP_MAIN
wget -c $DBPEDIA/$DBP_I18N/en/instance-types_en.nt.bz2
wget -c $DBPEDIA/$DBP_I18N/ar/labels_ar.nt.bz2
wget -c $DBPEDIA/$DBP_I18N/en/labels_en.nt.bz2
wget -c $DBPEDIA/$DBP_I18N/es/labels_es.nt.bz2
wget -c $DBPEDIA/$DBP_I18N/fr/labels_fr.nt.bz2
wget -c $DBPEDIA/$DBP_I18N/he/labels_he.nt.bz2
wget -c $DBPEDIA/$DBP_I18N/it/labels_it.nt.bz2
wget -c $DBPEDIA/$DBP_I18N/ja/labels_ja.nt.bz2
wget -c $DBPEDIA/$DBP_I18N/ru/labels_ru.nt.bz2
wget -c $DBPEDIA/$DBP_I18N/tr/labels_tr.nt.bz2
wget -c $DBPEDIA/$DBP_I18N/zh/labels_zh.nt.bz2
wget -c $DBPEDIA/$DBP_I18N/en/short-abstracts_en.nt.bz2
wget -c $DBPEDIA/$DBP_I18N/en/long-abstracts_en.nt.bz2
wget -c $DBPEDIA/$DBP_I18N/en/article-categories_en.nt.bz2
wget -c $DBPEDIA/$DBP_I18N/en/category-labels_en.nt.bz2 
wget -c $DBPEDIA/$DBP_I18N/en/skos-categories_en.nt.bz2


# special handling of the image file that has 5 corrupted entries
if [ ! -f images_en.nt ]
then
    wget -c $DBPEDIA/$DBP_I18N/en/images_en.nt.bz2
    bzcat images_en.nt.bz2 \
      | sed 's/\\\\/\\u005c\\u005c/g;s/\\\([^u"]\)/\\u005c\1/g' > images_en.nt
    rm -f images_en.nt.bz2
fi

# same problem for german labels
if [ ! -f labels_de.nt ]
then
    wget -c $DBPEDIA/$DBP_I18N/de/labels_de.nt.bz2
    bzcat labels_de.nt.bz2 \
      | sed 's/\\\\/\\u005c\\u005c/g;s/\\\([^u"]\)/\\u005c\1/g' > labels_de.nt
    rm -f labels_de.nt.bz2
fi

# Type specific attributes
wget -c $DBPEDIA/$DBP_I18N/en/geo-coordinates_en.nt.bz2
wget -c $DBPEDIA/$DBP_I18N/en/persondata_en.nt.bz2

# Category information
wget -c $DBPEDIA/$DBP_I18N/en/category_labels_en.nt.bz2
wget -c $DBPEDIA/$DBP_I18N/en/skos_categories_en.nt.bz2
wget -c $DBPEDIA/$DBP_I18N/en/article_categories_en.nt.bz2

# Redirects
wget -c $DBPEDIA/$DBP_I18N/en/redirects_en.nt.bz2

set +xe

# Instruction to launch the indexing
echo "Preparation & data fetch done: edit config in $WORKSPACE/indexing/config/"
echo "Then launch indexing command:"
echo "(cd $WORKSPACE && java -jar $INDEXING_JAR index)"

