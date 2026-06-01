package com.nomnomnow.nnnbackend.dto.request;

import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record RecipeFilterRequest(
        @NotEmpty List<Long> categoryIds
) {
}

