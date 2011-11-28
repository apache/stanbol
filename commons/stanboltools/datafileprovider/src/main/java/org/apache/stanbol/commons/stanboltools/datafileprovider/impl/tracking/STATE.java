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
package org.apache.stanbol.commons.stanboltools.datafileprovider.impl.tracking;

/**
 * The state of a DataFile. UNKNOWN indicates that this DataFile was
 * never tracked before.
 * @author Rupert Westenthaler
 *
 */
public enum STATE {
    /**
     * never checked
     */
    UNKNOWN,
    /**
     * not available on the last check
     */
    UNAVAILABLE,
    /**
     * available on the last check
     */
    AVAILABLE, 
    /**
     * Indicates that an ERROR was encountered while notifying an change
     * in the Event state
     */
    ERROR
}