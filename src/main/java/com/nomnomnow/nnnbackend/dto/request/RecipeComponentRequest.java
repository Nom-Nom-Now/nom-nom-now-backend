package com.nomnomnow.nnnbackend.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.nomnomnow.nnnbackend.entity.Unit;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record RecipeComponentRequest(
        Long ingredientId,
        @JsonProperty("name") @NotBlank String ingredientName,
        @NotNull @DecimalMin(value = "0.01") @Digits(integer = 8, fraction = 2) BigDecimal quantity,
        @NotNull Unit unit
) {
}
