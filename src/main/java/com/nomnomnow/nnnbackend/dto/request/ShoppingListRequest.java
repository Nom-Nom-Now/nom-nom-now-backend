package com.nomnomnow.nnnbackend.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.util.List;

public record ShoppingListRequest(
        @NotNull LocalDate weekStart,
        List<@Valid ShoppingListDayRequest> days
) {
    public ShoppingListRequest {
        if (days == null) {
            days = List.of();
        }
    }

    public ShoppingListRequest(LocalDate weekStart) {
        this(weekStart, List.of());
    }
}
