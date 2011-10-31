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
package org.apache.stanbol.entityhub.indexing.core;

import org.apache.stanbol.entityhub.servicesapi.yard.Yard;


/**
 * Interface that defines the target for indexing. 
 * @author Rupert Westenthaler
 *
 */
public interface IndexingDestination extends IndexingComponent {

    /**
     * Getter for the Yard to store the indexed Entities
     * @return the yard
     */
    Yard getYard();
    
    /**
     * Called after the indexing is completed to allow some post processing and
     * packaging the stored data and writing of the OSGI configuration used to
     * initialise the Yard.
     */
    void finalise();
}
