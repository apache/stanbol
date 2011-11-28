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

This url is defined as property in the 'pom.xml'
The list of downloaded file is defined within the 'download_models.xml'

## NOTE

Using this bundles is only an alternative of manually copying the required OpenNLP models to the '{stanbol-installation}/sling/datafiles'. However note that OpenNLP uses 'se' as prefix for Swedish however 
the official ISO language code is 'sv'! Therefoer the original model files need 
to be renamed from

    se-**
    
to

    sv-**
    
The build process of this bundle does this by default. However when copying
the model files to the '{stanbol-installation}/sling/datafiles' this MUST BE done
manually!
