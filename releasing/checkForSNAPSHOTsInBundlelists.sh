#!/bin/sh

# Count number of bundlelist 'list.xml' files with SNAPSHOT deps
find . -name list.xml -exec grep -l SNAPSHOT {} \; | wc -l