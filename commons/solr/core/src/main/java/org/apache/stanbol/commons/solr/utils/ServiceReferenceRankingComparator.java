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
package org.apache.stanbol.commons.solr.utils;

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
    public int compare(ServiceReference ref1, ServiceReference ref2) {
        int r1,r2;
        Object tmp = ref1.getProperty(Constants.SERVICE_RANKING);
        r1 = tmp != null ? ((Integer)tmp).intValue() : 0;
        tmp = ref2.getProperty(Constants.SERVICE_RANKING);
        r2 = tmp != null ? ((Integer)tmp).intValue() : 0;
        if(r1 == r2){
            tmp = ref1.getProperty(Constants.SERVICE_ID);
            long id1 = tmp != null ? ((Long)tmp).longValue() : Long.MAX_VALUE;
            tmp = ref2.getProperty(Constants.SERVICE_ID);
            long id2 = tmp != null ? ((Long)tmp).longValue() : Long.MAX_VALUE;
            //the lowest id must be first -> id1 < id2 -> [id1,id2] -> return -1
            return id1 < id2 ? -1 : id2 == id1 ? 0 : 1; 
        } else {
            //the highest ranking MUST BE first -> r1 < r2 -> [r2,r1] -> return 1
            return r1 < r2 ? 1:-1;
        }
    }

}
