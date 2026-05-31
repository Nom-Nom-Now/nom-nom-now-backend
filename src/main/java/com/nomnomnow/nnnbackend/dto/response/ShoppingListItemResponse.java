package com.nomnomnow.nnnbackend.dto.response;

import com.nomnomnow.nnnbackend.entity.Unit;

import java.math.BigDecimal;

public record ShoppingListItemResponse(
        String ingredientName,
        BigDecimal quantity,
        Unit unit
) {
}
