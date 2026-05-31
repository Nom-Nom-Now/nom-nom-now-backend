package com.nomnomnow.nnnbackend.dto.request;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record ShoppingListRequest(
        @NotNull LocalDate weekStart
) {
}
