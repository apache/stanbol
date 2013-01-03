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


Apache Stanbol Commons Solr extension for paoding-analysis 
=========================================================

This module provides the [paoding-analysis](http://code.google.com/p/paoding/) analyzer for Chinese as bundle extending the default Apache Stanbol Commons Solr core module.

paoding-analysis version
----------------------

This modules includes an fork from revision 154 of the the [paoding-analysis](http://code.google.com/p/paoding/) projects [svn repository](http://paoding.googlecode.com/svn/trunk/).

The last commit to this projects repository was in [Nov 15, 2010](https://code.google.com/p/paoding/source/detail?r=154). The project is under the [Apache License 2.0](http://www.apache.org/licenses/LICENSE-2.0).

Changes to the original paoding source
--------

This provides an overview on all changes made to the paoding source code (anything in the  <code>net.paoding.*</code> packages)

* The Logging framework was changed to SLF4J as this is the one preferable used by Stanbol
* Resources are now loaded via both the ContextClassloader and (if not present) the Classloader used to load the padding classes. Originally the Classloader of the paoding classes was only considered if the ContextClassloader was <code>null</code>
* Environment properties can now also be loaded from Java system properties (e.g. parsed via the -D argument to the JVM). System properties are only considered if no Environment property is defined.


Dictionary
----------

The dictionary is included in the module in an ZIP archive. The <code>org.apache.stanbol.commons.solr.extras.paoding.Activator</code> class can be used to initialize the dictionary. When using this module in OSGI this will be done automatically by the Bundle Activator. Outside of OSGI (e.g. for unit test) this needs to be done manually by calling:

    :::java
    File paodingDict; //the directory for the dict
    if(!paodingDict.isDirectory()){
        Activator.initPaodingDictionary(paodingDict, 
            getClass().getClassLoader().getResourceAsStream(
                Activator.DICT_ARCHIVE));
    }
    Activator.initPaodingDictHomeProperty(paodingDict);

All initialization methods supported by paoding are still supported. In addition the dictionary location can now also be parsed as Java system property (e.g. by adding '-DPAODING_DIC_HOME={path}' when starting the JVM). 

This module also includes an default '<code>paoding-analysis.properties</code>' file that sets the default dictionary path to '.' (the working directory). Users can use their own dictionary file if they place it in the classpath in front of this module. 


