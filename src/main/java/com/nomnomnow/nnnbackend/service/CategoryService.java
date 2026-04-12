package com.nomnomnow.nnnbackend.service;

import com.nomnomnow.nnnbackend.dto.response.CategoriesResponse;
import com.nomnomnow.nnnbackend.dto.response.CategoryResponse;
import com.nomnomnow.nnnbackend.dto.response.SuperCategoryResponse;
import com.nomnomnow.nnnbackend.entity.Categories;
import com.nomnomnow.nnnbackend.entity.SuperCategories;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CategoryService {

    public CategoriesResponse getAllCategories() {
        List<SuperCategoryResponse> superCategories = SuperCategories.getAll().stream()
                .map(sc -> new SuperCategoryResponse(sc.getId(), sc.getName()))
                .toList();
        List<CategoryResponse> categories = Categories.getAll().stream()
                .map(cat -> new CategoryResponse(
                        cat.getId(),
                        cat.getName(),
                        cat.getSuperCategoryId()
                ))
                .toList();
        return new CategoriesResponse(superCategories, categories);
    }
}
