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
package org.apache.stanbol.rules.manager.arqextention;

import com.hp.hpl.jena.sparql.expr.NodeValue;
import com.hp.hpl.jena.sparql.function.FunctionBase1;

public class CreateStandardLabel extends FunctionBase1 {

    public CreateStandardLabel() {
        super();
    }

    @Override
    public NodeValue exec(NodeValue nodeValue) {
        String value = nodeValue.getString();

        String[] split = value.split("(?=\\p{Upper})");

        int i = 0;

        if (split[i].isEmpty()) {
            i += 1;
        }

        String newString = split[i].substring(0, 1).toUpperCase() + split[i].substring(1, split[i].length());

        if (split.length > 1) {
            for (i += 1; i < split.length; i++) {
                newString += " " + split[i].substring(0, 1).toLowerCase()
                             + split[i].substring(1, split[i].length());
            }
        }

        return NodeValue.makeString(newString);
    }

}
