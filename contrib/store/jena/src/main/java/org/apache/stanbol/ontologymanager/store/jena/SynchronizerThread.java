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
package org.apache.stanbol.ontologymanager.store.jena;

import org.osgi.service.component.ComponentInstance;
import org.apache.stanbol.ontologymanager.store.api.StoreSynchronizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SynchronizerThread extends Thread {

    Logger logger = LoggerFactory.getLogger(SynchronizerThread.class);

    private StoreSynchronizer synchronizer;
    private ComponentInstance instance;
    private Boolean done = false;

    public SynchronizerThread(StoreSynchronizer synchronizer, ComponentInstance instance) {
        this.synchronizer = synchronizer;
        this.instance = instance;
    }

    @Override
    public void run() {
        synchronizer.synchronizeAll(true);
        while (true) {
            synchronized (done) {
                if (done) {
                    return;
                }
                long t1 = System.currentTimeMillis();
                logger.info("Started Synchronizing");
                synchronizer.synchronizeAll(false);
                long t2 = System.currentTimeMillis();
                logger.info("Completed Synchronizing in {} ms", (t2 - t1));
            }
            try {
                Thread.sleep(10000);
            } catch (InterruptedException e) {
                logger.info("Thread Interrupted");
            }

        }
    }

    public void done() {
        logger.info("About to stop synchronizer");
        synchronized (done) {
            this.done = true;
            this.synchronizer.clear();
            logger.info("Stopped synchronizer");
        }
        this.instance.dispose();
    }
}
