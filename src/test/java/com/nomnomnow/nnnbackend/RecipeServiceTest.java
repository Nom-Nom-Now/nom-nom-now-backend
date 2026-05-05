package com.nomnomnow.nnnbackend;

import com.nomnomnow.nnnbackend.dto.request.RecipeComponentRequest;
import com.nomnomnow.nnnbackend.dto.request.RecipeRequest;
import com.nomnomnow.nnnbackend.entity.Ingredient;
import com.nomnomnow.nnnbackend.entity.Recipe;
import com.nomnomnow.nnnbackend.entity.RecipeComponent;
import com.nomnomnow.nnnbackend.entity.Unit;
import com.nomnomnow.nnnbackend.exception.ResourceNotFoundException;
import com.nomnomnow.nnnbackend.repository.IngredientRepository;
import com.nomnomnow.nnnbackend.repository.RecipeComponentRepository;
import com.nomnomnow.nnnbackend.repository.RecipeRepository;
import com.nomnomnow.nnnbackend.service.RecipeService;
import com.nomnomnow.nnnbackend.user.AppUser;
import com.nomnomnow.nnnbackend.user.CurrentUserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RecipeServiceTest {

    @Mock
    private RecipeRepository recipeRepository;

    @Mock
    private IngredientRepository ingredientRepository;

    @Mock
    private RecipeComponentRepository recipeComponentRepository;

    @Mock
    private CurrentUserService currentUserService;

    @InjectMocks
    private RecipeService recipeService;

    private AppUser ownerUser;
    private AppUser otherUser;

    @BeforeEach
    void setUp() {
        ownerUser = new AppUser();
        ownerUser.setId(1L);
        ownerUser.setGoogleId("google-123");
        ownerUser.setEmail("owner@example.com");
        ownerUser.setName("Owner");

        otherUser = new AppUser();
        otherUser.setId(2L);
        otherUser.setGoogleId("google-456");
        otherUser.setEmail("other@example.com");
        otherUser.setName("Other");
    }

    // ---- create() ----

    @Test
    void create_setsOwnerAndSaves() {
        when(currentUserService.getCurrentUser()).thenReturn(ownerUser);

        var ingredient = new Ingredient();
        ingredient.setId(10L);
        ingredient.setName("Tomato");

        when(ingredientRepository.findByNameIgnoreCase("Tomato")).thenReturn(Optional.of(ingredient));
        when(recipeRepository.save(any(Recipe.class))).thenAnswer(invocation -> {
            Recipe r = invocation.getArgument(0);
            r.setId(100L);
            return r;
        });

        var componentRequest = new RecipeComponentRequest(null, "Tomato", new BigDecimal("200"), Unit.GRAM);
        var request = new RecipeRequest("Tomato Soup", "Boil tomatoes", 30, 5, Set.of(1L), List.of(componentRequest));

        var result = recipeService.create(request);

        assertThat(result.getName()).isEqualTo("Tomato Soup");
        assertThat(result.getInstructions()).isEqualTo("Boil tomatoes");
        assertThat(result.getCookingTime()).isEqualTo(30);
        assertThat(result.getPricePerPerson()).isEqualTo(5);
        assertThat(result.getOwner()).isEqualTo(ownerUser);
        assertThat(result.getComponents()).hasSize(1);
        assertThat(result.getComponents().iterator().next().getIngredient().getName()).isEqualTo("Tomato");

        verify(currentUserService).getCurrentUser();
        verify(recipeRepository).save(any(Recipe.class));
    }

    @Test
    void create_createsNewIngredientWhenNotFoundByName() {
        when(currentUserService.getCurrentUser()).thenReturn(ownerUser);
        when(ingredientRepository.findByNameIgnoreCase("Onion")).thenReturn(Optional.empty());
        when(ingredientRepository.save(any(Ingredient.class))).thenAnswer(inv -> {
            Ingredient i = inv.getArgument(0);
            i.setId(20L);
            return i;
        });
        when(recipeRepository.save(any(Recipe.class))).thenAnswer(inv -> {
            Recipe r = inv.getArgument(0);
            r.setId(101L);
            return r;
        });

        var componentRequest = new RecipeComponentRequest(null, "Onion", new BigDecimal("100"), Unit.GRAM);
        var request = new RecipeRequest("Onion Soup", null, null, null, null, List.of(componentRequest));

        var result = recipeService.create(request);

        assertThat(result.getName()).isEqualTo("Onion Soup");
        verify(ingredientRepository).save(any(Ingredient.class));
    }

    @Test
    void create_reusesExistingIngredientById() {
        when(currentUserService.getCurrentUser()).thenReturn(ownerUser);

        var ingredient = new Ingredient();
        ingredient.setId(10L);
        ingredient.setName("Garlic");

        when(ingredientRepository.findAllById(Set.of(10L))).thenReturn(List.of(ingredient));
        when(recipeRepository.save(any(Recipe.class))).thenAnswer(inv -> {
            Recipe r = inv.getArgument(0);
            r.setId(102L);
            return r;
        });

        var componentRequest = new RecipeComponentRequest(10L, "Garlic", new BigDecimal("3"), Unit.PIECE);
        var request = new RecipeRequest("Garlic Bread", null, 15, 3, null, List.of(componentRequest));

        var result = recipeService.create(request);

        assertThat(result.getComponents()).hasSize(1);
        verify(ingredientRepository).findAllById(Set.of(10L));
        verify(ingredientRepository, never()).findByNameIgnoreCase(anyString());
    }

    // ---- findById() ----

    @Test
    void findById_returnsRecipe() {
        var recipe = new Recipe();
        recipe.setId(1L);
        recipe.setName("Pasta");

        when(recipeRepository.findById(1L)).thenReturn(Optional.of(recipe));

        var result = recipeService.findById(1L);

        assertThat(result.getName()).isEqualTo("Pasta");
    }

    @Test
    void findById_throwsWhenNotFound() {
        when(recipeRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> recipeService.findById(999L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("999");
    }

    // ---- findAll() ----

    @Test
    void findAll_delegatesToRepository() {
        var recipe = new Recipe();
        recipe.setId(1L);
        Page<Recipe> page = new PageImpl<>(List.of(recipe));

        when(recipeRepository.findAll(any(Pageable.class))).thenReturn(page);

        var result = recipeService.findAll(Pageable.unpaged());

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getName()).isNull(); // name not set in this simple test
        verify(recipeRepository).findAll(any(Pageable.class));
    }

    // ---- updateRecipe() ----

    @Test
    void updateRecipe_updatesMetadataAndComponents() {
        var recipe = new Recipe();
        recipe.setId(5L);
        recipe.setName("Old Name");
        recipe.setOwner(ownerUser);
        recipe.setComponents(new java.util.HashSet<>());

        when(recipeRepository.findById(5L)).thenReturn(Optional.of(recipe));
        when(currentUserService.getCurrentUser()).thenReturn(ownerUser);
        when(recipeRepository.save(any(Recipe.class))).thenAnswer(inv -> inv.getArgument(0));
        when(recipeComponentRepository.existsByIngredientId(anyLong())).thenReturn(false);

        var ingredient = new Ingredient();
        ingredient.setId(10L);
        ingredient.setName("Butter");
        when(ingredientRepository.findAllById(Set.of(10L))).thenReturn(List.of(ingredient));

        var componentRequest = new RecipeComponentRequest(10L, "Butter", new BigDecimal("50"), Unit.GRAM);
        var request = new RecipeRequest("New Name", "New instructions", 45, 10, Set.of(2L), List.of(componentRequest));

        var result = recipeService.updateRecipe(5L, request);

        assertThat(result.getName()).isEqualTo("New Name");
        assertThat(result.getInstructions()).isEqualTo("New instructions");
        assertThat(result.getCookingTime()).isEqualTo(45);
        assertThat(result.getPricePerPerson()).isEqualTo(10);

        verify(recipeRepository).save(any(Recipe.class));
        verify(recipeRepository).flush();
    }

    @Test
    void updateRecipe_throwsWhenRecipeNotFound() {
        when(recipeRepository.findById(999L)).thenReturn(Optional.empty());

        var request = new RecipeRequest("X", null, null, null, null, List.of(
                new RecipeComponentRequest(null, "Y", new BigDecimal("1"), Unit.GRAM)
        ));

        assertThatThrownBy(() -> recipeService.updateRecipe(999L, request))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void updateRecipe_throwsAccessDeniedForNonOwner() {
        var recipe = new Recipe();
        recipe.setId(5L);
        recipe.setName("My Recipe");
        recipe.setOwner(ownerUser);
        recipe.setComponents(new java.util.HashSet<>());

        when(recipeRepository.findById(5L)).thenReturn(Optional.of(recipe));
        when(currentUserService.getCurrentUser()).thenReturn(otherUser);

        var request = new RecipeRequest("Hacked", null, null, null, null, List.of(
                new RecipeComponentRequest(null, "X", new BigDecimal("1"), Unit.GRAM)
        ));

        assertThatThrownBy(() -> recipeService.updateRecipe(5L, request))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("update");

        verify(recipeRepository, never()).save(any());
    }

    // ---- deleteRecipe() ----

    @Test
    void deleteRecipe_deletesAndCleansUpOrphans() {
        var ingredient = new Ingredient();
        ingredient.setId(10L);
        var recipeComponent = new RecipeComponent();
        recipeComponent.setIngredient(ingredient);

        var recipe = new Recipe();
        recipe.setId(5L);
        recipe.setOwner(ownerUser);
        recipe.getComponents().add(recipeComponent);

        when(recipeRepository.findById(5L)).thenReturn(Optional.of(recipe));
        when(currentUserService.getCurrentUser()).thenReturn(ownerUser);
        when(recipeComponentRepository.existsByIngredientId(10L)).thenReturn(false);

        recipeService.deleteRecipe(5L);

        verify(recipeRepository).delete(recipe);
        verify(recipeRepository).flush();
        verify(ingredientRepository).deleteById(10L);
    }

    @Test
    void deleteRecipe_doesNotDeleteIngredientIfStillUsed() {
        var ingredient = new Ingredient();
        ingredient.setId(10L);
        var recipeComponent = new RecipeComponent();
        recipeComponent.setIngredient(ingredient);

        var recipe = new Recipe();
        recipe.setId(5L);
        recipe.setOwner(ownerUser);
        recipe.getComponents().add(recipeComponent);

        when(recipeRepository.findById(5L)).thenReturn(Optional.of(recipe));
        when(currentUserService.getCurrentUser()).thenReturn(ownerUser);
        when(recipeComponentRepository.existsByIngredientId(10L)).thenReturn(true);

        recipeService.deleteRecipe(5L);

        verify(recipeRepository).delete(recipe);
        verify(ingredientRepository, never()).deleteById(anyLong());
    }

    @Test
    void deleteRecipe_throwsWhenRecipeNotFound() {
        when(recipeRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> recipeService.deleteRecipe(999L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("999");
    }

    @Test
    void deleteRecipe_throwsAccessDeniedForNonOwner() {
        var recipe = new Recipe();
        recipe.setId(5L);
        recipe.setOwner(ownerUser);
        recipe.setComponents(new java.util.HashSet<>());

        when(recipeRepository.findById(5L)).thenReturn(Optional.of(recipe));
        when(currentUserService.getCurrentUser()).thenReturn(otherUser);

        assertThatThrownBy(() -> recipeService.deleteRecipe(5L))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("delete");

        verify(recipeRepository, never()).delete(any());
    }
}
