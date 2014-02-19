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

# 1. build the indexing tool and copy it to the /data directory

# Such servers are often down - so use a mirror for now.
# Try to use those links of you want to get the newest version

# SIDER_DUMP=http://www4.wiwiss.fu-berlin.de/sider/sider_dump.nt.bz2
# DRUGBANK_DUMP=http://www4.wiwiss.fu-berlin.de/drugbank/drugbank_dump.nt.bz2
# DAILYMED_DUMP=http://www4.wiwiss.fu-berlin.de/dailymed/dailymed_dump.nt.bz2
# DISEASOME_DUMP=http://www4.wiwiss.fu-berlin.de/diseasome/diseasome_dump.nt.bz2

# Mirror hosted by the IKS project (http://www.iks-project.org)
export IKS_MIRROR=http://dev.iks-project.eu/downloads/stanbol-indices/ehealth/source-files/
export SIDER_DUMP=$IKS_MIRROR"sider_dump.nt.bz2"
export DRUGBANK_DUMP=$IKS_MIRROR"drugbank_dump.nt.bz2"
export DAILYMED_DUMP=$IKS_MIRROR"dailymed_dump.nt.bz2"
export DISEASOME_DUMP=$IKS_MIRROR"diseasome_dump.nt.bz2"


if [ ! -f target/indexing ]
then
    mkdir -p target/indexing
fi

if [ ! -f target/indexing/org.apache.stanbol.entityhub.indexing.genericrdf-*-jar-with-dependencies.jar ]
then
    echo "Prepairing Indexing Tool"
    cd ../../entityhub/indexing/genericrdf/
    if [ ! -f target/org.apache.stanbol.entityhub.indexing.genericrdf-*-jar-with-dependencies.jar ]
    then
        mvn assembly:single
    fi
    cd ../../../demos/ehealth/
    cp ../../entityhub/indexing/genericrdf/target/org.apache.stanbol.entityhub.indexing.genericrdf-*-jar-with-dependencies.jar target/indexing/
else
    echo "Indexing Tool present ... skip assembling a new version"
fi

# 2. init the configuration
cd target/indexing/
if [ ! -f indexing ]
then
    echo "Copying Indexing Configuration"
    mkdir -p indexing/config
    cp -R ../../src/main/indexing/config/* indexing/config
    # init missing directories and config files
    java -jar org.apache.stanbol.entityhub.indexing.genericrdf-*-jar-with-dependencies.jar init 
fi

# 3. download the files
cd indexing/resources/
if [ ! -f imported ]
then
    cd rdfdata/
    echo "Downloading RDF dumps"
    if [ ! -f sider_dump.nt.bz2 ]
    then
        echo "Downloading SIDER"
        wget -c $SIDER_DUMP
    fi

    if [ ! -f drugbank_dump.nt.bz2 ]
    then
        echo "Downloading DrugBank"
        wget -c $DRUGBANK_DUMP
    fi

    if [ ! -f dailymed_dump.nt.bz2 ]
    then
        echo "Downloading Dailymed"
        wget -c $DAILYMED_DUMP
    fi

    if [ ! -f diseasome_dump.nt.bz2 ]
    then
        echo "Downloading Diseasome"
        wget -c $DISEASOME_DUMP
    fi
    cd ..
else
    echo "RDF data already imported"
fi
cd ../..

# 3 Now we can start the indexing

java -jar -Xmx1024m -server org.apache.stanbol.entityhub.indexing.genericrdf-*-jar-with-dependencies.jar index

# finally copy the dist to the /target directory
cp -R indexing/dist/* ./..
