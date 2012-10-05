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
package org.apache.stanbol.ontologymanager.servicesapi.collector;

/**
 * An object that supports locking mechanisms, thus allowing/preventing modifications of the resources
 * contained therein. Lock management is assumed to occur in methods inherited from implementations.<br>
 * 
 * XXX add public lock handling methods as well?
 * 
 * @author alexdma
 * 
 */
public interface Lockable {

    /**
     * Determines if it is no longer possible to modify this resource until it is torn down.
     * 
     * @return true if this resource is write-locked, false otherwise.
     */
    boolean isLocked();

}
