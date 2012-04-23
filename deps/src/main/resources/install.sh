#!/bin/bash

# Licensed to the Apache Software Foundation (ASF) under one
# or more contributor license agreements.  See the NOTICE file
# distributed with this work for additional information
# regarding copyright ownership.  The ASF licenses this file
# to you under the Apache License, Version 2.0 (the
# "License"); you may not use this file except in compliance
# with the License.  You may obtain a copy of the License at
#
# http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing,
# software distributed under the License is distributed on an
# "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
# KIND, either express or implied.  See the License for the
# specific language governing permissions and limitations
# under the License.

# This script installs the Apache Stanbol dependency JARs
# packaged in this archive in the local Maven repository
# to make them available for the build process of
# Apache Stanbol.

echo "This will install the following artifacts into your local Maven repository:"
echo "  - OWL API version 3.2.3 from http://owlapi.sourceforge.net/"
echo "    License: Apache License, Version 2.0"
echo

read -p "Press any key to continue... CTRL-C to abort." -n 1 -s
echo

echo "Installing OWL API 3.2.3"
exec mvn install:install-file -DpomFile=deps/owlapi-3.2.3/owlapi-3.2.3.pom -Dfile=deps/owlapi-3.2.3/owlapi-bin.jar -Dsources=deps/owlapi-3.2.3/owlapi-src.jar
