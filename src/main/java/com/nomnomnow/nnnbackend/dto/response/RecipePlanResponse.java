package com.nomnomnow.nnnbackend.dto.response;

import java.time.LocalDate;

public record RecipePlanResponse(
        Long id,
        LocalDate planDate,
        RecipeResponse recipe
) {
}
