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

import org.apache.stanbol.rules.base.api.RuleAtom;

public class RuleAtomCallExeption extends Exception {

    /**
	 * 
	 */
    private static final long serialVersionUID = 1L;

    protected Class<RuleAtom> atomClass;

    /**
     * Creates a new instance of RuleAtomCallExeption.
     * 
     * @param the
     *            {@link RuleAtom} {@link Class}.
     */
    public RuleAtomCallExeption(Class<RuleAtom> atomClass) {
        this.atomClass = atomClass;
    }

    /**
     * Returns the {@link Class} of the atom that generated the exeption.
     * 
     * @return the atom {@link Class}
     */
    public Class<RuleAtom> getAtomClass() {
        return atomClass;
    }

    @Override
    public String getMessage() {
        String message = super.getMessage() + "Functions implemented by the class "
                         + atomClass.getCanonicalName()
                         + " can only used as arguments of top-level function.";
        return message;
    }
}
