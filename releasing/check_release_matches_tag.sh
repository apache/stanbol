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

#!/bin/bash
# check that a staged release matches the corresponding svn tags
#
# usage:
#  sh check_release_matches_tag.sh 004 /tmp/PROJECT-staging
#
PROJECT=stanbol
BASE=$2/$1/org/apache/${PROJECT}
TAGBASE=http://svn.apache.org/repos/asf/${PROJECT}/tags

# set this to the command that computes an md5 sum on your system
MD5=md5

function fail() {
	echo $* >&2
	exit 1
}

function check() {
       TAG=$TAGBASE/$1
       ZIP=$PWD/$2
       WORKDIR=workdir/$1/$(date +%s)
       CUR=$PWD
       echo
       echo "Checking $ZIP against $TAG"
       mkdir -p $WORKDIR
       cd $WORKDIR > /dev/null
       unzip $ZIP > /dev/null
       ZIPDIR=$PWD/$(ls)
       svn export $TAG svnexport > /dev/null
       cd svnexport > /dev/null
       diff -r . $ZIPDIR
       cd $CUR

}

CURDIR=`pwd`
cd $BASE || fail "Cannot cd to $BASE"

find . -name *.zip | cut -c 3- | sed 's/\// /g' | while read line
do
       set $line
       TAG=${1}-${2}
       ZIP=${1}/${2}/${3}
       check $TAG $ZIP
done
$MD5 $(find . -name *source-release.zip)
cd $CURDIR
