#!/bin/sh

mvn dependency:tree -o | grep SNAPSHOT | sed 's/.* \(.*-SNAPSHOT\).*/\1/' | sort -u