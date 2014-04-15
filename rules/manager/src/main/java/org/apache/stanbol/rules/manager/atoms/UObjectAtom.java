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

import org.apache.stanbol.rules.base.api.Symbols;

public class UObjectAtom extends StringFunctionAtom {

    public static final int STRING_TYPE = 0;
    public static final int INTEGER_TYPE = 1;
    public static final int VARIABLE_TYPE = 2;

    private Object argument;
    private int actualType;

    public UObjectAtom(Object argument) {
        this.argument = argument;

        if (argument instanceof VariableAtom) {
            actualType = 2;
        } else if (argument instanceof String) {
            actualType = 0;
        } else if (argument instanceof Integer) {
            actualType = 1;
        }
    }

    public int getActualType() {
        return actualType;
    }

    public Object getArgument() {
        return argument;
    }

    @Override
    public String toString() {
        String argumentString = null;
        switch (actualType) {
            case 0:
                argumentString = (String) argument;
                break;
            case 1:
                argumentString = argument.toString();
                break;
            case 2:
                argumentString = "?" + argument.toString().replace(Symbols.variablesPrefix, "");
                break;
            default:
                break;
        }
        return argumentString;
    }

    @Override
    public String prettyPrint() {
        return argument.toString();
    }

}
