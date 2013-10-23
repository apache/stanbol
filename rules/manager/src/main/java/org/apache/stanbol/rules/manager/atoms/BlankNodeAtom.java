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

import org.apache.stanbol.rules.base.api.URIResource;

public class BlankNodeAtom extends CoreAtom {

    private IObjectAtom argument1;
    private IObjectAtom argument2;

    public BlankNodeAtom(IObjectAtom argument1, IObjectAtom argument2) {
        this.argument1 = argument1;
        this.argument2 = argument2;
    }

    @Override
    public String toString() {

        return "createBN(" + argument1.toString() + ", " + argument2.toString() + ")";
    }

    @Override
    public String prettyPrint() {

        return "Create a blank node typed as " + argument2.toString() + " identified by "
               + argument1.toString();

    }

    public IObjectAtom getArgument1() {
        return argument1;
    }

    public IObjectAtom getArgument2() {
        return argument2;
    }
}
