!--
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

This module builds a runnable Stanbol Enhancer jar using the Sling Launchpad Maven plugin,
including the bundles defined at src/main/bundles/list.xml

To start this after building use:

    java -Xmx1024m -XX:MaxPermSize=256M -jar \
        target/org.apache.stanbol.launchers.full-*-SNAPSHOT.jar

The Stanbol Enhancer HTTP endpoint should then be available at 

    http://localhost:8080

So that you can POST content using, for example:

    curl -H "Content-Type: text/plain" \
        -T ../../enhancer/data/text-examples/obama-signing.txt \
        http://localhost:8080/engines

Configure any required parameter for the enhancement engines, at

  http://localhost:8080/system/console/

The OSGi state is stored in the ./stanbol folder.

The logs are found at stanbol/logs/error.log and can be configured from the
OSGi console.
