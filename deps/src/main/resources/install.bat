@echo off

REM Licensed to the Apache Software Foundation (ASF) under one
REM or more contributor license agreements. See the NOTICE file
REM distributed with this work for additional information
REM regarding copyright ownership. The ASF licenses this file
REM to you under the Apache License, Version 2.0 (the
REM "License"); you may not use this file except in compliance
REM with the License. You may obtain a copy of the License at
REM
REM http:\\www.apache.org\licenses\LICENSE-2.0
REM
REM Unless required by applicable law or agreed to in writing,
REM software distributed under the License is distributed on an
REM "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
REM KIND, either express or implied. See the License for the
REM specific language governing permissions and limitations
REM under the License.

REM This script installs the Apache Stanbol dependency JARs
REM packaged in this archive in the local Maven repository
REM to make them available for the build process of
REM Apache Stanbol.

echo This will install the following artifacts into your local Maven repository:
echo   - OWL API version 3.2.3 from http:\\owlapi.sourceforge.net\
echo     License: Apache License, Version 2.0
echo.

echo Press any key to continue... CTRL-C to abort.
echo.

pause

echo Installing OWL API 3.2.3
call mvn install:install-file -DpomFile=deps\owlapi-3.2.3\owlapi-3.2.3.pom -Dfile=deps\owlapi-3.2.3\owlapi-bin.jar -Dsources=deps\owlapi-3.2.3\owlapi-src.jar
