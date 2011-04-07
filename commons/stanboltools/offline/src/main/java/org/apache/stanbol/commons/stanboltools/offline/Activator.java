/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with this
 * work for additional information regarding copyright ownership. The ASF
 * licenses this file to You under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package org.apache.stanbol.commons.stanboltools.offline;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Activator for the offline mode bundle
 */
public class Activator implements BundleActivator {

    public static final String OFFLINE_MODE_PROPERTY = "org.apache.stanbol.offline.mode";
    private static final Logger log = LoggerFactory.getLogger(Activator.class);

    private ServiceRegistration serviceReg;

    @Override
    public void start(BundleContext context) throws Exception {
        final String s = System.getProperty(OFFLINE_MODE_PROPERTY);
        Object svc = null;
        String svcName = null;

        if ("true".equals(s)) {
            svc = new OfflineMode() {};
            svcName = OfflineMode.class.getName();
            log.info("OfflineMode activated by {}={}", OFFLINE_MODE_PROPERTY, s);
        } else {
            svc = new OnlineMode() {};
            svcName = OnlineMode.class.getName();
            log.info("Offline mode is not set by {}, OnlineMode activated", OFFLINE_MODE_PROPERTY);
        }
        serviceReg = context.registerService(svcName, svc, null);
    }

    @Override
    public void stop(BundleContext context) throws Exception {
        if (serviceReg != null) {
            serviceReg.unregister();
            serviceReg = null;
        }
    }
}
