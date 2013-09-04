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
Example Apache Stanbol Component
===========

This is an example Apache Stanbol component.

To compile the engine run

    mvn install

To deploy the engine to a stanbol instance running on localhost port 8080 run

    mvn org.apache.sling:maven-sling-plugin:install


After installing a new menu item pointing you to /${artifactId} will appear.

The example service allows to upload a file for which enhancement will be generated.
The service can be accessed via browser as HTML or as RDF for machine clients.
