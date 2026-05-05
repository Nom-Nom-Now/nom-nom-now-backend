package com.nomnomnow.nnnbackend;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@Testcontainers
class CategoryControllerTest {

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
    private ApplicationContext context;

    private MockMvc mockMvc;

    @org.junit.jupiter.api.BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context).build();
    }

    @Test
    void getCategories_returns200WithCorrectStructure() throws Exception {
        // /categories is permitAll, so no auth needed
        SecurityContextHolder.clearContext();

        mockMvc.perform(get("/categories"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.superCategories").isArray())
                .andExpect(jsonPath("$.categories").isArray());
    }

    @Test
    void getCategories_superCategoriesHaveIdAndName() throws Exception {
        SecurityContextHolder.clearContext();

        mockMvc.perform(get("/categories"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.superCategories.length()").value(8))
                .andExpect(jsonPath("$.superCategories[0].id").isNumber())
                .andExpect(jsonPath("$.superCategories[0].name").isString());
    }

    @Test
    void getCategories_categoriesHaveIdNameAndSuperCategoryId() throws Exception {
        SecurityContextHolder.clearContext();

        mockMvc.perform(get("/categories"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.categories").isNotEmpty())
                .andExpect(jsonPath("$.categories[0].id").isNumber())
                .andExpect(jsonPath("$.categories[0].name").isString())
                .andExpect(jsonPath("$.categories[0].superCategoryId").isNumber());
    }

    @Test
    void getCategories_containsExpectedSeasonalCategories() throws Exception {
        SecurityContextHolder.clearContext();

        mockMvc.perform(get("/categories"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.categories[?(@.name=='spring')]").isArray())
                .andExpect(jsonPath("$.categories[?(@.name=='summer')]").isArray())
                .andExpect(jsonPath("$.categories[?(@.name=='autumn')]").isArray())
                .andExpect(jsonPath("$.categories[?(@.name=='winter')]").isArray());
    }

    @Test
    void getCategories_containsExpectedOriginCategories() throws Exception {
        SecurityContextHolder.clearContext();

        mockMvc.perform(get("/categories"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.categories[?(@.name=='italian')]").isArray())
                .andExpect(jsonPath("$.categories[?(@.name=='asian')]").isArray())
                .andExpect(jsonPath("$.categories[?(@.name=='mexican')]").isArray());
    }

    @Test
    void getCategories_totalCategoriesCount() throws Exception {
        SecurityContextHolder.clearContext();

        mockMvc.perform(get("/categories"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.categories.length()").value(73))
                .andExpect(jsonPath("$.superCategories.length()").value(8));
    }
}
