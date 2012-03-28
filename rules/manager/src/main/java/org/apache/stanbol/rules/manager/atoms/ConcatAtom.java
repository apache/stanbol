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

/**
 * 
 * Atom for concatenation. Returns the concatenation of the first argument with the secondo argument.
 * 
 * @author anuzzolese
 * 
 */

public class ConcatAtom extends StringFunctionAtom {

    private StringFunctionAtom argument1;
    private StringFunctionAtom argument2;

    public ConcatAtom(StringFunctionAtom argument1, StringFunctionAtom argument2) {
        this.argument1 = argument1;
        this.argument2 = argument2;
    }

    @Override
    public String toString() {

        return "concat(" + argument1.prettyPrint() + ", " + argument2.prettyPrint() + ")";
    }

    @Override
    public String prettyPrint() {
        return "Concatenation of " + argument1.toString() + " with " + argument2.toString();
    }

    public StringFunctionAtom getArgument1() {
        return argument1;
    }

    public StringFunctionAtom getArgument2() {
        return argument2;
    }

}
