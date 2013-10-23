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

echo ">> Building incoming links File <<"

WORKSPACE=.
DBPEDIA=http://downloads.dbpedia.org/3.8

MAX_SORT_MEM=4G

# Turn on echoing and exit on error
set -x -e -o pipefail

# The language to build the index
LANGUAGE=$1
INCOMING_FILE=${WORKSPACE}/incoming_links_${LANGUAGE}.txt

#prpair Page_Links
PAGE_LINKS=page_links_${LANGUAGE}.nt
PAGE_LINKS_FILE=${PAGE_LINKS}.gz
if [ ! -f ${PAGE_LINKS_FILE} ]
then
    url=${DBPEDIA}/${LANGUAGE}/${PAGE_LINKS}.bz2
    wget -c ${url}
    echo "cleaning $PAGE_LINKS ..."
    #corrects encoding and recompress using gz
    bzcat ${PAGE_LINKS}.bz2 \
        | sed 's/\\\\/\\u005c\\u005c/g;s/\\\([^u"]\)/\\u005c\1/g' \
        | gzip -c > ${PAGE_LINKS_FILE}
    rm -f ${PAGE_LINKS}.bz2
fi

#prpair Redirects
REDIRECTS=redirects_${LANGUAGE}.nt
REDIRECTS_FILE=${REDIRECTS}.gz

if [ ! -f ${REDIRECTS_FILE} ]
then
    url=${DBPEDIA}/${LANGUAGE}/${REDIRECTS}.bz2
    wget -c ${url}
    echo "cleaning $REDIRECTS ..."
    #corrects encoding and recompress using gz
    bzcat ${REDIRECTS}.bz2 \
        | sed 's/\\\\/\\u005c\\u005c/g;s/\\\([^u"]\)/\\u005c\1/g' \
        | gzip -c > ${REDIRECTS_FILE}
    rm -f ${REDIRECTS}.bz2
fi

zcat ${PAGE_LINKS_FILE} \
| sed -e 's/.*dbpedia\.org\/resource\/\([^>]*\)> ./\1/' \
| sort -S $MAX_SORT_MEM \
| uniq -c  \
| sort -nr -S $MAX_SORT_MEM > $INCOMING_FILE

# Sort the incoming links on the entities, removing initial spaces added by uniq
cat $INCOMING_FILE \
    | sed 's/^\s*//' \
    | sort -k 2b,2 > $WORKSPACE/incoming_links_sorted_k2.txt

mv $INCOMING_FILE $WORKSPACE/original_incoming_links_${LANGUAGE}.txt

# Sort redirects
zcat ${REDIRECTS_FILE} | grep -v "^#" \
    | sed 's/.*dbpedia\.org\/resource\/\([^>]*\)>.*dbpedia\.org\/resource\/\([^>]*\)> ./\1 \2/' \
    | sort -k 2b,2 > $WORKSPACE/redirects_sorted_k2.txt

# Join redirects with the original incoming links to assign the
# same ranking to redirects
join -j 2 -o 2.1 1.1 $WORKSPACE/redirects_sorted_k2.txt $WORKSPACE/incoming_links_sorted_k2.txt \
    > $WORKSPACE/incoming_links_redirects.txt

# Merge the two files - maybe use sort merge?!
cat $WORKSPACE/incoming_links_redirects.txt $WORKSPACE/incoming_links_sorted_k2.txt \
    | sort -nr -S $MAX_SORT_MEM > $INCOMING_FILE

# WE ARE NOT REMOVING INTERMEDIATE FILES
# rm -f $WORKSPACE/incoming_links_sorted_k2.txt
# rm -f $WORKSPACE/redirects_sorted_k2.txt
# rm -f $WORKSPACE/incoming_links_redirects.txt
