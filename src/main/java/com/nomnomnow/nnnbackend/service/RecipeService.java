package com.nomnomnow.nnnbackend.service;

import com.nomnomnow.nnnbackend.repository.IngredientRepository;
import com.nomnomnow.nnnbackend.repository.RecipeRepository;
import com.nomnomnow.nnnbackend.entity.Recipe;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class RecipeService {

    private final IngredientRepository ingredientRepository;
    private final RecipeRepository recipeRepository;

    public Recipe create(Recipe recipe){
        if (recipe.getComponents() != null) {
            for (var component : recipe.getComponents()){
                component.setRecipe(recipe);

                /*var ingredient = component.getIngredient();
                Ingredient savedIngredient = ingredientRepository.save(ingredient);
                component.setIngredient(savedIngredient);*/

            }
        }
        return recipeRepository.save(recipe);
    }
}
