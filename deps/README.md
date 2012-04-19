<!--
  Licensed to the Apache Software Foundation (ASF) under one or more
  contributor license agreements.  See the NOTICE file distributed with
  this work for additional information regarding copyright ownership.
  The ASF licenses this file to You under the Apache License, Version 2.0
  (the "License"); you may not use this file except in compliance with
  the License.  You may obtain a copy of the License at

      http://www.apache.org/licenses/LICENSE-2.0

  Unless required by applicable law or agreed to in writing, software
  distributed under the License is distributed on an "AS IS" BASIS,
  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  See the License for the specific language governing permissions and
  limitations under the License.
-->

# Apache Stanbol Dependencies

Apache Stanbol has dependencies to third party libraries that are not
available via the Maven central repository, yet. Therefore, you have to
install them manually into your local Maven repository.

Third party dependencies which are not available via Maven central:

  - OWL API version 3.2.3 from http://owlapi.sourceforge.net/
    License: Apache License, Version 2.0

This project can be used to generate a -deps package that contains the
listed dependencies including scripts to install them in a local Maven
repository.

The dependencies will be downloaded automatically from their project
websites and re-packaged into an Apache Stanbol -deps package.

To generate the -deps package use

    $ mvn package

You will find the -deps package as ZIP and TAR.GZ in the target folder.
