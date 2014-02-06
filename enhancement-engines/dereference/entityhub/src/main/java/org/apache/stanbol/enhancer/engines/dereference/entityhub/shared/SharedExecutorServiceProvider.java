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

import java.util.concurrent.ExecutorService;

import org.apache.stanbol.enhancer.engines.dereference.entityhub.ExecutorServiceProvider;
import org.osgi.framework.BundleContext;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.util.tracker.ServiceTracker;

/**
 * Utility that wraps a {@link ServiceTracker} to lookup the shared thread
 * pool for dereferencing
 * 
 * @author Rupert Westenthaler
 *
 */
public class SharedExecutorServiceProvider implements ExecutorServiceProvider {

	private ServiceTracker tracker;

	public SharedExecutorServiceProvider(BundleContext context) {
		try {
			this.tracker = new ServiceTracker(context, context.createFilter(
					SharedDereferenceThreadPool.SHARED_THREAD_POOL_FILTER), null);
		} catch (InvalidSyntaxException e) {
			throw new IllegalStateException("Unable to create filter for the " +
					SharedDereferenceThreadPool.class.getSimpleName(), e);
		}
	}
	
	@Override
	public ExecutorService getExecutorService(){
		ExecutorService executorService = (ExecutorService)tracker.getService();
		if(executorService != null && !executorService.isShutdown()){
			return executorService;
		} else {
			return null;
		}
	}
	
	public void close(){
		tracker.close();
	}

	
}
