package com.nomnomnow.nnnbackend.service;

import com.nomnomnow.nnnbackend.dto.response.CategoryResponse;
import com.nomnomnow.nnnbackend.entity.Categories;
import com.nomnomnow.nnnbackend.entity.SuperCategories;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CategoryService {

    public CategoryResponse getAllCategories() {
        List<Categories> categories = Categories.getAll();
        List<SuperCategories> superCategories = SuperCategories.getAll();
        return new CategoryResponse(superCategories, categories);
    }
}
