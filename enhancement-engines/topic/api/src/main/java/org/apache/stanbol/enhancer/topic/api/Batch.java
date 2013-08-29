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
package org.apache.stanbol.enhancer.topic.api;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Container data transfer object to fetch partial results over a query results one batch at a time.
 * 
 * @param <T>
 *            the type of the items to batch over.
 */
public class Batch<T> implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * Boolean marker set by the backend to tell the caller that it can expect new items by issuing the same
     * query by passing the companion offset marker.
     */
    public final boolean hasMore;

    /**
     * Marker value that the caller can pass to the dataset to fetch the next batch and perform efficient
     * server side batching.
     * 
     * This value should refer to an indexed field with unique values such as a primary key or a random uuid
     * (good for shuffling the example in arbitrary order). The samples return in the batches should be sorted
     * according to this field so that the server can perform efficient range queries that are guaranteed to
     * return no duplicate results across batches.
     */
    public final Object nextOffset;

    public final List<T> items;

    public Batch(List<T> items, boolean hasMore, Object nextOffset) {
        this.items = items;
        this.hasMore = hasMore;
        this.nextOffset = nextOffset;
    }

    /**
     * Helper method to return a first empty batch to bootstrap an iteration loop.
     */
    public static <T2> Batch<T2> emtpyBatch(Class<T2> clazz) {
        return new Batch<T2>(new ArrayList<T2>(), true, null);
    }
}
