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
package org.apache.stanbol.cmsadapter.servicesapi.model.web.decorated;


public enum AdapterMode {

    /**
     * In <b>ONLINE</b> mode all the requests that require repository access is expected to successfully connect to
     * repository. Underlying objects from package {@linkplain org.apache.stanbol.cmsadapter.servicesapi.model.web}
     * are not used for accessing.
     */
    ONLINE,
    /**
     * In <b>TOLERATED OFFLINE</b> mode, a repository connection error will not be thrown instead 
     * existing underlying objects from package {@linkplain org.apache.stanbol.cmsadapter.servicesapi.model.web}
     * will be used.
     */
    TOLERATED_OFFLINE,
    /**
     * In <b>STRICT OFFLINE</b> repository is never accessed. All the information is expected to provided by
     * underlying objects from package {@linkplain org.apache.stanbol.cmsadapter.servicesapi.model.web}.
     */
    STRICT_OFFLINE

}