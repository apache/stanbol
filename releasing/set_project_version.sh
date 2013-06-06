#!/bin/sh

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