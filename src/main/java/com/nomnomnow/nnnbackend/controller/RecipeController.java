package com.nomnomnow.nnnbackend.controller;

import com.nomnomnow.nnnbackend.dto.request.RecipeRequest;
import com.nomnomnow.nnnbackend.dto.response.RecipeResponse;
import com.nomnomnow.nnnbackend.mapper.RecipeMapper;
import com.nomnomnow.nnnbackend.service.RecipeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/recipes")
@RequiredArgsConstructor
public class RecipeController {

    private final RecipeService recipeService;
    private final RecipeMapper recipeMapper;

    @PostMapping
    public RecipeResponse newRecipe(@Valid @RequestBody RecipeRequest request) {
        log.info("Received request: {}", request);
        var recipe = recipeService.create(request);
        return recipeMapper.toResponse(recipe);
    }

    @GetMapping
    public Page<RecipeResponse> getAllRecipes(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return recipeService.findAll(PageRequest.of(page,size))
                .map(recipeMapper::toResponse);

    }
}
