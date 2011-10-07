package org.apache.stanbol.reasoners.jobs.impl;

import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.apache.commons.codec.binary.Base64;
import org.apache.stanbol.reasoners.jobs.api.JobManager;
/**
 * Implementation of the {@JobManager} interface.
 * 
 * @author enridaga
 *
 *
 * @scr.component immediate="true"
 * @scr.service
 *
 */
public class JobManagerImpl implements JobManager {
    ExecutorService pool;
    Map<String,Future<?>> taskMap;

    public JobManagerImpl() {
        this.pool = Executors.newCachedThreadPool();
        this.taskMap = new HashMap<String,Future<?>>();
    }

    @Override
    public String execute(Callable<?> callable) {
        String id = JobManagerImpl.buildId(callable);
        Future<?> future = this.pool.submit(callable);
        synchronized (taskMap) {
            taskMap.put(id, future);
            return id;
        }
    }

    @Override
    public Future<?> ping(String id) {
        return taskMap.get(id);
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
            taskMap.get(id).cancel(true);
            taskMap.remove(id);
        }
    }
    

    /**
     * To build a unique string identifier for a background process
     * 
     * @param obj
     * @return
     */
    private static String buildId(Object obj) {
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

}
