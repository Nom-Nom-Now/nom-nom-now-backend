package com.nomnomnow.nnnbackend.dto.response;

import java.util.List;

public record RecipeResponse(
        Long id,
        String name,
        String instructions,
        Integer cookingTime,
        Integer pricePerPerson,
        String imageUrl,
        String ownerName,
        String categories,
        List<RecipeComponentResponse> components
) {
}
