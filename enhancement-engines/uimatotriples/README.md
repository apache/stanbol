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

# Apache Stanbol UIMA To Triples Enhancement Engine

This turns UIMA annotations into RDF triples by rules defined by the user. This module is only responsible for handling annotations that are coming from UIMA Remote Client or UIMA Local Client Enhancement Engines; It does not create any annotations on its own. Naturally, in the Enhancement Chain's processing order this enhancer should follow annotation source. For details, see: http://blog.iks-project.eu/uima-apache-stanbol-integration-2/