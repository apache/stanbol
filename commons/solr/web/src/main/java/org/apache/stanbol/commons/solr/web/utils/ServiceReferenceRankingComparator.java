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
package org.apache.stanbol.commons.solr.web.utils;

import java.util.Comparator;

import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;

/**
 * Compares {@link ServiceReference}s based on the {@link Constants#SERVICE_RANKING}
 * property value. Highest Rank will be listed first.
 */
public class ServiceReferenceRankingComparator implements Comparator<ServiceReference> {
    
    /**
     * Singelton instance
     */
    public static ServiceReferenceRankingComparator INSTANCE = new ServiceReferenceRankingComparator();
    @Override
    public int compare(ServiceReference r1, ServiceReference r2) {
        int ranking1,ranking2;
        Integer tmp = (Integer)r1.getProperty(Constants.SERVICE_RANKING);
        ranking1 = tmp != null ? tmp : 0;
        tmp = (Integer)r2.getProperty(Constants.SERVICE_RANKING);
        ranking2 = tmp != null ? tmp : 0;
        return ranking2-ranking1; //highest rank first
    }

}
