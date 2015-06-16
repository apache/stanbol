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
package org.apache.stanbol.entityhub.indexing.core.impl;

import java.util.concurrent.BlockingQueue;

import org.slf4j.Logger;

public class EntityErrorLoggerDaemon extends IndexingDaemon<IndexingError,Object> {

    private final Logger err;
    public EntityErrorLoggerDaemon(String name, 
            BlockingQueue<QueueItem<IndexingError>> consume, Logger err) {
        super(name, IndexerConstants.SEQUENCE_NUMBER_ERROR_HANDLING_DAEMON,
            consume, null, null);
        this.err = err;
    }

    @Override
    public void run() {
        while(!isQueueFinished()){
            QueueItem<IndexingError> errorItem = consume();
            if(errorItem != null){
                IndexingError error = errorItem.getItem();
                err.error(String.format("Error while indexing %s: %s",
                    error.getEntity(),error.getMessage()),error.getException());
            }
        }
        setFinished();
    }

}
