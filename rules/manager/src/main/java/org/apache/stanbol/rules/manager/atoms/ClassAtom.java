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

public class ClassAtom extends CoreAtom {

    private IObjectAtom classResource;
    private IObjectAtom argument1;

    public ClassAtom(IObjectAtom classResource, IObjectAtom argument1) {
        this.classResource = classResource;
        this.argument1 = argument1;
    }

    public IObjectAtom getClassResource() {
        return classResource;
    }

    public IObjectAtom getArgument1() {
        return argument1;
    }

    @Override
    public String prettyPrint() {

        return argument1.toString() + " is an individual of the class " + classResource.toString();
    }

    @Override
    public String toString() {
        /*
         * String arg1 = null; String arg2 = null;
         * 
         * 
         * 
         * if(argument1.toString().startsWith(Symbols.variablesPrefix)){ arg1 =
         * "?"+argument1.toString().replace(Symbols.variablesPrefix, ""); VariableAtom variable =
         * (VariableAtom) argument1; if(variable.isNegative()){ arg1 = "notex(" + arg1 + ")"; } } else{ arg1 =
         * argument1.toString(); }
         * 
         * if(classResource.toString().startsWith(Symbols.variablesPrefix)){ arg2 =
         * "?"+classResource.toString().replace(Symbols.variablesPrefix, ""); VariableAtom variable =
         * (VariableAtom) classResource; if(variable.isNegative()){ arg2 = "notex(" + arg2 + ")"; } } else{
         * arg2 = classResource.toString(); }
         */
        return "is(" + classResource.toString() + ", " + argument1.toString() + ")";
    }

}
