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
package org.apache.stanbol.contenthub.index;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Service;
import org.apache.stanbol.commons.semanticindex.index.IndexManagementException;
import org.apache.stanbol.commons.semanticindex.index.SemanticIndex;
import org.apache.stanbol.commons.semanticindex.index.SemanticIndexManager;
import org.apache.stanbol.commons.solr.utils.ServiceReferenceRankingComparator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentContext;

/**
 * Default implementation for {@link SemanticIndexManager}
 * 
 * @author suat
 * 
 */
@Component(immediate = true)
@Service
public class SemanticIndexManagerImpl implements SemanticIndexManager {
    private ComponentContext componentContext;

    @Activate
    protected void activate(ComponentContext componentContext) {
        this.componentContext = componentContext;
    }

    @Override
    public SemanticIndex<?> getIndex(String name) throws IndexManagementException {
        return getIndex(name, null);
    }

    @Override
    public List<SemanticIndex<?>> getIndexes(String name) throws IndexManagementException {
        return getIndexes(name, null);
    }

    @Override
    public SemanticIndex<?> getIndexByEndpointType(String endpointType) throws IndexManagementException {
        return getIndex(null, endpointType);
    }

    @Override
    public List<SemanticIndex<?>> getIndexesByEndpointType(String endpointType) throws IndexManagementException {
        return getIndexes(null, endpointType);
    }

    @Override
    public SemanticIndex<?> getIndex(String name, String endpointType) throws IndexManagementException {
        List<SemanticIndex<?>> result = getIndexList(name, endpointType, false);
        if (result.size() == 0) {
            return null;
        } else {
            return result.get(0);
        }

    }

    @Override
    public List<SemanticIndex<?>> getIndexes(String name, String endpointType) throws IndexManagementException {
        return getIndexList(name, endpointType, true);
    }

    private List<SemanticIndex<?>> getIndexList(String name, String endpointType, boolean multiple) {
        BundleContext bundleContext = componentContext.getBundleContext();
        List<SemanticIndex<?>> results = new ArrayList<SemanticIndex<?>>();
        try {
            ServiceReference[] refs = bundleContext.getServiceReferences(SemanticIndex.class.getName(), null);
            if (refs != null) {
                if (refs.length > 1) {
                    // TODO: rw move the ServiceReferenceRankingComperator to a utils module
                    Arrays.sort(refs, ServiceReferenceRankingComparator.INSTANCE);
                }
                for (ServiceReference ref : refs) {
                    SemanticIndex<?> si = (SemanticIndex<?>) bundleContext.getService(ref);
                    if (name == null || name.equals(si.getName())) {
                        if (endpointType == null) {
                            results.add(si);
                        } else {
                            // search both the RESTful and the JAVA interfaces
                            Set<String> endpointTypes = si.getRESTSearchEndpoints().keySet();
                            if (endpointTypes != null && endpointTypes.contains(endpointType)) {
                                results.add(si);
                            } else {
                                endpointTypes = si.getSearchEndPoints().keySet();
                                if (endpointTypes != null && endpointTypes.contains(endpointType)) {
                                    results.add(si);
                                } else { // service does not match requirements -> unget the service
                                    bundleContext.ungetService(ref);
                                }
                            }
                        }
                    }
                    if (multiple == false && results.size() == 1) {
                        break;
                    }
                }
            }
        } catch (InvalidSyntaxException e) {
            // ignore as there is no filter
        }
        return results;
    }
}
