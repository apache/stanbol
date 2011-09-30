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
package org.apache.stanbol.commons.jsonld;

import java.util.Comparator;

/**
 * A comparator for JSON-LD maps to ensure the order of certain key elements
 * like '#', '@', 'a' in JSON-LD output.
 *
 * @author Fabian Christ
 */
public class JsonComparator implements Comparator<Object> {

    @Override
    public int compare(Object arg0, Object arg1) {
        int value;
        if (arg0.equals(arg1)) {
            value = 0;
        } else if (arg0.equals(JsonLdCommon.CONTEXT)) {
            value = -1;
        } else if (arg1.equals(JsonLdCommon.CONTEXT)) {
            value = 1;
        } else if (arg0.equals(JsonLdCommon.COERCE)) {
            value = 1;
        } else if (arg1.equals(JsonLdCommon.COERCE)) {
            value = -1;
        } else if (arg0.equals(JsonLdCommon.TYPES)) {
            value = 1;
        } else if (arg1.equals(JsonLdCommon.TYPES)) {
            value = -1;
        } else if (arg0.equals(JsonLdCommon.SUBJECT)) {
            value = -1;
        } else if (arg1.equals(JsonLdCommon.SUBJECT)) {
            value = 1;
        } else if (arg0.equals(JsonLdCommon.TYPE)) {
            value = -1;
        } else if (arg1.equals(JsonLdCommon.TYPE)) {
            value = 1;
        } else if (arg0.equals(JsonLdCommon.DATATYPE)) {
            value = 1;
        } else if (arg1.equals(JsonLdCommon.DATATYPE)) {
            value = -1;
        } else if (arg0.equals(JsonLdCommon.LITERAL)) {
            value = 1;
        } else if (arg1.equals(JsonLdCommon.LITERAL)) {
            value = -1;
        } else {
            value = String.valueOf(arg0).toLowerCase().compareTo(String.valueOf(arg1).toLowerCase());
        }

        return value;
    }

}
