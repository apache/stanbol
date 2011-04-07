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
                logger.info("Completed Synchronizing in " + (t2 - t1) + " miliseconds");
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
