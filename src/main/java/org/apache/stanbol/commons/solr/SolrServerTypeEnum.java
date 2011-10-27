/*
* Licensed to the Apache Software Foundation (ASF) under one or more
* contributor license agreements.  See the NOTICE file distributed with
* this work for additional information regarding copyright ownership.
* The ASF licenses this file to You under the Apache License, Version 2.0
* (the "License"); you may not use this file except in compliance with
* the License.  You may obtain a copy of the License at
*
*     http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/
package org.apache.stanbol.commons.solr;

/**
 * SolrServer types defined here to avoid java dependencies to the according java classes
 * 
 * @author Rupert Westenthaler
 * 
 */
public enum SolrServerTypeEnum {
    /**
     * Uses an embedded SolrServer that runs within the same virtual machine
     */
    EMBEDDED,
    /**
     * The default type that can be used for query and updates
     */
    HTTP,
    /**
     * This server is preferable used for updates
     */
    STREAMING,
    /**
     * This allows to use load balancing on multiple SolrServers via a round robin algorithm.
     */
    LOAD_BALANCE
}