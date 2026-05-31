package com.nomnomnow.nnnbackend.dto.response;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

public record ShoppingListResponse(
        Long id,
        LocalDate weekStart,
        OffsetDateTime createdAt,
        List<ShoppingListItemResponse> items
) {
}
