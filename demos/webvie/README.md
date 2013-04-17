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

VIE based Stanbol Enhancer UI
=============================

Adapter to add a VIE based user interface for the Stanbol Enhancer.

Features
--------

 * Text Enhancement visualisation using highlighting
 * Entity Enhancement visualisation using selection
 * HTML Editing and RDFa extension in the browser
 * Selection of enhancement chain using dropdown
 * Using Stanbol's REST endpoints through VIE and VIE's Stanbol service

Supported Browsers
------------------

 * Chrome
 * Firefox
 * Internet Explorer 7+ (6?)
 * Opera
 * Safari

Deploy
------

After building stanbol in a different folder you can build and deploy this bundle by using the following command:

    mvn install -DskipTests -PinstallBundle -Dsling.url=http://localhost:8080/system/console

Components
----------

Main components used for the component:

 * [Annotate.js](https://github.com/szabyg/annotate.js)
 * [VIE](http://viejs.org)
 * [VIE.autocomplete](https://github.com/szabyg/VIE.autocomplete)
 * [VIE.entitypreview](https://github.com/szabyg/vie.entitypreview)

