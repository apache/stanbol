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

public class NumberAtom extends NumericFunctionAtom {

    private String numberString;

    public NumberAtom(String number) {
        this.numberString = number;
    }

    public String getNumber() {
        return numberString;
    }

    @Override
    public String toString() {

        return numberString;
    }

    @Override
    public String prettyPrint() {
        return numberString;
    }

    public Number getNumberValue() {
        Number number = null;
        if (numberString.contains("\\.")) {
            int index = numberString.lastIndexOf('.');
            if (index + 1 == numberString.length() - 1) {
                number = Float.valueOf(numberString);
            } else {
                number = Double.valueOf(numberString);
            }
        } else {
            number = Integer.valueOf(numberString);
        }

        return number;
    }

}
