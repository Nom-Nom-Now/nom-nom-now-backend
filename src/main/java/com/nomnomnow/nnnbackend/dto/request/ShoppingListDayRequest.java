package com.nomnomnow.nnnbackend.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.time.LocalDate;

public record ShoppingListDayRequest(
        @NotNull LocalDate planDate,
        @NotNull @Positive Integer peopleCount
) {
}
