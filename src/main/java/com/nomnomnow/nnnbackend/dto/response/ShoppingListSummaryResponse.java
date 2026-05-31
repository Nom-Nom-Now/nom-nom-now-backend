package com.nomnomnow.nnnbackend.dto.response;

import java.time.LocalDate;
import java.time.OffsetDateTime;

public record ShoppingListSummaryResponse(
        Long id,
        LocalDate weekStart,
        OffsetDateTime createdAt,
        int itemCount
) {
}
