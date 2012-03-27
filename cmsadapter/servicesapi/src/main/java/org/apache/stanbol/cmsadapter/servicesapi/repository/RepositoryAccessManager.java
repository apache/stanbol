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
package org.apache.stanbol.cmsadapter.servicesapi.repository;

import org.apache.stanbol.cmsadapter.servicesapi.model.web.ConnectionInfo;

/**
 * This class is responsible for retrieving a suitable accessor when a 
 * session or connection description is given.
 * 
 * @author cihan
 * 
 */
public interface RepositoryAccessManager {

    /**
     * 
     * @param connectionInfo
     * @return Any suitable {@link RepositoryAccess} instance that can connect to the
     * CMS repository described in <b>connectionInfo</b> parameter or null if no suitable 
     * accessor can be found. 
     */
    RepositoryAccess getRepositoryAccessor(ConnectionInfo connectionInfo);
    
    /**
     * 
     * @param session
     * @return Any suitable {@link RepositoryAccess} instance that can connect to the
     * CMS repository through session given in  <b>session</b> parameter or null if no suitable 
     * accessor can be found. 
     */
    RepositoryAccess getRepositoryAccess(Object session);

}
