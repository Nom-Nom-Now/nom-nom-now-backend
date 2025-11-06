package com.nomnomnow.nnnbackend.service;

import com.nomnomnow.nnnbackend.dto.request.RecipeComponentRequest;
import com.nomnomnow.nnnbackend.dto.request.RecipeRequest;
import com.nomnomnow.nnnbackend.entity.Category;
import com.nomnomnow.nnnbackend.entity.Ingredient;
import com.nomnomnow.nnnbackend.entity.Recipe;
import com.nomnomnow.nnnbackend.entity.RecipeComponent;
import com.nomnomnow.nnnbackend.exception.ResourceNotFoundException;
import com.nomnomnow.nnnbackend.repository.CategoryRepository;
import com.nomnomnow.nnnbackend.repository.IngredientRepository;
import com.nomnomnow.nnnbackend.repository.RecipeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RecipeService {

    private final CategoryRepository categoryRepository;
    private final IngredientRepository ingredientRepository;
    private final RecipeRepository recipeRepository;

    @Transactional
    public Recipe create(RecipeRequest request) {
        var recipe = new Recipe();
        recipe.setName(request.name());
        recipe.setInstructions(request.instructions());
        recipe.setCookingTime(request.cookingTime());

        attachCategories(recipe, request.categoryIds());
        attachComponents(recipe, request.components());

        return recipeRepository.save(recipe);
    }

    @Transactional(readOnly = true)
    public List<Recipe> findAll() {
        return recipeRepository.findAll();
    }

    private void attachCategories(Recipe recipe, Set<Long> categoryIds) {
        recipe.getCategories().clear();
        if (categoryIds == null || categoryIds.isEmpty()) {
            return;
        }

        var categories = categoryRepository.findAllById(categoryIds);
        var foundIds = categories.stream()
                .map(Category::getId)
                .collect(Collectors.toSet());
        var requestedIds = new HashSet<>(categoryIds);
        requestedIds.removeAll(foundIds);
        if (!requestedIds.isEmpty()) {
            throw new ResourceNotFoundException("Unknown category ids: " + requestedIds);
        }

        recipe.getCategories().addAll(categories);
    }

    private void attachComponents(Recipe recipe, List<RecipeComponentRequest> componentRequests) {
        recipe.getComponents().clear();
        if (componentRequests == null || componentRequests.isEmpty()) {
            return;
        }

        var existingIngredientsById = loadExistingIngredients(componentRequests);
        var cachedIngredientsByName = new HashMap<String, Ingredient>();

        for (RecipeComponentRequest componentRequest : componentRequests) {
            var recipeComponent = buildComponent(recipe, componentRequest, existingIngredientsById, cachedIngredientsByName);
            recipe.getComponents().add(recipeComponent);
        }
    }

    private Map<Long, Ingredient> loadExistingIngredients(List<RecipeComponentRequest> componentRequests) {
        var ingredientIds = componentRequests.stream()
                .map(RecipeComponentRequest::ingredientId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        if (ingredientIds.isEmpty()) {
            return Map.of();
        }

        var ingredients = ingredientRepository.findAllById(ingredientIds);
        var ingredientsById = ingredients.stream()
                .collect(Collectors.toMap(Ingredient::getId, ingredient -> ingredient));

        var missingIds = new HashSet<>(ingredientIds);
        missingIds.removeAll(ingredientsById.keySet());
        if (!missingIds.isEmpty()) {
            throw new ResourceNotFoundException("Unknown ingredient ids: " + missingIds);
        }

        return ingredientsById;
    }

    private RecipeComponent buildComponent(Recipe recipe,
                                           RecipeComponentRequest componentRequest,
                                           Map<Long, Ingredient> existingIngredientsById,
                                           Map<String, Ingredient> cachedIngredientsByName) {
        var recipeComponent = new RecipeComponent();
        recipeComponent.setRecipe(recipe);
        recipeComponent.setUnit(componentRequest.unit());
        recipeComponent.setQuantity(componentRequest.quantity());
        recipeComponent.setIngredient(findOrCreateIngredient(componentRequest, existingIngredientsById, cachedIngredientsByName));
        return recipeComponent;
    }

    private Ingredient findOrCreateIngredient(RecipeComponentRequest componentRequest,
                                              Map<Long, Ingredient> existingIngredientsById,
                                              Map<String, Ingredient> cachedIngredientsByName) {
        var ingredientId = componentRequest.ingredientId();
        if (ingredientId != null) {
            return existingIngredientsById.get(ingredientId);
        }

        var trimmedName = componentRequest.ingredientName().trim();
        var cacheKey = trimmedName.toLowerCase(Locale.ROOT);

        if (cachedIngredientsByName.containsKey(cacheKey)) {
            return cachedIngredientsByName.get(cacheKey);
        }

        var ingredient = ingredientRepository.findByNameIgnoreCase(trimmedName)
                .orElseGet(() -> {
                    var newIngredient = new Ingredient();
                    newIngredient.setName(trimmedName);
                    return ingredientRepository.save(newIngredient);
                });

        cachedIngredientsByName.put(cacheKey, ingredient);
        return ingredient;
    }
}
