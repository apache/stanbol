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

import java.util.Collection;
import java.util.Iterator;

import org.apache.stanbol.rules.base.api.Recipe;

public class RecipeList implements Collection<Recipe> {

    private Recipe[] recipes;

    public RecipeList() {

    }

    public boolean add(Recipe recipe) {
        if (recipes == null) {
            recipes = new Recipe[1];
            recipes[0] = recipe;
        } else {
            Recipe[] recipesCopy = new Recipe[recipes.length + 1];
            System.arraycopy(recipes, 0, recipesCopy, 0, recipes.length);
            recipesCopy[recipesCopy.length - 1] = recipe;
            recipes = recipesCopy;
        }
        return true;
    }

    public boolean addAll(Collection<? extends Recipe> c) {

        Recipe[] collectionOfRecipes = new Recipe[c.size()];
        collectionOfRecipes = c.toArray(collectionOfRecipes);

        if (recipes == null) {
            recipes = collectionOfRecipes;
        } else {
            Recipe[] recipesCopy = new Recipe[recipes.length + collectionOfRecipes.length];
            System.arraycopy(recipes, 0, recipesCopy, 0, recipes.length);
            System.arraycopy(collectionOfRecipes, 0, recipesCopy, recipes.length, collectionOfRecipes.length);
            recipes = recipesCopy;
        }
        return true;
    }

    public void clear() {
        // TODO Auto-generated method stub

    }

    public boolean contains(Object o) {
        for (Recipe recipe : recipes) {
            if (recipe.equals(o)) {
                return true;
            }
        }
        return false;
    }

    public boolean containsAll(Collection<?> c) {

        for (Object o : c) {
            for (Recipe recipe : recipes) {
                if (!recipe.equals(o)) {
                    return false;
                } else {
                    break;
                }
            }
        }
        return true;
    }

    public boolean isEmpty() {
        if (recipes == null || (recipes.length == 1 && recipes[0] == null)) {
            return true;
        } else {
            return false;
        }
    }

    public Iterator<Recipe> iterator() {
        return new RecipeIterator(this);
    }

    public boolean remove(Object o) {
        boolean removed = false;
        for (int i = 0; i < recipes.length && !removed; i++) {
            Recipe recipe = recipes[i];
            if (recipe.equals(o)) {
                Recipe[] recipesCopy = new Recipe[recipes.length - 1];
                System.arraycopy(recipes, 0, recipesCopy, 0, i);
                System.arraycopy(recipes, i + 1, recipesCopy, 0, recipesCopy.length - i);
                recipes = recipesCopy;
                removed = true;
            }
        }
        return removed;
    }

    public boolean removeAll(Collection<?> c) {
        if (contains(c)) {
            for (Object o : c) {
                boolean removed = false;
                for (int i = 0; i < recipes.length && !removed; i++) {
                    Recipe recipe = recipes[i];
                    if (recipe.equals(o)) {
                        Recipe[] recipesCopy = new Recipe[recipes.length - 1];
                        System.arraycopy(recipes, 0, recipesCopy, 0, i);
                        System.arraycopy(recipes, i + 1, recipesCopy, 0, recipesCopy.length - i);
                        recipes = recipesCopy;
                        removed = true;
                    }
                }
            }
            return true;
        } else {
            return false;
        }
    }

    public boolean retainAll(Collection<?> c) {
        Recipe[] recipesCopy = null;
        Recipe[] recipesTMP = null;
        for (Object o : c) {
            if (o instanceof Recipe) {
                if (contains(o)) {
                    if (recipesCopy == null) {
                        recipesCopy = new Recipe[1];
                        recipesCopy[0] = (Recipe) o;
                    } else {
                        recipesTMP = new Recipe[recipesCopy.length + 1];
                        System.arraycopy(recipesCopy, 0, recipesTMP, 0, recipesCopy.length);
                        recipesTMP[recipesTMP.length - 1] = (Recipe) o;
                        recipesCopy = recipesTMP;
                    }
                }
            }
        }
        recipes = recipesCopy;
        return true;
    }

    public int size() {
        if (recipes == null) {
            return 0;
        } else {
            return recipes.length;
        }

    }

    public Object[] toArray() {
        return recipes;
    }

    public <T> T[] toArray(T[] a) {
        return (T[]) recipes;
    }

}
