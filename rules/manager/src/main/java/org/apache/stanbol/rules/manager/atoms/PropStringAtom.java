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

public class PropStringAtom extends StringFunctionAtom {

    private StringFunctionAtom namespaceArg;
    private StringFunctionAtom labelArg;

    public PropStringAtom(StringFunctionAtom namespaceArg, StringFunctionAtom labelArg) {
        this.namespaceArg = namespaceArg;
        this.labelArg = labelArg;
    }

    public StringFunctionAtom getNamespaceArg() {
        return namespaceArg;
    }

    public StringFunctionAtom getLabelArg() {
        return labelArg;
    }

    @Override
    public String toString() {
        String ns = namespaceArg.toString();
        String label = labelArg.toString();
        String kReS = "propString(" + ns + ", " + label + ")";
        return kReS;
    }

    @Override
    public String prettyPrint() {
        return "property string with namespace <" + namespaceArg.prettyPrint() + "> and label "
               + labelArg.prettyPrint();
    }

}
