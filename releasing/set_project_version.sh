#!/bin/sh

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

# Recursively sets the project version to the version given by
# the first parameter.

CURDIR=`pwd`

USAGE="Sets the Maven project version using the Maven versions plugin."
USAGE="${USAGE}\n Usage: `basename ${0}` [-r] <new version>"
USAGE="${USAGE}\n Options: \t -r \t recursively process sub directories"
USAGE="${USAGE}\n          \t -d \t update dependencies in other POMs to use the new version"
TOOLVERSION=1

while getopts hvrd OPT; do
  case "$OPT" in
    h)
      echo $USAGE
      exit 0
      ;;
    v)
      echo "`basename $0` version ${TOOLVERSION}"
      exit 0
      ;;
    r)
      RECURSIVE=1
      echo "- Running recursively"
      ;;
    d)
      PROCESSDEPS="true"
      echo "- Processing dependencies"
      ;;
    \?)
      # getopts issues an error message
      echo $USAGE >&2
      exit 1
      ;;
  esac
done

shift `expr $OPTIND - 1`

VERSION=${1}

if [ -z "${VERSION}" ]
then
 echo $USAGE
 exit
fi

if [ ! -n "${PROCESSDEPS}" ]
then
  PROCESSDEPS="false"
fi

if [ -e "pom.xml" ]
then
  echo "Setting project version to ${VERSION} in `pwd`"
  mvn versions:set -DnewVersion=${VERSION} -DgenerateBackupPoms=false -DprocessDependencies="${PROCESSDEPS}"
fi

if [ -n "${RECURSIVE}" ]
then
  for i in `find . -name pom.xml | sed 's/\/pom.xml//' | sed 's/\.//'`
  do
    cd ${CURDIR}${i}
    echo "Setting project version to ${VERSION} in `pwd`"
    mvn versions:set -DnewVersion=${VERSION} -DgenerateBackupPoms=false -DprocessDependencies="${PROCESSDEPS}"
  done
fi

cd ${CURDIR}
echo "Finished."
