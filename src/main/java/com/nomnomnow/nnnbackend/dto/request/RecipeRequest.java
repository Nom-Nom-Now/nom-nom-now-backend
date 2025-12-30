package com.nomnomnow.nnnbackend.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.PositiveOrZero;

import java.util.List;
import java.util.Set;

public record RecipeRequest(
        @NotBlank String name,
        String instructions,
        @PositiveOrZero Integer cookingTime,
        Set<Long> categoryIds,
        @NotEmpty List<@Valid RecipeComponentRequest> components
) {
}
