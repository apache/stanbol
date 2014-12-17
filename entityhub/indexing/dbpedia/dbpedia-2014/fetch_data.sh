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

DBPEDIA=http://data.dws.informatik.uni-mannheim.de/dbpedia/2014

# Turn on echoing and exit on error
set -x -e -o pipefail

# General attributes for all entities
langs=( \
    ar \
    da \
    de \
    es \
    fi \
    fr \
    he \
    hr \
    hi \
    hu \
    it \
    ja \
    nl \
    no \
    pl \
    pt \
    ru \
    sv \
    tr \
    zh \
    )

files_lang=( \
    %LANG%/labels_%LANG%.ttl \
    %LANG%/labels_en_uris_%LANG%.ttl \
    %LANG%/long_abstracts_%LANG%.ttl \
    %LANG%/long_abstracts_en_uris_%LANG%.ttl \
    %LANG%/short_abstracts_%LANG%.ttl \
    %LANG%/short_abstracts_en_uris_%LANG%.ttl \
    %LANG%/instance_types_%LANG%.ttl \
    %LANG%/images_%LANG%.ttl \
    %LANG%/geo_coordinates_%LANG%.ttl \
    %LANG%/mappingbased_properties_%LANG%.ttl \
    %LANG%/homepages_en_uris_%LANG%.ttl \
    %LANG%/homepages_%LANG%.ttl \
    %LANG%/raw_infobox_properties_%LANG%.ttl \
    %LANG%/article_categories_%LANG%.ttl \
    %LANG%/article_categories_en_uris_%LANG%.ttl \
    %LANG%/skos_categories_%LANG%.ttl \
    %LANG%/skos_categories_en_uris_%LANG%.ttl \
    )

files=( \
    en/labels_en.ttl \
    en/instance_types_en.ttl \
    en/images_en.ttl \
    en/geo_coordinates_en.ttl \
    en/mappingbased_properties_en.ttl \
    en/long_abstracts_en.ttl \
    en/short_abstracts_en.ttl \
    en/homepages_en.ttl \
    en/raw_infobox_properties_en.ttl \
    en/article_categories_en.ttl \
    en/skos_categories_en.ttl \

    links/yago_types.ttl \
    links/yago_type_links.ttl \
    links/yago_taxonomy.ttl \
    links/yago_links.ttl \
    links/freebase_links.nt \
    links/geonames_links.ttl \
    links/musicbrainz_links.nt \
    links/openei_links.nt \
    links/nytimes_links.nt \
    links/factbook_links.nt \
    links/eurostat_wbsg_links.nt \
    links/eurostat_linkedstatistics_links.nt \
    links/gutenberg_links.nt 
    
    )

rm -f urls.txt
touch urls.txt

for lang in "${langs[@]}"; do
    for i in "${files_lang[@]}"; do
	f="${i//'%LANG%'/$lang}"
	echo ${DBPEDIA}/$f.bz2 >> urls.txt
    done
done
for f in "${files[@]}"; do
    echo ${DBPEDIA}/$f.bz2 >> urls.txt
done

wget -c -i urls.txt

set +xe

