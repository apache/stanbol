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
package org.apache.stanbol.rules.base.api.util;

import java.util.Iterator;

import org.apache.stanbol.rules.base.api.Recipe;

public class RecipeIterator implements Iterator<Recipe> {

    private int currentIndex;
    private int listSize;
    private Recipe[] recipes;

    public RecipeIterator(RecipeList recipeList) {
        this.listSize = recipeList.size();
        this.recipes = new Recipe[listSize];
        this.recipes = recipeList.toArray(this.recipes);
        this.currentIndex = 0;

    }

    public boolean hasNext() {
        if (currentIndex < (listSize)) {
            return true;
        } else {
            return false;
        }
    }

    public Recipe next() {
        Recipe recipe = recipes[currentIndex];
        currentIndex++;
        return recipe;
    }

    public void remove() {

    }

}
