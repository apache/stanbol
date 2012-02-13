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

Stanbol Reasoners Services Tests
----------------------------------
The tests run automatically during the test phase, start the runnable jar of the kres launcher
and test it over HTTP.

To create or debug tests you can also keep the launcher running and run 
individual tests against it, and use a specific HTTP port, as follows:

  $ cd services-tests
  $ mvn -o clean install -DkeepJarRunning=true -Dhttp.port=8080
  
The launcher starts and keeps running until you press CTRL-C.

In a different console, you can now run individual tests:

  $ mvn -o test -Dtest.server.url=http://localhost:8080 -Dtest=ReasonersConcurrencyTest
  
And add -Dmaven.surefire.debug to enable debugging of the tests.

Or do the same from your IDE, see the POM for system properties that the tests
expect.

You can also run the tests in the same way against any other instance, 
of course.
