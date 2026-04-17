package com.nomnomnow.nnnbackend.service;

import com.nomnomnow.nnnbackend.dto.request.RecipeComponentRequest;
import com.nomnomnow.nnnbackend.dto.request.RecipeRequest;
import com.nomnomnow.nnnbackend.entity.Ingredient;
import com.nomnomnow.nnnbackend.entity.Recipe;
import com.nomnomnow.nnnbackend.entity.RecipeComponent;
import com.nomnomnow.nnnbackend.exception.ResourceNotFoundException;
import com.nomnomnow.nnnbackend.repository.IngredientRepository;
import com.nomnomnow.nnnbackend.repository.RecipeRepository;
import com.nomnomnow.nnnbackend.user.CurrentUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RecipeService {

    private final IngredientRepository ingredientRepository;
    private final RecipeRepository recipeRepository;
    private final CurrentUserService currentUserService;

    @Transactional
    public Recipe create(RecipeRequest request) {
        var recipe = new Recipe();
        recipe.setName(request.name().trim());
        recipe.setInstructions(request.instructions());
        recipe.setCookingTime(request.cookingTime());
        recipe.setOwner(currentUserService.getCurrentUser());

        recipe.setCategories(request.categoryIds());
        attachComponents(recipe, request.components());

        return recipeRepository.save(recipe);
    }

    @Transactional(readOnly = true)
    public Page<Recipe> findAll(Pageable pageable) {
        return recipeRepository.findAll(pageable);
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
