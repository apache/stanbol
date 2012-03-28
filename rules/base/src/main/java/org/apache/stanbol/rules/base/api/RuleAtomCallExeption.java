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
package org.apache.stanbol.rules.base.api;

import org.apache.stanbol.rules.base.api.RuleAtom;

/**
 * A {@link RuleAtomCallExeption} is thrown when an adapter is not able to adapt a rule atom of the rule.
 * 
 * @author mac
 * 
 */
public class RuleAtomCallExeption extends Exception {

    /**
	 * 
	 */
    private static final long serialVersionUID = 1L;

    protected Class<?> atomClass;

    /**
     * Creates a new instance of RuleAtomCallExeption.
     * 
     * @param the
     *            {@link RuleAtom} {@link Class}.
     */
    public RuleAtomCallExeption(Class<?> atomClass) {
        this.atomClass = atomClass;
    }

    /**
     * Returns the {@link Class} of the atom that generated the exeption.
     * 
     * @return the atom {@link Class}
     */
    public Class<?> getAtomClass() {
        return atomClass;
    }

    @Override
    public String getMessage() {
        return "The adapter does not provide an implementation for the atom: " + atomClass.getCanonicalName()
               + " ";
    }
}
