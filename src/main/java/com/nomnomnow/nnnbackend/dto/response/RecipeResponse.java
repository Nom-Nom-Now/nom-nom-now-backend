package com.nomnomnow.nnnbackend.dto.response;

import java.util.List;

public record RecipeResponse(
        Long id,
        String name,
        String instructions,
        Integer cookingTime,
        String ownerName,
        String categories,
        List<RecipeComponentResponse> components
) {
}
