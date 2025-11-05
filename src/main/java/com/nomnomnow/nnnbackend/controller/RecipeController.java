package com.nomnomnow.nnnbackend.controller;

import com.nomnomnow.nnnbackend.entity.Recipe;
import com.nomnomnow.nnnbackend.repository.RecipeRepository;
import com.nomnomnow.nnnbackend.service.RecipeService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class RecipeController {

    private final RecipeRepository recipeRepository;
    private final RecipeService recipeService;

    @PostMapping("/recipe")
    Recipe newRecipe(@RequestBody Recipe recipe) {
        return recipeService.create(recipe);
    }

    @GetMapping("/recipe")
    List<Recipe> getAllRecipes() {
        return recipeRepository.findAll();
    }
}
