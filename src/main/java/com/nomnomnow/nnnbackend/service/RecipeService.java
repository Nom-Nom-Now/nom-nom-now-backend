package com.nomnomnow.nnnbackend.service;

import com.nomnomnow.nnnbackend.dto.request.RecipeComponentRequest;
import com.nomnomnow.nnnbackend.dto.request.RecipeRequest;
import com.nomnomnow.nnnbackend.entity.Ingredient;
import com.nomnomnow.nnnbackend.entity.Recipe;
import com.nomnomnow.nnnbackend.entity.RecipeComponent;
import com.nomnomnow.nnnbackend.exception.BadRequestException;
import com.nomnomnow.nnnbackend.exception.ResourceNotFoundException;
import com.nomnomnow.nnnbackend.repository.IngredientRepository;
import com.nomnomnow.nnnbackend.repository.RecipeComponentRepository;
import com.nomnomnow.nnnbackend.repository.RecipeRepository;
import com.nomnomnow.nnnbackend.user.CurrentUserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class RecipeService {
    private static final long MAX_IMAGE_BYTES = 5L * 1024L * 1024L;

    private final IngredientRepository ingredientRepository;
    private final RecipeRepository recipeRepository;
    private final CurrentUserService currentUserService;
    private final RecipeComponentRepository recipeComponentRepository;

    @Transactional
    public Recipe create(RecipeRequest request) {
        return create(request, null);
    }

    @Transactional
    public Recipe create(RecipeRequest request, MultipartFile image) {
        var recipe = new Recipe();
        recipe.setName(request.name().trim());
        recipe.setInstructions(request.instructions());
        recipe.setCookingTime(request.cookingTime());
        recipe.setPricePerPerson(request.pricePerPerson());
        recipe.setOwner(currentUserService.getCurrentUser());

        recipe.setCategories(request.categoryIds());
        attachComponents(recipe, request.components());
        attachImage(recipe, image);

        return recipeRepository.save(recipe);
    }

    @Transactional(readOnly = true)
    public Page<Recipe> findAll(Pageable pageable) {
        return recipeRepository.findAll(pageable);
    }

    @Transactional(readOnly = true)
    public Recipe getRecipeImage(long recipeId) {
        var recipe = recipeRepository.findById(recipeId)
                .orElseThrow(() -> new ResourceNotFoundException("Recipe not found with id: " + recipeId));

        if (recipe.getImageData() == null || recipe.getImageData().length == 0) {
            throw new ResourceNotFoundException("Recipe image not found with recipe id: " + recipeId);
        }

        return recipe;
    }

    private void attachImage(Recipe recipe, MultipartFile image) {
        if (image == null || image.isEmpty()) {
            return;
        }

        var contentType = image.getContentType();
        if (contentType == null || !contentType.toLowerCase(Locale.ROOT).startsWith("image/")) {
            throw new BadRequestException("Only image uploads are supported");
        }

        if (image.getSize() > MAX_IMAGE_BYTES) {
            throw new BadRequestException("Recipe image must be 5 MB or smaller");
        }

        try {
            recipe.setImageData(image.getBytes());
            recipe.setImageContentType(contentType);
            recipe.setImageFilename(image.getOriginalFilename());
            recipe.setImageSize(image.getSize());
        } catch (IOException exception) {
            throw new BadRequestException("Could not read recipe image");
        }
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

    @Transactional
    public void deleteRecipe(long recipeId) {
        var recipe = recipeRepository.findById(recipeId)
                .orElseThrow(() -> new ResourceNotFoundException("Recipe not found with id: " + recipeId));

        var currentUser = currentUserService.getCurrentUser();
        if (!recipe.getOwner().getId().equals(currentUser.getId())) {
            throw new AccessDeniedException("You are not allowed to delete this recipe");
        }

        // Ingredient-IDs merken, bevor die Components via orphanRemoval gelöscht werden
        var ingredientIds = recipe.getComponents().stream()
                .map(component -> component.getIngredient().getId())
                .collect(Collectors.toSet());

        recipeRepository.delete(recipe);
        // Flush, damit die gerade gelöschten RecipeComponents im anschließenden
        // existsByIngredientId-Check nicht mehr sichtbar sind.
        recipeRepository.flush();

        deleteOrphanedIngredients(ingredientIds);
        log.info("Deleted recipe {} with {} components", recipeId, ingredientIds.size());
    }

    private void deleteOrphanedIngredients(Set<Long> ingredientIds) {
        for (Long ingredientId : ingredientIds) {
            if (!recipeComponentRepository.existsByIngredientId(ingredientId)) {
                ingredientRepository.deleteById(ingredientId);
                log.debug("Deleted orphaned ingredient {}", ingredientId);
            }
        }
    }
}
