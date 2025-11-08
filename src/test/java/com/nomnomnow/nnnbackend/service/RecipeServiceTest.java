package com.nomnomnow.nnnbackend.service;

import com.nomnomnow.nnnbackend.dto.request.RecipeComponentRequest;
import com.nomnomnow.nnnbackend.dto.request.RecipeRequest;
import com.nomnomnow.nnnbackend.entity.Category;
import com.nomnomnow.nnnbackend.entity.Ingredient;
import com.nomnomnow.nnnbackend.entity.Recipe;
import com.nomnomnow.nnnbackend.entity.Unit;
import com.nomnomnow.nnnbackend.exception.ResourceNotFoundException;
import com.nomnomnow.nnnbackend.repository.CategoryRepository;
import com.nomnomnow.nnnbackend.repository.IngredientRepository;
import com.nomnomnow.nnnbackend.repository.RecipeRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RecipeServiceTest {

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private IngredientRepository ingredientRepository;

    @Mock
    private RecipeRepository recipeRepository;

    @InjectMocks
    private RecipeService recipeService;

    @Test
    void create_withExistingCategoriesAndMixedIngredients_buildsRecipeAndCachesNewIngredient() {
        var categoryOne = new Category();
        categoryOne.setId(1L);
        categoryOne.setName("Dinner");

        var categoryTwo = new Category();
        categoryTwo.setId(2L);
        categoryTwo.setName("Snack");

        var rice = new Ingredient();
        rice.setId(10L);
        rice.setName("Rice");

        var sugar = new Ingredient();
        sugar.setId(11L);
        sugar.setName("Sugar");

        var componentRequests = List.of(
                new RecipeComponentRequest(10L, "Rice", new BigDecimal("100.00"), Unit.GRAM),
                new RecipeComponentRequest(null, "  Sugar  ", new BigDecimal("2.00"), Unit.TEASPOON),
                new RecipeComponentRequest(null, "Sugar", new BigDecimal("1.00"), Unit.TEASPOON)
        );
        var request = new RecipeRequest(
                " Sushi ",
                "Mix everything",
                15,
                Set.of(1L, 2L),
                componentRequests
        );

        when(categoryRepository.findAllById(Set.of(1L, 2L))).thenReturn(List.of(categoryOne, categoryTwo));
        when(ingredientRepository.findAllById(Set.of(10L))).thenReturn(List.of(rice));
        when(ingredientRepository.findByNameIgnoreCase("Sugar")).thenReturn(Optional.empty());
        when(ingredientRepository.save(any(Ingredient.class))).thenReturn(sugar);
        when(recipeRepository.save(any(Recipe.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var recipe = recipeService.create(request);

        assertThat(recipe.getName()).isEqualTo("Sushi");
        assertThat(recipe.getCategories()).containsExactlyInAnyOrder(categoryOne, categoryTwo);
        assertThat(recipe.getComponents()).hasSize(3);
        assertThat(recipe.getComponents())
                .allSatisfy(component -> assertThat(component.getRecipe()).isSameAs(recipe));
        assertThat(recipe.getComponents())
                .filteredOn(component -> component.getIngredient().equals(rice))
                .singleElement()
                .satisfies(component -> {
                    assertThat(component.getQuantity()).isEqualByComparingTo("100.00");
                    assertThat(component.getUnit()).isEqualTo(Unit.GRAM);
                });
        assertThat(recipe.getComponents())
                .filteredOn(component -> component.getIngredient().equals(sugar))
                .hasSize(2);

        verify(ingredientRepository, times(1)).findByNameIgnoreCase("Sugar");
        verify(ingredientRepository, times(1)).save(any(Ingredient.class));
        verify(recipeRepository).save(any(Recipe.class));
    }

    @Test
    void create_whenCategoryIdMissing_throwsResourceNotFoundException() {
        var request = new RecipeRequest(
                "Soup",
                null,
                30,
                Set.of(9L),
                List.of(new RecipeComponentRequest(null, "Water", BigDecimal.ONE, Unit.LITER))
        );

        when(categoryRepository.findAllById(anyCollection())).thenReturn(List.of());

        assertThatThrownBy(() -> recipeService.create(request))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Unknown category ids: [9]");

        verify(recipeRepository, never()).save(any(Recipe.class));
        verify(ingredientRepository, never()).findAllById(anyCollection());
    }

    @Test
    void create_whenIngredientIdMissing_throwsResourceNotFoundException() {
        var request = new RecipeRequest(
                "Tea",
                null,
                5,
                Set.of(),
                List.of(new RecipeComponentRequest(55L, "Lemon", BigDecimal.ONE, Unit.PIECE))
        );

        when(ingredientRepository.findAllById(Set.of(55L))).thenReturn(List.of());

        assertThatThrownBy(() -> recipeService.create(request))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Unknown ingredient ids: [55]");

        verify(recipeRepository, never()).save(any(Recipe.class));
        verify(ingredientRepository, never()).findByNameIgnoreCase(anyString());
        verify(ingredientRepository, never()).save(any(Ingredient.class));
    }
}
