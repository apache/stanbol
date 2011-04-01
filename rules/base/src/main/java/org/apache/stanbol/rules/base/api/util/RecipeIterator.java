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
		if(currentIndex<(listSize-1)){
			return true;
		}
		else{
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
