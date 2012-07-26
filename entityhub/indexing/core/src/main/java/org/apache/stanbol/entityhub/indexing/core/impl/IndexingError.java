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
/**
 * 
 */
package org.apache.stanbol.entityhub.indexing.core.impl;

public class IndexingError {
    private final Exception ex;
    private final String msg;
    private final String entityId;
    public IndexingError(String id,String msg,Exception ex){
        this.entityId = id;
        this.msg = msg;
        this.ex = ex;
    }
    /**
     * @return the ex
     */
    public Exception getException() {
        return ex;
    }
    /**
     * @return the msg
     */
    public String getMessage() {
        return msg;
    }
    /**
     * @return the entityId
     */
    public String getEntity() {
        return entityId;
    }
}