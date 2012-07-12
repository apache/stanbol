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

# Data files Bundles for OpenNLP

This source repository only holds the pom.xml file and folder structure of this bundle.

To avoid loading subversion repository with large binary files this artifact has to be build and deployed manually to retrieve precomputed models from other sites.


## Downloading the OpenNLP statistical model 

The OpenNLP models are downloaded from 

    http://opennlp.sourceforge.net/models-1.5
    https://github.com/utcompling/OpenNLP-Models

This url is defined as property in the 'pom.xml'
The list of downloaded file is defined within the 'download_models.xml'

## NOTES

* Using this bundles is only an alternative of manually copying the required OpenNLP models to the '{stanbol-installation}/stanbol/datafiles'.
* This uses the Sentence detector for Portuguese as their is no one available for Spanish
* The POS model for Spanish is downloaded form github
