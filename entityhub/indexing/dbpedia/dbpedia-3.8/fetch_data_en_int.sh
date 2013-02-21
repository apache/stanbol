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


INDEXING_JAR=./org.apache.stanbol.entityhub.indexing.dbpedia-*-jar-with-dependencies.jar
WORKSPACE=.
DBPEDIA=http://downloads.dbpedia.org/3.8

# Turn on echoing and exit on error
set -x -e -o pipefail

java -jar $INDEXING_JAR init

# Download the RDF dumps:
cd $WORKSPACE/indexing/resources/rdfdata

# General attributes for all entities

files=(dbpedia_3.8.owl \
    en/labels_en.nt \
    en/short_abstracts_en.nt \
    en/long_abstracts_en.nt \
    en/instance_types_en.nt \
    en/images_en.nt \
    en/geo_coordinates_en.nt \
    en/redirects_en.nt \
    en/page_links_en.nt \
    en/mappingbased_properties_en.nt \
    de/labels_en_uris_de.nt \
    ar/labels_en_uris_ar.nt es/labels_en_uris_es.nt fr/labels_en_uris_fr.nt \
    he/labels_en_uris_he.nt it/labels_en_uris_it.nt ja/labels_en_uris_ja.nt \
    ru/labels_en_uris_ru.nt tr/labels_en_uris_tr.nt nl/labels_en_uris_nl.nt \
    zh/labels_en_uris_zh.nt pt/labels_en_uris_pt.nt sv/labels_en_uris_sv.nt \
    da/labels_en_uris_da.nt \
    de/short_abstracts_en_uris_de.nt es/short_abstracts_en_uris_es.nt \
    fr/short_abstracts_en_uris_fr.nt ar/short_abstracts_en_uris_ar.nt \
    zh/short_abstracts_en_uris_zh.nt it/short_abstracts_en_uris_it.nt \
    de/long_abstracts_en_uris_de.nt it/long_abstracts_en_uris_it.nt \
    es/long_abstracts_en_uris_es.nt fr/long_abstracts_en_uris_fr.nt \
    en/skos_categories_en.nt \
    en/article_categories_en.nt \
    )

for i in "${files[@]}"
do
    :
    # clean possible encoding errors
    filename=$(basename $i)
    if [ ! -f ${filename}.gz ]
    then
        url=${DBPEDIA}/${i}.bz2
        wget -c ${url}
        echo "cleaning $filename ..."
        #corrects encoding and recompress using gz
        bzcat ${filename}.bz2 \
            | sed 's/\\\\/\\u005c\\u005c/g;s/\\\([^u"]\)/\\u005c\1/g' \
            | gzip -c > ${filename}.gz
        rm -f ${filename}.bz2
    fi
done

cd ../../..

set +xe

# Instruction to launch the indexing
echo "Preparation & data fetch done: edit config in $WORKSPACE/indexing/config/"
echo "Then launch indexing command:"
echo "(cd $WORKSPACE && java -jar $INDEXING_JAR index)"

