package com.nomnomnow.nnnbackend.mapper;

import com.nomnomnow.nnnbackend.dto.response.CategoryResponse;
import com.nomnomnow.nnnbackend.dto.response.RecipeComponentResponse;
import com.nomnomnow.nnnbackend.dto.response.RecipeResponse;
import com.nomnomnow.nnnbackend.entity.Category;
import com.nomnomnow.nnnbackend.entity.Recipe;
import com.nomnomnow.nnnbackend.entity.RecipeComponent;
import org.springframework.stereotype.Component;

import java.util.Comparator;

@Component
public class RecipeMapper {

    public RecipeResponse toResponse(Recipe recipe) {
        var categories = recipe.getCategories()
            .stream()
            .sorted(Comparator.comparing(Category::getName, Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER)))
            .map(category -> new CategoryResponse(category.getId(), category.getName(), category.getColor()))
            .toList();

        var components = recipe.getComponents()
            .stream()
            .sorted(Comparator.comparing(component -> component.getIngredient().getName(), Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER)))
            .map(this::toComponentResponse)
            .toList();

        return new RecipeResponse(
            recipe.getId(),
            recipe.getName(),
            recipe.getInstructions(),
            recipe.getCookingTime(),
            categories,
            components
        );
    }

    private RecipeComponentResponse toComponentResponse(RecipeComponent component) {
        var ingredient = component.getIngredient();
        return new RecipeComponentResponse(
            ingredient.getId(),
            ingredient.getName(),
            component.getQuantity(),
            component.getUnit()
        );
    }
}
