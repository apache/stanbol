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
package org.apache.stanbol.explanation.impl;

import java.util.Collection;

import org.apache.stanbol.explanation.api.Explainable;
import org.apache.stanbol.explanation.api.Explanation;
import org.apache.stanbol.explanation.api.ExplanationTypes;

public class ExplanationImpl implements Explanation {

    Explainable<?> object;

    ExplanationTypes type;

    public ExplanationImpl(Explainable<?> explainable, ExplanationTypes type) {
        this.object = explainable;
        this.type = type;
    }

    @Override
    public Explainable<?> getObject() {
        return object;
    }

    @Override
    public Collection<?> getGrounding() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public ExplanationTypes getType() {
        return type;
    }

}
