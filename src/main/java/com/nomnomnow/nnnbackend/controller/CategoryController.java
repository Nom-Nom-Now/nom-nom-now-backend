package com.nomnomnow.nnnbackend.controller;

import com.nomnomnow.nnnbackend.dto.request.CategoryRequest;
import com.nomnomnow.nnnbackend.dto.response.CategoryResponse;
import com.nomnomnow.nnnbackend.service.CategoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RequiredArgsConstructor
@RequestMapping("/category")
@RestController
public class CategoryController {

    private final CategoryService categoryService;

    @PostMapping
    public CategoryResponse createCategory(@RequestBody CategoryRequest categoryRequest) {
        var newCategory = categoryService.create(categoryRequest);
        return new CategoryResponse(newCategory.getId(), newCategory.getName(), newCategory.getColor());
    }
}
