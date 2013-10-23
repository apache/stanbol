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

/**
 * A {@link NoSuchRecipeException} is thrown when the recipe requested does not exist in the store.
 * 
 * @author anuzzolese
 * 
 */

public class NoSuchRecipeException extends Exception {

    /**
	 * 
	 */
    private static final long serialVersionUID = 1L;

    protected String recipeID;

    /**
     * Creates a new instance of OntologySpaceModificationException.
     * 
     * @param space
     *            the ontology space whose modification was attempted.
     */
    public NoSuchRecipeException(String recipeID) {
        this.recipeID = recipeID;
    }

    /**
     * Returns the {@link String} of the recipe that threw the exception.
     * 
     * @return the recipe {@link String} on which the exception was thrown.
     */
    public String getRecipeID() {
        return recipeID;
    }

    @Override
    public String getMessage() {
        return "The recipe " + recipeID + " does not exist.";
    }

}
