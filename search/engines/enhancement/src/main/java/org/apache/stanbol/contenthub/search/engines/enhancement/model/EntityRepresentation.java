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

package org.apache.stanbol.contenthub.search.engines.enhancement.model;

import java.util.ArrayList;
import java.util.List;

/**
 * 
 * @author cihan
 * 
 */
public class EntityRepresentation {

    private String ref;
    private List<String> types = new ArrayList<String>();
    private double score;

    public EntityRepresentation(String ref) {
        this.ref = ref;
    }

    public String getRef() {
        return ref;
    }

    public void setScore(double score) {
        this.score = score;
    }

    public double getScore() {
        return score;
    }

    public List<String> getTypes() {
        return types;
    }
}
