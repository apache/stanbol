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

public class IndividualPropertyAtom extends CoreAtom {

    private IObjectAtom objectProperty;
    private IObjectAtom argument1;
    private IObjectAtom argument2;

    public IndividualPropertyAtom(IObjectAtom objectProperty, IObjectAtom argument1, IObjectAtom argument2) {
        this.objectProperty = objectProperty;
        this.argument1 = argument1;
        this.argument2 = argument2;
    }

    public IObjectAtom getObjectProperty() {
        return objectProperty;
    }

    public IObjectAtom getArgument1() {
        return argument1;
    }

    public IObjectAtom getArgument2() {
        return argument2;
    }

    @Override
    public String prettyPrint() {
        return "Individual " + argument1.toString() + " has object property " + argument1.toString()
               + " that refers to individual " + argument2.toString();
    }

    @Override
    public String toString() {

        return "has(" + objectProperty.toString() + ", " + argument1.toString() + ", " + argument2.toString()
               + ")";

        /*
         * String arg1 = null; String arg2 = null; String arg3 = null;
         * 
         * if (argument1.toString().startsWith(Symbols.variablesPrefix)) { arg1 = "?" +
         * argument1.toString().replace(Symbols.variablesPrefix, ""); VariableAtom variable = (VariableAtom)
         * argument1; if (variable.isNegative()) { arg1 = "notex(" + arg1 + ")"; } } else { arg1 =
         * argument1.toString(); }
         * 
         * if (objectProperty.toString().startsWith(Symbols.variablesPrefix)) { arg3 = "?" +
         * objectProperty.toString().replace(Symbols.variablesPrefix, ""); VariableAtom variable =
         * (VariableAtom) objectProperty; if (variable.isNegative()) { arg3 = "notex(" + arg3 + ")"; } } else
         * { arg3 = objectProperty.toString(); }
         * 
         * if (argument2.toString().startsWith(Symbols.variablesPrefix)) { arg2 = "?" +
         * argument2.toString().replace(Symbols.variablesPrefix, ""); VariableAtom variable = (VariableAtom)
         * argument2; if (variable.isNegative()) { arg2 = "notex(" + arg2 + ")"; } } else { arg2 =
         * argument2.toString(); }
         * 
         * return "has(" + arg3 + ", " + arg1 + ", " + arg2 + ")";
         */

    }

}
