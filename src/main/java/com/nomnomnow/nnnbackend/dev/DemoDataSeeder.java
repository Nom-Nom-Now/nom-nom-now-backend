package com.nomnomnow.nnnbackend.dev;

import com.nomnomnow.nnnbackend.entity.Categories;
import com.nomnomnow.nnnbackend.entity.Ingredient;
import com.nomnomnow.nnnbackend.entity.Recipe;
import com.nomnomnow.nnnbackend.entity.RecipeComponent;
import com.nomnomnow.nnnbackend.entity.SuperCategories;
import com.nomnomnow.nnnbackend.entity.Unit;
import com.nomnomnow.nnnbackend.repository.IngredientRepository;
import com.nomnomnow.nnnbackend.repository.RecipeRepository;
import com.nomnomnow.nnnbackend.user.AppUser;
import com.nomnomnow.nnnbackend.user.AppUserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Component
@Profile("dev")
@ConditionalOnProperty(prefix = "app.demo-data", name = "enabled", havingValue = "true", matchIfMissing = true)
public class DemoDataSeeder implements ApplicationRunner {

    private static final int MIN_RECIPES_PER_CATEGORY = 2;

    private final AppUserService appUserService;
    private final IngredientRepository ingredientRepository;
    private final RecipeRepository recipeRepository;
    private final String devUserEmail;
    private final String devUserName;
    private final int recipesPerCategory;

    public DemoDataSeeder(
            AppUserService appUserService,
            IngredientRepository ingredientRepository,
            RecipeRepository recipeRepository,
            @Value("${app.dev-user.email:dev@nomnomnow.local}") String devUserEmail,
            @Value("${app.dev-user.name:Local Dev User}") String devUserName,
            @Value("${app.demo-data.recipes-per-category:2}") int recipesPerCategory
    ) {
        this.appUserService = appUserService;
        this.ingredientRepository = ingredientRepository;
        this.recipeRepository = recipeRepository;
        this.devUserEmail = devUserEmail;
        this.devUserName = devUserName;
        this.recipesPerCategory = Math.max(MIN_RECIPES_PER_CATEGORY, recipesPerCategory);
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        var owner = appUserService.findOrCreateDevUser(devUserEmail, devUserName);
        var createdRecipes = 0;

        for (var category : Categories.getAll()) {
            for (int variant = 1; variant <= recipesPerCategory; variant++) {
                var recipeName = demoRecipeName(category, variant);
                if (recipeRepository.existsByName(recipeName)) {
                    continue;
                }

                recipeRepository.save(buildRecipe(owner, category, variant));
                createdRecipes++;
            }
        }

        if (createdRecipes > 0) {
            log.info("Seeded {} local demo recipes across {} categories", createdRecipes, Categories.getAll().size());
        }
    }

    private Recipe buildRecipe(AppUser owner, Categories category, int variant) {
        var categoryName = displayName(category.getName());
        var recipe = new Recipe();

        recipe.setOwner(owner);
        recipe.setName(demoRecipeName(category, variant));
        recipe.setInstructions("Prepare the " + categoryName.toLowerCase(Locale.ROOT)
                + " demo ingredients, season to taste, and serve for local testing.");
        recipe.setCookingTime(10 + ((int) category.getId() % 6 * 5) + (variant * 3));
        recipe.setPricePerPerson(250 + ((int) category.getId() % 8 * 40) + (variant * 25));
        recipe.setCategories(Set.of(category.getId()));

        recipe.getComponents().add(buildComponent(
                recipe,
                findOrCreateIngredient(categoryName + " Mix"),
                BigDecimal.valueOf(isDrink(category) ? 250 : 180),
                isDrink(category) ? Unit.MILLILITER : Unit.GRAM
        ));
        recipe.getComponents().add(buildComponent(
                recipe,
                findOrCreateIngredient(isDrink(category) ? "Ice Cubes" : "Olive Oil"),
                BigDecimal.valueOf(isDrink(category) ? 6 : 2),
                isDrink(category) ? Unit.PIECE : Unit.TABLESPOON
        ));
        recipe.getComponents().add(buildComponent(
                recipe,
                findOrCreateIngredient("Sea Salt"),
                BigDecimal.ONE,
                Unit.TEASPOON
        ));

        return recipe;
    }

    private String demoRecipeName(Categories category, int variant) {
        return "Demo " + displayName(category.getName()) + " Recipe " + variant;
    }

    private Ingredient findOrCreateIngredient(String name) {
        return ingredientRepository.findByNameIgnoreCase(name)
                .orElseGet(() -> {
                    var ingredient = new Ingredient();
                    ingredient.setName(name);
                    return ingredientRepository.save(ingredient);
                });
    }

    private RecipeComponent buildComponent(Recipe recipe, Ingredient ingredient, BigDecimal quantity, Unit unit) {
        var component = new RecipeComponent();
        component.setRecipe(recipe);
        component.setIngredient(ingredient);
        component.setQuantity(quantity);
        component.setUnit(unit);
        return component;
    }

    private boolean isDrink(Categories category) {
        return category.getSuperCategoryId() == SuperCategories.DRINKS.getId();
    }

    private String displayName(String name) {
        var spaced = name.replaceAll("([a-z])([A-Z])", "$1 $2");
        return Arrays.stream(spaced.split(" "))
                .filter(part -> !part.isBlank())
                .map(this::capitalize)
                .collect(Collectors.joining(" "));
    }

    private String capitalize(String value) {
        if (value.isBlank()) {
            return value;
        }
        return value.substring(0, 1).toUpperCase(Locale.ROOT) + value.substring(1);
    }
}
