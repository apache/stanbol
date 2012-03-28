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

public class SubstringAtom extends StringFunctionAtom {

    private StringFunctionAtom stringFunctionAtom;
    private NumericFunctionAtom start;
    private NumericFunctionAtom length;

    public SubstringAtom(StringFunctionAtom stringFunctionAtom,
                         NumericFunctionAtom start,
                         NumericFunctionAtom length) {
        this.stringFunctionAtom = stringFunctionAtom;
        this.start = start;
        this.length = length;
    }

    public StringFunctionAtom getStringFunctionAtom() {
        return stringFunctionAtom;
    }

    public NumericFunctionAtom getStart() {
        return start;
    }

    public NumericFunctionAtom getLength() {
        return length;
    }

    @Override
    public String toString() {
        String string = stringFunctionAtom.toString();
        return "substring(" + string + ", " + start.toString() + ", " + length.toString() + ")";
    }

    @Override
    public String prettyPrint() {
        return "the substring of " + stringFunctionAtom.prettyPrint() + " startihg from the position "
               + start.prettyPrint() + " and for " + length.prettyPrint() + " characters.";
    }

}
