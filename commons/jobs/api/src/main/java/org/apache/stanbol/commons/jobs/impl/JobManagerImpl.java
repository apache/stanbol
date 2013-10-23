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
package org.apache.stanbol.commons.jobs.impl;

import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.apache.commons.codec.binary.Base64;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Service;
import org.apache.stanbol.commons.jobs.api.Job;
import org.apache.stanbol.commons.jobs.api.JobManager;
/**
 * Implementation of the {@JobManager} interface.
 * 
 * @author enridaga
 *
 */
@Component(immediate = true)
@Service(JobManager.class)
public class JobManagerImpl implements JobManager {
    private ExecutorService pool;
    private Map<String,Future<?>> taskMap;
    private Map<String,String> locations;

    public JobManagerImpl() {
        this.pool = Executors.newCachedThreadPool();
        this.taskMap = new HashMap<String,Future<?>>();
        this.locations = new HashMap<String,String>();
    }

    @Override
    public String execute(Job job) {
        String id = JobManagerImpl.buildId(job);
        Future<?> future = this.pool.submit(job);
        synchronized (taskMap) {
            taskMap.put(id, future);
            locations.put(id, job.buildResultLocation(id));
            return id;
        }
    }

    @Override
    public Future<?> ping(String id) {
        synchronized (taskMap) {
            return taskMap.get(id);            
        }
    }


    @Override
    public String getResultLocation(String id) {
        synchronized (locations) {
            return locations.get(id);            
        }
    }

    @Override
    public boolean hasJob(String id) {
        synchronized (taskMap) {
            return taskMap.containsKey(id);
        }
    }

    @Override
    public int size() {
        synchronized (taskMap) {
            return taskMap.size();
        }
    }

    @Override
    public void remove(String id) {
        synchronized (taskMap) {
            // If the job does not exists
            Future<?> f = taskMap.get(id);
            if(f==null) {
                throw new IllegalArgumentException("Job does not exists");
            }
            f.cancel(true);
            taskMap.remove(id);
            synchronized (locations) {
                locations.remove(id);
            }
        }
    }

    /**
     * To build a unique string identifier for a background process
     * 
     * @param obj
     * @return
     */
    public static String buildId(Object obj) {
        String str = obj.toString();
        byte[] thedigest = null;
        try {
            byte[] bytesOfMessage = str.getBytes("UTF-8");
            MessageDigest md = MessageDigest.getInstance("MD5");
            thedigest = md.digest(bytesOfMessage);
        } catch (UnsupportedEncodingException e) {
            // This should never happen
            e.printStackTrace();
        } catch (NoSuchAlgorithmException e) {
            // This should never happen
            e.printStackTrace();
        }
        return Base64.encodeBase64URLSafeString(thedigest);
    }

    /**
     * Removes all jobs
     */
    @Override
    public void removeAll() {
        String[] ids;
        synchronized (taskMap) {
            ids =  taskMap.keySet().toArray(new String[taskMap.keySet().size()]);
        }
        for(String j : ids){
            remove(j);
        }
    }

}
