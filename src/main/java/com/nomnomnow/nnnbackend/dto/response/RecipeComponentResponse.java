package com.nomnomnow.nnnbackend.dto.response;

import com.nomnomnow.nnnbackend.entity.Unit;

import java.math.BigDecimal;

public record RecipeComponentResponse(
        Long ingredientId,
        String ingredientName,
        BigDecimal quantity,
        Unit unit
) {
}
