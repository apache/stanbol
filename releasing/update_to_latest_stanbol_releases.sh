#!/bin/sh

# Updates the dependencies in all POMs to use the latest
# available Stanbol release artifacts.
# Uses the Maven versions plugin.

mvn versions:use-latest-releases -DexcludeReactor=false -DgenerateBackupPoms=false -Dincludes=org.apache.stanbol:*