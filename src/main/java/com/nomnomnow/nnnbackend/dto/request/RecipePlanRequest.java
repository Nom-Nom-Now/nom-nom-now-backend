package com.nomnomnow.nnnbackend.dto.request;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.List;

public record RecipePlanRequest(
        @NotNull LocalDate weekStart,

        @NotEmpty
        @Size(max = 7, message = "Cannot plan more than 7 recipes per week")
        List<@NotNull Long> recipeIds
) {
}
