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

echo ">> Building incoming links File for freebase.com <<"

WORKSPACE=indexing/resources

MAX_SORT_MEM=4G

# Turn on echoing and exit on error
set -x -e -o pipefail

INCOMING_FILE=${WORKSPACE}/incoming_links.txt
FB_DUMP=$1

zgrep "^<http://rdf.freebase.com/ns/m\..*<.*>.*<http://rdf.freebase.com/ns/m\." $FB_DUMP\
| cut -f 3 \
| sed 's/.*\/ns\/\(.*\)>/\1/g' \
| sort -S $MAX_SORT_MEM \
| uniq -c \
| sort -nr -S $MAX_SORT_MEM > $INCOMING_FILE

