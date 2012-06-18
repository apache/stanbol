#!/bin/bash
# check that a staged release matches the corresponding svn tags
#
# usage:
#  sh check_release_matches_tag.sh 004 /tmp/PROJECT-staging
#
PROJECT=stanbol
BASE=$2/$1/org/apache/${PROJECT}
TAGBASE=http://svn.apache.org/repos/asf/incubator/${PROJECT}/tags/

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
md5 $(find . -name *source-release.zip)
cd $CURDIR
