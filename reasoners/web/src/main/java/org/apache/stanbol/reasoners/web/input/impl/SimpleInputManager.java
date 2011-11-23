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
package org.apache.stanbol.reasoners.web.input.impl;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import org.apache.stanbol.reasoners.servicesapi.ReasoningServiceInputManager;
import org.apache.stanbol.reasoners.servicesapi.ReasoningServiceInputProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SimpleInputManager implements ReasoningServiceInputManager {

    private Logger logger = LoggerFactory.getLogger(SimpleInputManager.class);

    private List<ReasoningServiceInputProvider> providers;

    public SimpleInputManager() {
        this.providers = new ArrayList<ReasoningServiceInputProvider>();
    }

    @Override
    public void addInputProvider(ReasoningServiceInputProvider provider) {
        providers.add(provider);
    }

    @Override
    public void removeInputProvider(ReasoningServiceInputProvider provider) {
        providers.remove(provider);
    }

    @Override
    public <T> Iterator<T> getInputData(final Class<T> type) {
        final List<ReasoningServiceInputProvider> fProviders = getProviders();
        return new Iterator<T>() {
            private Iterator<T> current = null;
            private Iterator<ReasoningServiceInputProvider> pIterator = fProviders.iterator();

            /**
             * Set the next provider as the current one. Returns true if a non-empty iterator have been set in
             * the current variable, false if no (more) providers are available.
             * 
             * @return
             */
            private boolean nextProvider() {
                if (pIterator.hasNext()) {
                    ReasoningServiceInputProvider provider = pIterator.next();
                    if (provider.adaptTo(type)) {
                        // If this provider can adapt
                        try {
                            current = provider.getInput(type);
                        } catch (IOException e) {
                            // This is bad, but we can go on to the next :)
                            logger.error("Cannot get input from provider", e);
                            return nextProvider();
                        }
                        // If is empty, try the next
                        if (current.hasNext() == false) {
                            return nextProvider();
                        } else {
                            return true;
                        }
                    } else {
                        // If this provider cannot adapt, try the next
                        return nextProvider();
                    }
                } else {
                    // No providers anymore
                    return false;
                }
            }

            @Override
            public boolean hasNext() {
                if (current == null) {
                    // initialize the iterator
                    if (nextProvider()) {
                        return current.hasNext();
                    } else {
                        // No provider available, iterator is empty
                        return false;
                    }
                } else if (current.hasNext()) {
                    return true;
                } else {
                    // If the current iterator has finished, try the next
                    if (nextProvider()) {
                        return true;
                    }
                }
                return false;
            }

            @Override
            public T next() {
                if (current == null) {
                    // initialize the iterator
                    if (nextProvider()) {
                        return current.next();
                    } else {
                        throw new IllegalStateException("Iterator has no more items");
                    }
                }else{
                    if(current.hasNext()){
                        return current.next();
                    }else{
                        // This has finished, try the next (if any)
                        if (nextProvider()) {
                            return current.next();
                        } else {
                            throw new IllegalStateException("Iterator has no more items");
                        }
                    }
                }
            }

            @Override
            public void remove() {
                throw new UnsupportedOperationException();
            }

        };
    }

    @Override
    public List<ReasoningServiceInputProvider> getProviders() {
        return Collections.unmodifiableList(providers);
    }

}
