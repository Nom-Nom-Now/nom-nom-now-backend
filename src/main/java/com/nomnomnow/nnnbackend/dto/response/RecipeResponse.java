package com.nomnomnow.nnnbackend.dto.response;

import java.util.List;

public record RecipeResponse(
        Long id,
        String name,
        String instructions,
        Integer cookingTime,
        List<CategoryResponse> categories,
        List<RecipeComponentResponse> components
) {
}
