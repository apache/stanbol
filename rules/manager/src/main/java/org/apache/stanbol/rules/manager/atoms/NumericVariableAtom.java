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

package org.apache.stanbol.rules.manager.atoms;

import java.net.URI;

import org.apache.stanbol.rules.base.api.Symbols;

public class NumericVariableAtom extends NumericFunctionAtom {

    private URI uri;
    private boolean negative;

    public NumericVariableAtom(URI uri, boolean negative) {
        this.uri = uri;
        this.negative = negative;
    }

    public URI getURI() {
        return uri;
    }

    public boolean isNegative() {
        return negative;
    }

    @Override
    public String toString() {
        return "?" + getVariableName();
    }

    @Override
    public String prettyPrint() {
        return "variable " + uri.toString();
    }

    public String getVariableName() {
        String uriString = uri.toString();

        uriString = uriString.replace(Symbols.variablesPrefix, "");

        return uriString;
    }

}
