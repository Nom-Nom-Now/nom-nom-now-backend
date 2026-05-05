package com.nomnomnow.nnnbackend;

import com.nomnomnow.nnnbackend.controller.CategoryController;
import com.nomnomnow.nnnbackend.dto.response.CategoriesResponse;
import com.nomnomnow.nnnbackend.dto.response.CategoryResponse;
import com.nomnomnow.nnnbackend.dto.response.SuperCategoryResponse;
import com.nomnomnow.nnnbackend.service.CategoryService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Lightweight slice test — no database, no full Spring context.
 * CategoryService only reads in-memory enums, so booting PostgreSQL
 * adds startup cost without increasing coverage.
 */
@WebMvcTest(CategoryController.class)
class CategoryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CategoryService categoryService;

    @Test
    void getCategories_returns200WithCorrectStructure() throws Exception {
        when(categoryService.getAllCategories()).thenReturn(
                new CategoriesResponse(List.of(), List.of())
        );

        mockMvc.perform(get("/categories"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.superCategories").isArray())
                .andExpect(jsonPath("$.categories").isArray());
    }

    @Test
    void getCategories_superCategoriesHaveIdAndName() throws Exception {
        var superCategories = List.of(
                new SuperCategoryResponse(1L, "season"),
                new SuperCategoryResponse(2L, "origin")
        );
        when(categoryService.getAllCategories()).thenReturn(
                new CategoriesResponse(superCategories, List.of())
        );

        mockMvc.perform(get("/categories"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.superCategories.length()").value(2))
                .andExpect(jsonPath("$.superCategories[0].id").value(1))
                .andExpect(jsonPath("$.superCategories[0].name").value("season"));
    }

    @Test
    void getCategories_categoriesHaveIdNameAndSuperCategoryId() throws Exception {
        var categories = List.of(
                new CategoryResponse(1L, "spring", 1L),
                new CategoryResponse(2L, "summer", 1L)
        );
        when(categoryService.getAllCategories()).thenReturn(
                new CategoriesResponse(List.of(), categories)
        );

        mockMvc.perform(get("/categories"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.categories").isNotEmpty())
                .andExpect(jsonPath("$.categories[0].id").value(1))
                .andExpect(jsonPath("$.categories[0].name").value("spring"))
                .andExpect(jsonPath("$.categories[0].superCategoryId").value(1));
    }

    @Test
    void getCategories_delegatesToService() throws Exception {
        var superCategories = List.of(new SuperCategoryResponse(1L, "season"));
        var categories = List.of(new CategoryResponse(1L, "spring", 1L));
        when(categoryService.getAllCategories()).thenReturn(
                new CategoriesResponse(superCategories, categories)
        );

        mockMvc.perform(get("/categories"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.superCategories.length()").value(1))
                .andExpect(jsonPath("$.categories.length()").value(1));
    }
}
