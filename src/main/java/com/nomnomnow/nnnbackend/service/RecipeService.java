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
        recipe.setOwner(currentUserService.getCurrentUser());

        applyRecipeRequest(recipe, request);
        attachImage(recipe, image);

        return recipeRepository.save(recipe);
    }

    @Transactional
    public Recipe updateRecipe(long recipeId, RecipeRequest request) {
        return updateRecipe(recipeId, request, null);
    }

    @Transactional
    public Recipe updateRecipe(long recipeId, RecipeRequest request, MultipartFile image) {
        var recipe = findRecipe(recipeId);
        ensureCurrentUserOwns(recipe, "update");

        var previousIngredientIds = collectIngredientIds(recipe);

        applyRecipeFields(recipe, request);
        recipe.getComponents().clear();
        recipeRepository.flush();
        attachComponents(recipe, request.components());
        attachImage(recipe, image);

        var savedRecipe = recipeRepository.saveAndFlush(recipe);
        deleteOrphanedIngredients(previousIngredientIds);

        return savedRecipe;
    }

    @Transactional(readOnly = true)
    public Page<Recipe> findAll(Pageable pageable) {
        return recipeRepository.findAll(pageable);
    }

    @Transactional(readOnly = true)
    public Recipe getRecipeImage(long recipeId) {
        var recipe = findRecipe(recipeId);

        if (recipe.getImageData() == null || recipe.getImageData().length == 0) {
            throw new ResourceNotFoundException("Recipe image not found with recipe id: " + recipeId);
        }

        return recipe;
    }

    private Recipe findRecipe(long recipeId) {
        return recipeRepository.findById(recipeId)
                .orElseThrow(() -> new ResourceNotFoundException("Recipe not found with id: " + recipeId));
    }

    private void applyRecipeRequest(Recipe recipe, RecipeRequest request) {
        applyRecipeFields(recipe, request);
        attachComponents(recipe, request.components());
    }

    private void applyRecipeFields(Recipe recipe, RecipeRequest request) {
        recipe.setName(request.name().trim());
        recipe.setInstructions(request.instructions());
        recipe.setCookingTime(request.cookingTime());
        recipe.setPricePerPerson(request.pricePerPerson());
        recipe.setCategories(request.categoryIds());
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

        var attachedIngredientIds = new HashSet<Long>();
        var attachedIngredientNames = new HashSet<String>();

        for (RecipeComponentRequest componentRequest : componentRequests) {
            var ingredient = findOrCreateIngredient(componentRequest, existingIngredientsById, cachedIngredientsByName);
            ensureIngredientNotAlreadyAttached(ingredient, attachedIngredientIds, attachedIngredientNames);

            var recipeComponent = buildComponent(recipe, componentRequest, ingredient);
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
                                           Ingredient ingredient) {
        var recipeComponent = new RecipeComponent();
        recipeComponent.setRecipe(recipe);
        recipeComponent.setUnit(componentRequest.unit());
        recipeComponent.setQuantity(componentRequest.quantity());
        recipeComponent.setIngredient(ingredient);
        return recipeComponent;
    }

    private void ensureIngredientNotAlreadyAttached(Ingredient ingredient,
                                                    Set<Long> attachedIngredientIds,
                                                    Set<String> attachedIngredientNames) {
        if (ingredient.getId() != null) {
            if (!attachedIngredientIds.add(ingredient.getId())) {
                throw duplicateIngredientException(ingredient);
            }
            return;
        }

        var nameKey = normalizeIngredientName(ingredient.getName());
        if (!attachedIngredientNames.add(nameKey)) {
            throw duplicateIngredientException(ingredient);
        }
    }

    private BadRequestException duplicateIngredientException(Ingredient ingredient) {
        return new BadRequestException("Use each ingredient only once per recipe: " + ingredient.getName());
    }

    private Ingredient findOrCreateIngredient(RecipeComponentRequest componentRequest,
                                              Map<Long, Ingredient> existingIngredientsById,
                                              Map<String, Ingredient> cachedIngredientsByName) {
        var ingredientId = componentRequest.ingredientId();
        if (ingredientId != null) {
            return existingIngredientsById.get(ingredientId);
        }

        var trimmedName = componentRequest.ingredientName().trim();
        var cacheKey = normalizeIngredientName(trimmedName);

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

    private String normalizeIngredientName(String name) {
        return name.trim().toLowerCase(Locale.ROOT);
    }

    @Transactional
    public void deleteRecipe(long recipeId) {
        var recipe = findRecipe(recipeId);
        ensureCurrentUserOwns(recipe, "delete");

        // Ingredient-IDs merken, bevor die Components via orphanRemoval gelöscht werden
        var ingredientIds = collectIngredientIds(recipe);

        recipeRepository.delete(recipe);
        // Flush, damit die gerade gelöschten RecipeComponents im anschließenden
        // existsByIngredientId-Check nicht mehr sichtbar sind.
        recipeRepository.flush();

        deleteOrphanedIngredients(ingredientIds);
        log.info("Deleted recipe {} with {} components", recipeId, ingredientIds.size());
    }

    private void ensureCurrentUserOwns(Recipe recipe, String action) {
        var currentUser = currentUserService.getCurrentUser();
        if (recipe.getOwner() == null || !recipe.getOwner().getId().equals(currentUser.getId())) {
            throw new AccessDeniedException("You are not allowed to " + action + " this recipe");
        }
    }

    private Set<Long> collectIngredientIds(Recipe recipe) {
        return recipe.getComponents().stream()
                .map(component -> component.getIngredient().getId())
                .collect(Collectors.toSet());
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
