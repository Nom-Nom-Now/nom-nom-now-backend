package com.nomnomnow.nnnbackend;

import com.nomnomnow.nnnbackend.user.AppUser;
import com.nomnomnow.nnnbackend.user.AppUserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.oauth2Login;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
class AuthIntegrationTest {

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

    @Autowired
    private AppUserRepository appUserRepository;

    private AppUser testUser;

    @BeforeEach
    void setUp() {
        testUser = appUserRepository.findByGoogleId("auth-test-google-id").orElseGet(() -> {
            var user = new AppUser();
            user.setGoogleId("auth-test-google-id");
            user.setEmail("authtest@example.com");
            user.setName("Auth Test User");
            return appUserRepository.save(user);
        });
    }

    private RequestPostProcessor loginAs(String sub, String name, String email) {
        return oauth2Login()
                .attributes(attrs -> {
                    attrs.put("sub", sub);
                    attrs.put("name", name);
                    attrs.put("email", email);
                });
    }

    @Test
    void me_withValidOAuth2Auth_returnsUserInfo() throws Exception {
        mockMvc.perform(get("/auth/me")
                        .with(loginAs(testUser.getGoogleId(), testUser.getName(), testUser.getEmail())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(testUser.getId()))
                .andExpect(jsonPath("$.email").value("authtest@example.com"))
                .andExpect(jsonPath("$.name").value("Auth Test User"));
    }

    @Test
    void me_withMissingAuth_returns401Or403() throws Exception {
        mockMvc.perform(get("/auth/me"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void me_withOAuth2AuthButUnmappedGoogleId_throws500() throws Exception {
        mockMvc.perform(get("/auth/me")
                        .with(loginAs("nonexistent-google-id", "Ghost User", "ghost@example.com")))
                .andExpect(status().isInternalServerError());
    }

    @Test
    void me_returnsMapWithExpectedKeys() throws Exception {
        mockMvc.perform(get("/auth/me")
                        .with(loginAs(testUser.getGoogleId(), testUser.getName(), testUser.getEmail())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isMap())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.email").isString())
                .andExpect(jsonPath("$.name").isString());
    }

    @Test
    void me_returnsCorrectEmail() throws Exception {
        mockMvc.perform(get("/auth/me")
                        .with(loginAs(testUser.getGoogleId(), testUser.getName(), testUser.getEmail())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("authtest@example.com"));
    }
}
