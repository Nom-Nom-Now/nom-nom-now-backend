package com.nomnomnow.nnnbackend.service;

import com.nomnomnow.nnnbackend.dto.request.RecipeComponentRequest;
import com.nomnomnow.nnnbackend.dto.request.RecipeRequest;
import com.nomnomnow.nnnbackend.entity.Ingredient;
import com.nomnomnow.nnnbackend.entity.Recipe;
import com.nomnomnow.nnnbackend.entity.RecipeComponent;
import com.nomnomnow.nnnbackend.entity.Unit;
import com.nomnomnow.nnnbackend.repository.IngredientRepository;
import com.nomnomnow.nnnbackend.repository.RecipeComponentRepository;
import com.nomnomnow.nnnbackend.repository.RecipeRepository;
import com.nomnomnow.nnnbackend.user.AppUser;
import com.nomnomnow.nnnbackend.user.CurrentUserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.math.BigDecimal;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RecipeServiceTest {

    @Mock
    private IngredientRepository ingredientRepository;

    @Mock
    private RecipeRepository recipeRepository;

    @Mock
    private CurrentUserService currentUserService;

    @Mock
    private RecipeComponentRepository recipeComponentRepository;

    private RecipeService recipeService;

    @BeforeEach
    void setUp() {
        recipeService = new RecipeService(
                ingredientRepository,
                recipeRepository,
                currentUserService,
                recipeComponentRepository
        );
    }

    @Test
    void updateRecipeReplacesEditableFieldsAndKeepsExistingImage() {
        var owner = user(42L);
        var oldIngredient = ingredient(10L, "Old Flour");
        var recipe = recipe(1L, owner);
        recipe.setImageData(new byte[]{1, 2, 3});
        recipe.setImageContentType("image/jpeg");
        recipe.setImageFilename("old.jpg");
        recipe.setImageSize(3L);
        recipe.getComponents().add(component(recipe, oldIngredient));

        when(recipeRepository.findById(1L)).thenReturn(Optional.of(recipe));
        when(currentUserService.getCurrentUser()).thenReturn(owner);
        when(ingredientRepository.findByNameIgnoreCase("Salt")).thenReturn(Optional.empty());
        when(ingredientRepository.save(any(Ingredient.class))).thenAnswer(invocation -> {
            var ingredient = (Ingredient) invocation.getArgument(0);
            ingredient.setId(20L);
            return ingredient;
        });
        when(recipeRepository.saveAndFlush(recipe)).thenReturn(recipe);
        when(recipeComponentRepository.existsByIngredientId(10L)).thenReturn(false);

        var updatedRecipe = recipeService.updateRecipe(1L, request());

        assertThat(updatedRecipe.getName()).isEqualTo("Updated Recipe");
        assertThat(updatedRecipe.getInstructions()).isEqualTo("Updated instructions");
        assertThat(updatedRecipe.getCookingTime()).isEqualTo(35);
        assertThat(updatedRecipe.getPricePerPerson()).isEqualTo(499);
        assertThat(updatedRecipe.getCategories()).isEqualTo("2,3");
        assertThat(updatedRecipe.getImageData()).containsExactly(1, 2, 3);
        assertThat(updatedRecipe.getImageContentType()).isEqualTo("image/jpeg");
        assertThat(updatedRecipe.getComponents()).hasSize(1);
        assertThat(updatedRecipe.getComponents().iterator().next().getIngredient().getName()).isEqualTo("Salt");
        verify(recipeRepository).saveAndFlush(recipe);
        verify(ingredientRepository).deleteById(10L);
    }

    @Test
    void updateRecipeCanReplaceComponentListWithSameIngredient() {
        var owner = user(42L);
        var ingredient = ingredient(10L, "Smoke Water");
        var recipe = recipe(1L, owner);
        recipe.getComponents().add(component(recipe, ingredient));

        when(recipeRepository.findById(1L)).thenReturn(Optional.of(recipe));
        when(currentUserService.getCurrentUser()).thenReturn(owner);
        when(ingredientRepository.findAllById(Set.of(10L))).thenReturn(List.of(ingredient));
        when(recipeRepository.saveAndFlush(recipe)).thenReturn(recipe);
        when(recipeComponentRepository.existsByIngredientId(10L)).thenReturn(true);

        var updatedRecipe = recipeService.updateRecipe(1L, requestWithIngredientId(10L));

        assertThat(updatedRecipe.getComponents()).hasSize(1);
        assertThat(updatedRecipe.getComponents().iterator().next().getQuantity()).isEqualByComparingTo("3");
        verify(recipeRepository).flush();
        verify(ingredientRepository, never()).deleteById(10L);
    }

    @Test
    void updateRecipeRejectsNonOwner() {
        var recipe = recipe(1L, user(42L));

        when(recipeRepository.findById(1L)).thenReturn(Optional.of(recipe));
        when(currentUserService.getCurrentUser()).thenReturn(user(99L));

        assertThatThrownBy(() -> recipeService.updateRecipe(1L, request()))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessage("You are not allowed to update this recipe");

        verify(recipeRepository, never()).saveAndFlush(any());
        verifyNoInteractions(ingredientRepository, recipeComponentRepository);
    }

    private RecipeRequest request() {
        return new RecipeRequest(
                " Updated Recipe ",
                "Updated instructions",
                35,
                499,
                new LinkedHashSet<>(List.of(2L, 3L)),
                List.of(new RecipeComponentRequest(null, "Salt", BigDecimal.valueOf(2), Unit.GRAM))
        );
    }

    private RecipeRequest requestWithIngredientId(Long ingredientId) {
        return new RecipeRequest(
                "Updated Recipe",
                "Updated instructions",
                35,
                499,
                new LinkedHashSet<>(List.of(2L, 3L)),
                List.of(new RecipeComponentRequest(ingredientId, "Smoke Water", BigDecimal.valueOf(3), Unit.MILLILITER))
        );
    }

    private Recipe recipe(Long id, AppUser owner) {
        var recipe = new Recipe();
        recipe.setId(id);
        recipe.setOwner(owner);
        return recipe;
    }

    private AppUser user(Long id) {
        var user = new AppUser();
        user.setId(id);
        return user;
    }

    private Ingredient ingredient(Long id, String name) {
        var ingredient = new Ingredient();
        ingredient.setId(id);
        ingredient.setName(name);
        return ingredient;
    }

    private RecipeComponent component(Recipe recipe, Ingredient ingredient) {
        var component = new RecipeComponent();
        component.setRecipe(recipe);
        component.setIngredient(ingredient);
        component.setQuantity(BigDecimal.ONE);
        component.setUnit(Unit.GRAM);
        return component;
    }
}
