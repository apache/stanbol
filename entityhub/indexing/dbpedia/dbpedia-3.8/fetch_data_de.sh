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
    de/labels_de.nt \
    en/short_abstracts_en.nt \
    de/short_abstracts_de.nt \
    en/instance_types_en.nt \
    de/instance_types_de.nt \
    en/images_en.nt \
    de/images_de.nt \
    en/geo_coordinates_en.nt \
    de/geo_coordinates_de.nt \
    en/redirects_en.nt \
    de/redirects_de.nt \
    de/mappingbased_properties_de.nt \
    de/article_categories_de.nt \
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

