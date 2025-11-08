package com.nomnomnow.nnnbackend.dto.request;


import jakarta.validation.constraints.NotBlank;

import java.util.List;

public record CategoryRequest(
        @NotBlank String name,
        String color,
        List<Long> recipeIds
) {

}
