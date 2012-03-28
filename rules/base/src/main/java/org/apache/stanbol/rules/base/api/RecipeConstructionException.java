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

import java.io.PrintStream;
import java.io.PrintWriter;

/**
 * A {@link RecipeConstructionException} is thrown when an error occurs during the generation of a recipe
 * object after that a recipe is fetched by the store. This does not mean that the recipe does not exist, but
 * only that some error prevents to adapt the recipe in the store to a {@link Recipe} object.
 * 
 * @author anuzzolese
 * 
 */
public class RecipeConstructionException extends Exception {

    /**
	 * 
	 */
    private static final long serialVersionUID = 1L;

    private Throwable e;

    public RecipeConstructionException(Throwable e) {
        this.e = e;
    }

    @Override
    public String getMessage() {
        return "An exception occurred while generating the Recipe.";
    }

    @Override
    public void printStackTrace() {
        e.printStackTrace();
    }

    @Override
    public void printStackTrace(PrintStream s) {
        e.printStackTrace(s);
    }

    @Override
    public void printStackTrace(PrintWriter s) {
        e.printStackTrace(s);
    }

    @Override
    public Throwable getCause() {
        return e;
    }

    @Override
    public StackTraceElement[] getStackTrace() {
        return e.getStackTrace();
    }

}
