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
package org.apache.stanbol.ontologymanager.store.rest;

import java.util.Hashtable;
import java.util.List;
import java.util.Vector;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Service;
import org.apache.stanbol.ontologymanager.store.api.LockManager;

@Component
@Service
public class LockManagerImp implements LockManager {

    private Lock internalLock = null;
    private ReentrantReadWriteLock globalSpaceLock = null;
    private List<ReentrantReadWriteLock> rwlockList = null;
    private Hashtable<String,ReentrantReadWriteLock> index = null;

    public LockManagerImp() {
        rwlockList = new Vector<ReentrantReadWriteLock>();
        index = new Hashtable<String,ReentrantReadWriteLock>();
        internalLock = new ReentrantLock();
        globalSpaceLock = new ReentrantReadWriteLock();
    }

    private static LockManager instance = null;

    public static LockManager getInstance() {
        if (instance == null) {
            instance = new LockManagerImp();
        }
        return instance;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.apache.stanbol.ontologymanager.store.rest.ILockManager#obtainReadLockFor(java.lang.String)
     */
    public void obtainReadLockFor(String ontologyPath) {
        if (ontologyPath.equalsIgnoreCase(GLOBAL_SPACE)) {
            globalSpaceLock.readLock().lock();
        } else {
            String closureID = getClosureIDFor(ontologyPath);
            internalLock.lock();
            if (!index.containsKey(closureID)) {
                ReentrantReadWriteLock rrwl = new ReentrantReadWriteLock();
                rwlockList.add(rrwl);
                index.put(closureID, rrwl);
            }
            internalLock.unlock();
            ReentrantReadWriteLock rrwl = index.get(closureID);
            globalSpaceLock.readLock().lock();
            rrwl.readLock().lock();
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.apache.stanbol.ontologymanager.store.rest.ILockManager#releaseReadLockFor(java.lang.String)
     */
    public void releaseReadLockFor(String ontologyPath) {
        if (ontologyPath.equalsIgnoreCase(GLOBAL_SPACE)) {
            globalSpaceLock.readLock().unlock();
        } else {
            String closureID = getClosureIDFor(ontologyPath);
            ReentrantReadWriteLock rrwl = index.get(closureID);
            rrwl.readLock().unlock();
            globalSpaceLock.readLock().unlock();
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.apache.stanbol.ontologymanager.store.rest.ILockManager#obtainWriteLockFor(java.lang.String)
     */
    public void obtainWriteLockFor(String ontologyPath) {
        if (ontologyPath.equalsIgnoreCase(GLOBAL_SPACE)) {
            globalSpaceLock.writeLock().lock();
        } else {
            String closureID = getClosureIDFor(ontologyPath);
            internalLock.lock();
            if (!index.containsKey(closureID)) {
                ReentrantReadWriteLock rrwl = new ReentrantReadWriteLock();
                rwlockList.add(rrwl);
                index.put(closureID, rrwl);
            }
            internalLock.unlock();
            ReentrantReadWriteLock rrwl = index.get(closureID);
            globalSpaceLock.readLock().lock();
            rrwl.writeLock().lock();
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.apache.stanbol.ontologymanager.store.rest.ILockManager#releaseWriteLockFor(java.lang.String)
     */
    public void releaseWriteLockFor(String ontologyPath) {
        if (ontologyPath.equalsIgnoreCase(GLOBAL_SPACE)) {
            globalSpaceLock.writeLock().unlock();
        } else {
            String closureID = getClosureIDFor(ontologyPath);
            ReentrantReadWriteLock rrwl = index.get(closureID);
            rrwl.writeLock().unlock();
            globalSpaceLock.readLock().unlock();
        }
    }

    private String getClosureIDFor(String ontologyPath) {
        return ontologyPath;
    }
}
