package com.nomnomnow.nnnbackend.service;

import com.nomnomnow.nnnbackend.dto.request.CategoryRequest;
import com.nomnomnow.nnnbackend.entity.Category;
import com.nomnomnow.nnnbackend.exception.BadRequestException;
import com.nomnomnow.nnnbackend.repository.CategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CategoryService {

    private final CategoryRepository categoryRepository;

    public Category create(CategoryRequest categoryRequest) {
        if (categoryRequest.recipeIds() == null || categoryRequest.recipeIds().isEmpty()) {
            //Create Category with recipe
            var trimmedName = categoryRequest.name().trim();

            Category category = new Category();
            category.setName(trimmedName);
            category.setColor(categoryRequest.color());

            var existingCategories = categoryRepository.findByNameIgnoreCase(trimmedName);
            if(existingCategories.isPresent()){
                throw new BadRequestException("Category with name " + trimmedName + " already exists");
            }
            return categoryRepository.save(category);

        }
        //TODO Create Category without Recipe
        return null;
    }
}
