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
package org.apache.stanbol.enhancer.engines.dereference.entityhub.shared;

import java.util.Dictionary;
import java.util.Hashtable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.ConfigurationPolicy;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Property;
import org.osgi.framework.Constants;
import org.osgi.framework.Filter;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.util.concurrent.ThreadFactoryBuilder;

/**
 * Component that registers a Thread
 * @author westei
 *
 */
@Component(
	    configurationFactory = false, //only a single instance 
	    policy = ConfigurationPolicy.OPTIONAL, // the baseUri is required!
	    specVersion = "1.1", 
	    metatype = true, 
	    immediate = true)
public class SharedDereferenceThreadPool {
	
	private final Logger log = LoggerFactory.getLogger(SharedDereferenceThreadPool.class);

	@Property(intValue=SharedDereferenceThreadPool.DEFAULT_SHARED_THREAD_POOL_SIZE)
	public static final String SHARED_THREAD_POOL_SIZE = "enhancer.engines.dereference.entityhub.sharedthreadpool.size";
	public static final int DEFAULT_SHARED_THREAD_POOL_SIZE = 8;
	
	/**
	 * used as key for {@link Filter}s to track the shared thread pool
	 */
	static final String SHARED_THREAD_POOL_NAME = "enhancer.engines.dereference.entityhub.sharedthreadpool.name";
	/**
	 * used as value for {@link Filter}s to track the shared thread pool
	 */
	static final String DEFAULT_SHARED_THREAD_POOL_NAME = "shared";
	
	/**
	 * {@link Filter} string for tracking the {@link ExecutorService} registered
	 * by this component as OSGI service
	 */
	public static String SHARED_THREAD_POOL_FILTER = String.format(
			"(&(%s=%s)(%s=%s))",
			Constants.OBJECTCLASS,ExecutorService.class.getName(),
			SHARED_THREAD_POOL_NAME, DEFAULT_SHARED_THREAD_POOL_NAME);

	private ServiceRegistration serviceRegistration;

	private ExecutorService executorService;
	
	@Activate
	protected void activate(ComponentContext ctx) throws ConfigurationException {
		log.info("activate {}",getClass().getSimpleName());
		Object value = ctx.getProperties().get(SHARED_THREAD_POOL_SIZE);
		int poolSize;
		if(value == null){
			poolSize = DEFAULT_SHARED_THREAD_POOL_SIZE;
		} else if(value instanceof Number){
			poolSize = ((Number)value).intValue();
		} else {
			try {
				poolSize = Integer.parseInt(value.toString());
			} catch (NumberFormatException e){
        		throw new ConfigurationException(SHARED_THREAD_POOL_SIZE, "Value '" + value
        				+ "'(type: "+value.getClass().getName()+") can not be parsed "
        				+ "as Integer");
			}
		}
		if(poolSize == 0){
			poolSize = DEFAULT_SHARED_THREAD_POOL_SIZE;
		}
		if(poolSize < 0){
			log.info("{} is deactivated because configured thread pool size < 0",
					getClass().getSimpleName());
		} else {
            String namePattern = getClass().getSimpleName()+"-thread-%s";
            ThreadFactory threadFactory = new ThreadFactoryBuilder()
                .setNameFormat(namePattern)
                .setDaemon(true).build();
            log.debug(" - create Threadpool(namePattern='{}' | size='{}')",
                namePattern,poolSize);
            executorService = Executors.newFixedThreadPool(poolSize, threadFactory);
            Dictionary<String, Object> dict = new Hashtable<String, Object>();
            dict.put(SHARED_THREAD_POOL_SIZE, poolSize);
            dict.put(SHARED_THREAD_POOL_NAME, DEFAULT_SHARED_THREAD_POOL_NAME);
            log.debug(" - register ExecutorService");
			serviceRegistration = ctx.getBundleContext().registerService(
					ExecutorService.class.getName(), executorService, dict);
		}
	}
	
	@Deactivate
	protected void deactivate(ComponentContext ctx){
		log.info("deactivate {}", getClass().getSimpleName());
		if(serviceRegistration != null){
			serviceRegistration.unregister();
		}
		if(executorService != null){
			log.info(" ... shutdown ExecutorService");
			executorService.shutdown();
			executorService = null;
		}
	}
	
}
