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
package org.apache.stanbol.enhancer.topic;

/**
 * Data transfer object to report estimated classification performance of a classifier.
 * 
 * TODO: explain the metrics and give links to wikipedia
 */
public class ClassificationPerformance {

    public final float precision;

    public final float recall;

    public final float f1;

    // TODO: include ids of badly classified positive and negative examples?

    public ClassificationPerformance(float precision, float recall, float f1) {
        this.precision = precision;
        this.recall = recall;
        this.f1 = f1;
    }

}
