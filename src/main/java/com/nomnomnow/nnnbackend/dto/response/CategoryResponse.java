package com.nomnomnow.nnnbackend.dto.response;

public record CategoryResponse(
        Long id,
        String name,
        Long superCategoryId
) {
}
