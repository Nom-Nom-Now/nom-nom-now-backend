package com.nomnomnow.nnnbackend;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
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
    private MockMvc mockMvc;

    @Test
    void getCategories_returns200WithCorrectStructure() throws Exception {
        mockMvc.perform(get("/categories"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.superCategories").isArray())
                .andExpect(jsonPath("$.categories").isArray());
    }

    @Test
    void getCategories_superCategoriesHaveIdAndName() throws Exception {
        mockMvc.perform(get("/categories"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.superCategories.length()").value(8))
                .andExpect(jsonPath("$.superCategories[0].id").isNumber())
                .andExpect(jsonPath("$.superCategories[0].name").isString());
    }

    @Test
    void getCategories_categoriesHaveIdNameAndSuperCategoryId() throws Exception {
        mockMvc.perform(get("/categories"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.categories").isNotEmpty())
                .andExpect(jsonPath("$.categories[0].id").isNumber())
                .andExpect(jsonPath("$.categories[0].name").isString())
                .andExpect(jsonPath("$.categories[0].superCategoryId").isNumber());
    }

    @Test
    void getCategories_delegatesToService() throws Exception {
        mockMvc.perform(get("/categories"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.superCategories.length()").value(8))
                .andExpect(jsonPath("$.categories.length()").value(73));
    }
}
