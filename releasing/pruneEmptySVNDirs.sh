#!/bin/sh

for i in `find . -type d -print -name ".svn" | grep -v "/\.svn"`
do
    if [ -z "`ls -A $i | grep -v '\.svn' `" ]
    then
        echo Removing $i
        ls -A $i
        svn remove $i
    fi
done