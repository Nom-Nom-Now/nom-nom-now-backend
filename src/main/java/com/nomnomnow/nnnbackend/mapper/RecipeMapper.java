package com.nomnomnow.nnnbackend.mapper;

import com.nomnomnow.nnnbackend.dto.response.RecipeComponentResponse;
import com.nomnomnow.nnnbackend.dto.response.RecipeResponse;
import com.nomnomnow.nnnbackend.entity.Categories;
import com.nomnomnow.nnnbackend.entity.Recipe;
import com.nomnomnow.nnnbackend.entity.RecipeComponent;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Comparator;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class RecipeMapper {

    private static final Map<Long, String> CATEGORY_NAMES_BY_ID = Categories.getAll().stream()
            .collect(Collectors.toMap(Categories::getId, Categories::getName));

    public RecipeResponse toResponse(Recipe recipe) {

        var components = recipe.getComponents()
                .stream()
                .sorted(Comparator.comparing(component -> component.getIngredient().getName(), Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER)))
                .map(this::toComponentResponse)
                .toList();

        var ownerName = recipe.getOwner() != null ? recipe.getOwner().getName() : null;
        var imageUrl = recipe.getImageData() != null && recipe.getImageData().length > 0
                ? "/recipes/" + recipe.getId() + "/image"
                : null;

        return new RecipeResponse(
                recipe.getId(),
                recipe.getName(),
                recipe.getInstructions(),
                recipe.getCookingTime(),
                recipe.getPricePerPerson(),
                imageUrl,
                ownerName,
                mapCategoryIdsToNames(recipe.getCategories()),
                components
        );
    }

    private String mapCategoryIdsToNames(String categories) {
        if (categories == null || categories.isBlank()) {
            return null;
        }

        return Arrays.stream(categories.split(","))
                .map(String::trim)
                .filter(categoryId -> !categoryId.isBlank())
                .map(Long::parseLong)
                .map(CATEGORY_NAMES_BY_ID::get)
                .filter(categoryName -> categoryName != null && !categoryName.isBlank())
                .collect(Collectors.joining(","));
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
