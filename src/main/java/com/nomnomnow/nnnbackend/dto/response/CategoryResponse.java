package com.nomnomnow.nnnbackend.dto.response;

import com.nomnomnow.nnnbackend.entity.Categories;
import com.nomnomnow.nnnbackend.entity.SuperCategories;

import java.util.List;

public record CategoryResponse(
        List<SuperCategories> superCategories,
        List<Categories> categories
) {
}
