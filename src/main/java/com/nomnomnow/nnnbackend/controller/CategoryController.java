package com.nomnomnow.nnnbackend.controller;

import com.nomnomnow.nnnbackend.dto.response.CategoryResponse;
import com.nomnomnow.nnnbackend.service.CategoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RequiredArgsConstructor
@RequestMapping("/categories")
@RestController
public class CategoryController {

    private final CategoryService categoryService;

    @GetMapping
    public CategoryResponse getAllCategories() {
        return categoryService.getAllCategories();
    }
}
