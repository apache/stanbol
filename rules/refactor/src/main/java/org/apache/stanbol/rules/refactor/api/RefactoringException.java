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
package org.apache.stanbol.rules.refactor.api;

import java.io.PrintStream;
import java.io.PrintWriter;

import org.semanticweb.owlapi.model.IRI;

/**
 * A {@link RefactoringException} is thrown when an error occurs during the refactoring.
 * 
 * @author anuzzolese
 * 
 */

public class RefactoringException extends Exception {

    /**
	 * 
	 */
    private static final long serialVersionUID = 1L;

    protected IRI recipeIRI;

    private Throwable t;

    private String message;

    /**
     * Creates a new instance of RefactoringException.
     */
    public RefactoringException(String message, Throwable t) {
        this.message = message;
        this.t = t;
    }

    @Override
    public void printStackTrace() {
        t.printStackTrace();
    }

    @Override
    public String getMessage() {
        return message;
    }

    public void printStackTrace(PrintStream s) {
        t.printStackTrace(s);
    }

    @Override
    public void printStackTrace(PrintWriter s) {
        t.printStackTrace(s);
    }

}
