package com.nomnomnow.nnnbackend.dev;

import com.nomnomnow.nnnbackend.entity.Categories;
import com.nomnomnow.nnnbackend.entity.Ingredient;
import com.nomnomnow.nnnbackend.entity.Recipe;
import com.nomnomnow.nnnbackend.repository.IngredientRepository;
import com.nomnomnow.nnnbackend.repository.RecipeRepository;
import com.nomnomnow.nnnbackend.user.AppUser;
import com.nomnomnow.nnnbackend.user.AppUserService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DemoDataSeederTest {

    @Mock
    private AppUserService appUserService;

    @Mock
    private IngredientRepository ingredientRepository;

    @Mock
    private RecipeRepository recipeRepository;

    @Test
    void seedsAtLeastTwoRecipesPerCategory() {
        when(appUserService.findOrCreateDevUser("dev@nomnomnow.local", "Local Dev User")).thenReturn(user());
        when(recipeRepository.existsByName(anyString())).thenReturn(false);
        when(ingredientRepository.findByNameIgnoreCase(anyString())).thenReturn(Optional.empty());
        when(ingredientRepository.save(any(Ingredient.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(recipeRepository.save(any(Recipe.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var seeder = new DemoDataSeeder(
                appUserService,
                ingredientRepository,
                recipeRepository,
                "dev@nomnomnow.local",
                "Local Dev User",
                1
        );

        seeder.run(null);

        var recipeCaptor = ArgumentCaptor.forClass(Recipe.class);
        verify(recipeRepository, org.mockito.Mockito.times(Categories.getAll().size() * 2)).save(recipeCaptor.capture());

        var recipesPerCategory = recipeCaptor.getAllValues().stream()
                .collect(Collectors.groupingBy(Recipe::getCategories, Collectors.counting()));

        assertThat(recipesPerCategory).hasSize(Categories.getAll().size());
        assertThat(recipesPerCategory.values()).allMatch(count -> count == 2L);
        assertThat(recipeCaptor.getAllValues()).allSatisfy(recipe -> {
            assertThat(recipe.getName()).startsWith("Demo ");
            assertThat(recipe.getOwner()).isNotNull();
            assertThat(recipe.getComponents()).hasSize(3);
        });
    }

    @Test
    void skipsAlreadySeededRecipes() {
        when(appUserService.findOrCreateDevUser("dev@nomnomnow.local", "Local Dev User")).thenReturn(user());
        when(recipeRepository.existsByName(anyString())).thenReturn(true);

        var seeder = new DemoDataSeeder(
                appUserService,
                ingredientRepository,
                recipeRepository,
                "dev@nomnomnow.local",
                "Local Dev User",
                2
        );

        seeder.run(null);

        verify(recipeRepository, never()).save(any());
        verifyNoInteractions(ingredientRepository);
    }

    private AppUser user() {
        var user = new AppUser();
        user.setId(1L);
        user.setGoogleId("dev:dev@nomnomnow.local");
        user.setEmail("dev@nomnomnow.local");
        user.setName("Local Dev User");
        return user;
    }
}
