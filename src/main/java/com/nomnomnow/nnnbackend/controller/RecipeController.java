package com.nomnomnow.nnnbackend.controller;

import com.nomnomnow.nnnbackend.dto.request.RecipeRequest;
import com.nomnomnow.nnnbackend.dto.response.RecipeResponse;
import com.nomnomnow.nnnbackend.mapper.RecipeMapper;
import com.nomnomnow.nnnbackend.service.RecipeService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@RestController
@RequestMapping("/recipes")
@RequiredArgsConstructor
@Validated
public class RecipeController {

    private final RecipeService recipeService;
    private final RecipeMapper recipeMapper;

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    public RecipeResponse newRecipe(@Valid @RequestBody RecipeRequest request) {
        log.info("Received request: {}", request);
        var recipe = recipeService.create(request);
        return recipeMapper.toResponse(recipe);
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public RecipeResponse newRecipeWithImage(
            @Valid @RequestPart("recipe") RecipeRequest request,
            @RequestPart(value = "image", required = false) MultipartFile image
    ) {
        log.info("Received multipart request: {}", request);
        var recipe = recipeService.create(request, image);
        return recipeMapper.toResponse(recipe);
    }

    @PutMapping(path = "/{id}", consumes = MediaType.APPLICATION_JSON_VALUE)
    public RecipeResponse updateRecipe(
            @PathVariable long id,
            @Valid @RequestBody RecipeRequest request
    ) {
        log.info("Updating recipe {} with request: {}", id, request);
        var recipe = recipeService.updateRecipe(id, request);
        return recipeMapper.toResponse(recipe);
    }

    @PutMapping(path = "/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public RecipeResponse updateRecipeWithImage(
            @PathVariable long id,
            @Valid @RequestPart("recipe") RecipeRequest request,
            @RequestPart(value = "image", required = false) MultipartFile image
    ) {
        log.info("Updating recipe {} with multipart request: {}", id, request);
        var recipe = recipeService.updateRecipe(id, request, image);
        return recipeMapper.toResponse(recipe);
    }

    @GetMapping
    public Page<RecipeResponse> getAllRecipes(
            @RequestParam(required = false) String q,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Max(50) int size
    ) {
        var pageable = PageRequest.of(page, size);
        var recipes = (q != null && !q.isBlank())
                ? recipeService.search(q, pageable)
                : recipeService.findAll(pageable);
        return recipes.map(recipeMapper::toResponse);
    }

    @GetMapping("/user/{userId}")
    public Page<RecipeResponse> getUserRecipes(
            @PathVariable long userId,
            @RequestParam(required = false) String q,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Max(50) int size
    ) {
        var pageable = PageRequest.of(page, size);
        var recipes = (q != null && !q.isBlank())
                ? recipeService.searchByOwner(userId, q, pageable)
                : recipeService.findByOwner(userId, pageable);
        return recipes.map(recipeMapper::toResponse);
    }

    @GetMapping("/{id}/image")
    public ResponseEntity<byte[]> getRecipeImage(@PathVariable long id) {
        var recipe = recipeService.getRecipeImage(id);
        var headers = new HttpHeaders();
        var contentType = recipe.getImageContentType() != null
                ? recipe.getImageContentType()
                : MediaType.APPLICATION_OCTET_STREAM_VALUE;
        var imageData = recipe.getImageData();
        headers.setContentType(MediaType.parseMediaType(contentType));
        headers.setContentLength(recipe.getImageSize() != null ? recipe.getImageSize() : imageData.length);

        var filename = recipe.getImageFilename();
        if (filename != null && !filename.isBlank()) {
            headers.setContentDisposition(ContentDisposition.inline().filename(filename).build());
        }

        return new ResponseEntity<>(imageData, headers, HttpStatus.OK);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteRecipe(@PathVariable long id) {
        log.info("Deleting recipe {}", id);
        recipeService.deleteRecipe(id);
    }
}
