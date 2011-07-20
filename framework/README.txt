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

This module builds a runnable Stanbol Framework jar using the Sling Launchpad
Maven plugin, including the bundles defined at src/main/bundles/list.xml.

Note: This launcher only includes the Stanbol Framework without any enhancement
engines or data models to be used by the EntityHub. This means that this
launcher will not be very useable if you expect an out-of-the-box ready-to-use
Stanbol launcher. This launcher is mainly meant as the minimal starting point
if you would like to create your own Stanbol infrastructure based on the
Stanbol Framework.

To start this after building use:

  java -Xmx512M -jar target/org.apache.stanbol.launchers.framework-0.9.0-incubating-SNAPSHOT.jar

The Stanbol Framework HTTP endpoint should then be available at 

  http://localhost:8080


Configure Stanbol at

  http://localhost:8080/system/console/

The OSGi state is stored in the ./sling folder.

The logs are found at sling/logs/error.log and can be configured from the
OSGi console.
