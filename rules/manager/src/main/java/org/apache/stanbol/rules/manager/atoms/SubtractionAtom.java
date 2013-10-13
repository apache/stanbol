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

public class SubtractionAtom extends NumericFunctionAtom {

    private NumericFunctionAtom numericFunctionAtom1;
    private NumericFunctionAtom numericFunctionAtom2;

    public SubtractionAtom(NumericFunctionAtom numericFunctionAtom1, NumericFunctionAtom numericFunctionAtom2) {
        this.numericFunctionAtom1 = numericFunctionAtom1;
        this.numericFunctionAtom2 = numericFunctionAtom2;
    }

    public NumericFunctionAtom getNumericFunctionAtom1() {
        return numericFunctionAtom1;
    }

    public NumericFunctionAtom getNumericFunctionAtom2() {
        return numericFunctionAtom2;
    }

    @Override
    public String toString() {
        String kReSFunction1 = numericFunctionAtom1.toString();
        String kReSFunction2 = numericFunctionAtom2.toString();
        return "sub(" + kReSFunction1 + ", " + kReSFunction2 + ")";
    }

    @Override
    public String prettyPrint() {
        return numericFunctionAtom1.prettyPrint() + "-" + numericFunctionAtom2.prettyPrint();
    }

}
