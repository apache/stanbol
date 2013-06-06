#!/bin/sh

# Updates the dependencies in all POMs to use the latest
# available Stanbol release artifacts.
# Uses the Maven versions plugin.

mvn versions:use-latest-versions -DexcludeReactor=false -DgenerateBackupPoms=false -DallowSnapshots=true -Dincludes=org.apache.stanbol:*