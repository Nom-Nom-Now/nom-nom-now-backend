package com.nomnomnow.nnnbackend.controller;

import com.nomnomnow.nnnbackend.entity.Recipe;
import com.nomnomnow.nnnbackend.exception.BadRequestException;
import com.nomnomnow.nnnbackend.exception.GlobalExceptionHandler;
import com.nomnomnow.nnnbackend.mapper.RecipeMapper;
import com.nomnomnow.nnnbackend.service.RecipeService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.ArrayList;
import java.util.List;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class RecipeControllerTest {

    @Mock
    private RecipeService recipeService;

    @Mock
    private RecipeMapper recipeMapper;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .standaloneSetup(new RecipeController(recipeService, recipeMapper))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void createReturnsBadRequestBodyForDuplicateIngredients() throws Exception {
        when(recipeService.create(any()))
                .thenThrow(new BadRequestException("Use each ingredient only once per recipe: Salt"));

        mockMvc.perform(post("/recipes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validRecipeJson()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("Use each ingredient only once per recipe: Salt"))
                .andExpect(jsonPath("$.path").value("/recipes"));
    }

    @Test
    void createReturnsConflictBodyForLegacyDuplicateNameConstraint() throws Exception {
        when(recipeService.create(any()))
                .thenThrow(new DataIntegrityViolationException("duplicate", new RuntimeException("recipe_name_key")));

        mockMvc.perform(post("/recipes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validRecipeJson()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.message", containsString("old unique constraint")));
    }

    @Test
    void getUserRecipesDelegatesToServiceWithCorrectUserId() throws Exception {
        var emptyPage = new PageImpl<Recipe>(new ArrayList<>());
        when(recipeService.findByOwner(eq(42L), any(PageRequest.class))).thenReturn(emptyPage);

        mockMvc.perform(get("/recipes/user/42"));

        verify(recipeService).findByOwner(eq(42L), any(PageRequest.class));
    }

    @Test
    void getUserRecipesPassesPaginationParameters() throws Exception {
        var emptyPage = new PageImpl<Recipe>(new ArrayList<>());
        when(recipeService.findByOwner(eq(7L), any(PageRequest.class))).thenReturn(emptyPage);

        mockMvc.perform(get("/recipes/user/7")
                .param("page", "2")
                .param("size", "10"));

        verify(recipeService).findByOwner(eq(7L), eq(PageRequest.of(2, 10)));
    }

    @Test
    void getAllRecipesWithQueryDelegatesToSearch() throws Exception {
        var emptyPage = new PageImpl<Recipe>(new ArrayList<>());
        when(recipeService.search(eq("pizza"), any(PageRequest.class))).thenReturn(emptyPage);

        mockMvc.perform(get("/recipes").param("q", "pizza"));

        verify(recipeService).search(eq("pizza"), any(PageRequest.class));
    }

    @Test
    void getAllRecipesWithoutQueryDelegatesToFindAll() throws Exception {
        var emptyPage = new PageImpl<Recipe>(new ArrayList<>());
        when(recipeService.findAll(any(PageRequest.class))).thenReturn(emptyPage);

        mockMvc.perform(get("/recipes"));

        verify(recipeService).findAll(any(PageRequest.class));
    }

    @Test
    void getUserRecipesWithQueryDelegatesToSearchByOwner() throws Exception {
        var emptyPage = new PageImpl<Recipe>(new ArrayList<>());
        when(recipeService.searchByOwner(eq(42L), eq("pasta"), any(PageRequest.class))).thenReturn(emptyPage);

        mockMvc.perform(get("/recipes/user/42").param("q", "pasta"));

        verify(recipeService).searchByOwner(eq(42L), eq("pasta"), any(PageRequest.class));
    }

    @Test
    void getUserRecipesWithBlankQueryDelegatesToFindByOwner() throws Exception {
        var emptyPage = new PageImpl<Recipe>(new ArrayList<>());
        when(recipeService.findByOwner(eq(42L), any(PageRequest.class))).thenReturn(emptyPage);

        mockMvc.perform(get("/recipes/user/42").param("q", "   "));

        verify(recipeService).findByOwner(eq(42L), any(PageRequest.class));
    }

    private String validRecipeJson() {
        return """
                {
                  "name": "Pasta",
                  "instructions": "Cook it.",
                  "cookingTime": 15,
                  "categoryIds": [1],
                  "components": [
                    {
                      "name": "Salt",
                      "quantity": 1,
                      "unit": "GRAM"
                    }
                  ]
                }
                """;
    }
}
