package com.nomnomnow.nnnbackend.dto.response;

import java.util.List;

public record CategoriesResponse(
        List<SuperCategoryResponse> superCategories,
        List<CategoryResponse> categories
) {
}
