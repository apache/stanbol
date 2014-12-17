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

DBpedia 2014
============

This folder contains work-in-progress scrips for downloading dbpedia dump files.

fetch_data.sh
-------------

This script fetches all data from the [DBpedia download server](http://data.dws.informatik.uni-mannheim.de/dbpedia/2014/).
The list of the downloaded files is specified in an array at the begin of the 
script, templatized by language. Users may need to edit this list based on their 
demands.

