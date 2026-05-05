package com.nomnomnow.nnnbackend;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nomnomnow.nnnbackend.dto.request.RecipeComponentRequest;
import com.nomnomnow.nnnbackend.dto.request.RecipeRequest;
import com.nomnomnow.nnnbackend.entity.Unit;
import com.nomnomnow.nnnbackend.user.AppUser;
import com.nomnomnow.nnnbackend.user.AppUserRepository;
import com.nomnomnow.nnnbackend.repository.RecipeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.util.List;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.oauth2Login;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
class RecipeControllerTest {

    @Container
    static PostgreSQLContainer<?> postgreSQLContainer = new PostgreSQLContainer<>("postgres:16-alpine");

    @DynamicPropertySource
    static void dbProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgreSQLContainer::getJdbcUrl);
        registry.add("spring.datasource.username", postgreSQLContainer::getUsername);
        registry.add("spring.datasource.password", postgreSQLContainer::getPassword);
        registry.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver");
        registry.add("spring.security.oauth2.client.registration.google.client-id", () -> "test-client-id");
        registry.add("spring.security.oauth2.client.registration.google.client-secret", () -> "test-client-secret");
    }

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private AppUserRepository appUserRepository;

    @Autowired
    private RecipeRepository recipeRepository;

    private AppUser testUser;
    private AppUser otherUser;

    @BeforeEach
    void setUp() {
        // Clean slate — prevents data leakage between tests
        recipeRepository.deleteAll();
        appUserRepository.deleteAll();

        testUser = new AppUser();
        testUser.setGoogleId("test-google-id");
        testUser.setEmail("test@example.com");
        testUser.setName("Test User");
        testUser = appUserRepository.save(testUser);

        otherUser = new AppUser();
        otherUser.setGoogleId("other-google-id");
        otherUser.setEmail("other@example.com");
        otherUser.setName("Other User");
        otherUser = appUserRepository.save(otherUser);
    }

    private RequestPostProcessor loginAs(String sub, String name, String email) {
        return oauth2Login()
                .attributes(attrs -> {
                    attrs.put("sub", sub);
                    attrs.put("name", name);
                    attrs.put("email", email);
                });
    }

    private RecipeRequest buildRecipeRequest(String name, String instructions, int cookingTime, int pricePerPerson) {
        var component = new RecipeComponentRequest(null, "Test Ingredient", new BigDecimal("100"), Unit.GRAM);
        return new RecipeRequest(name, instructions, cookingTime, pricePerPerson, null, List.of(component));
    }

    private MvcResult createRecipe(String name) throws Exception {
        var request = buildRecipeRequest(name, "Do something", 30, 5);
        return mockMvc.perform(post("/recipes")
                        .with(loginAs(testUser.getGoogleId(), testUser.getName(), testUser.getEmail()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andReturn();
    }

    // ---- POST /recipes ----

    @Test
    void postRecipe_returns200WithCreatedRecipe() throws Exception {
        var request = buildRecipeRequest("Pasta Carbonara", "Boil pasta, fry pancetta", 25, 4);

        mockMvc.perform(post("/recipes")
                        .with(loginAs(testUser.getGoogleId(), testUser.getName(), testUser.getEmail()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Pasta Carbonara"))
                .andExpect(jsonPath("$.instructions").value("Boil pasta, fry pancetta"))
                .andExpect(jsonPath("$.cookingTime").value(25))
                .andExpect(jsonPath("$.pricePerPerson").value(4))
                .andExpect(jsonPath("$.ownerName").value("Test User"))
                .andExpect(jsonPath("$.components").isArray())
                .andExpect(jsonPath("$.components.length()").value(1))
                .andExpect(jsonPath("$.components[0].ingredientName").value("Test Ingredient"))
                .andExpect(jsonPath("$.components[0].quantity").value(100))
                .andExpect(jsonPath("$.components[0].unit").value("GRAM"));
    }

    @Test
    void postRecipe_returns400WhenNameIsBlank() throws Exception {
        var request = buildRecipeRequest("", "Instructions", 30, 5);

        mockMvc.perform(post("/recipes")
                        .with(loginAs(testUser.getGoogleId(), testUser.getName(), testUser.getEmail()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void postRecipe_returns400WhenComponentsEmpty() throws Exception {
        var request = new RecipeRequest("Soup", "Cook", 20, 5, null, List.of());

        mockMvc.perform(post("/recipes")
                        .with(loginAs(testUser.getGoogleId(), testUser.getName(), testUser.getEmail()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    // ---- GET /recipes ----

    @Test
    void getRecipes_returnsPage() throws Exception {
        createRecipe("Recipe One");
        createRecipe("Recipe Two");

        mockMvc.perform(get("/recipes")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content.length()").value(2))
                .andExpect(jsonPath("$.content[0].name").isNotEmpty());
    }

    @Test
    void getRecipes_withPagination() throws Exception {
        createRecipe("First");

        mockMvc.perform(get("/recipes")
                        .param("page", "0")
                        .param("size", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.totalElements").isNumber());
    }

    // ---- GET /recipes/{id} ----

    @Test
    void getRecipeById_returnsRecipe() throws Exception {
        MvcResult result = createRecipe("My Recipe");
        String responseBody = result.getResponse().getContentAsString();
        Long id = objectMapper.readTree(responseBody).path("id").asLong();

        mockMvc.perform(get("/recipes/" + id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id))
                .andExpect(jsonPath("$.name").value("My Recipe"));
    }

    @Test
    void getRecipeById_returns404WhenNotFound() throws Exception {
        mockMvc.perform(get("/recipes/999999"))
                .andExpect(status().isNotFound());
    }

    // ---- PUT /recipes/{id} ----

    @Test
    void putRecipe_updatesRecipe() throws Exception {
        MvcResult createResult = createRecipe("Old Name");
        String responseBody = createResult.getResponse().getContentAsString();
        Long id = objectMapper.readTree(responseBody).path("id").asLong();

        var updateRequest = new RecipeRequest("New Name", "Updated instructions", 45, 8, null, List.of(
                new RecipeComponentRequest(null, "Updated Ingredient", new BigDecimal("250"), Unit.MILLILITER)
        ));

        mockMvc.perform(put("/recipes/" + id)
                        .with(loginAs(testUser.getGoogleId(), testUser.getName(), testUser.getEmail()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id))
                .andExpect(jsonPath("$.name").value("New Name"))
                .andExpect(jsonPath("$.instructions").value("Updated instructions"))
                .andExpect(jsonPath("$.cookingTime").value(45))
                .andExpect(jsonPath("$.pricePerPerson").value(8));
    }

    @Test
    void putRecipe_returns404WhenNotFound() throws Exception {
        var request = buildRecipeRequest("X", "Y", 10, 2);

        mockMvc.perform(put("/recipes/999999")
                        .with(loginAs(testUser.getGoogleId(), testUser.getName(), testUser.getEmail()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());
    }

    @Test
    void putRecipe_returns403WhenNotOwner() throws Exception {
        MvcResult createResult = createRecipe("Owner Recipe");
        String responseBody = createResult.getResponse().getContentAsString();
        Long id = objectMapper.readTree(responseBody).path("id").asLong();

        var request = buildRecipeRequest("Stolen Recipe", "Hack", 10, 2);

        mockMvc.perform(put("/recipes/" + id)
                        .with(loginAs(otherUser.getGoogleId(), otherUser.getName(), otherUser.getEmail()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    // ---- DELETE /recipes/{id} ----

    @Test
    void deleteRecipe_returns204() throws Exception {
        MvcResult createResult = createRecipe("To Delete");
        String responseBody = createResult.getResponse().getContentAsString();
        Long id = objectMapper.readTree(responseBody).path("id").asLong();

        mockMvc.perform(delete("/recipes/" + id)
                        .with(loginAs(testUser.getGoogleId(), testUser.getName(), testUser.getEmail())))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/recipes/" + id))
                .andExpect(status().isNotFound());
    }

    @Test
    void deleteRecipe_returns404WhenNotFound() throws Exception {
        mockMvc.perform(delete("/recipes/999999")
                        .with(loginAs(testUser.getGoogleId(), testUser.getName(), testUser.getEmail())))
                .andExpect(status().isNotFound());
    }

    @Test
    void deleteRecipe_returns403WhenNotOwner() throws Exception {
        MvcResult createResult = createRecipe("Protected Recipe");
        String responseBody = createResult.getResponse().getContentAsString();
        Long id = objectMapper.readTree(responseBody).path("id").asLong();

        mockMvc.perform(delete("/recipes/" + id)
                        .with(loginAs(otherUser.getGoogleId(), otherUser.getName(), otherUser.getEmail())))
                .andExpect(status().isForbidden());
    }
}
