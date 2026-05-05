package com.nomnomnow.nnnbackend;

import com.nomnomnow.nnnbackend.user.AppUser;
import com.nomnomnow.nnnbackend.user.AppUserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;
import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
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
    private ApplicationContext context;

    private MockMvc mockMvc;

    @Autowired
    private AppUserRepository appUserRepository;

    private AppUser testUser;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context).build();

        // Create or find test user
        testUser = appUserRepository.findByGoogleId("auth-test-google-id").orElseGet(() -> {
            var user = new AppUser();
            user.setGoogleId("auth-test-google-id");
            user.setEmail("authtest@example.com");
            user.setName("Auth Test User");
            return appUserRepository.save(user);
        });
    }

    private void authenticateAs(String googleId, String name, String email) {
        Map<String, Object> attributes = Map.of(
                "sub", googleId,
                "name", name,
                "email", email
        );

        OAuth2User principal = new DefaultOAuth2User(
                List.of(new SimpleGrantedAuthority("ROLE_USER")),
                attributes,
                "sub"
        );

        OAuth2AuthenticationToken auth = new OAuth2AuthenticationToken(
                principal,
                List.of(new SimpleGrantedAuthority("ROLE_USER")),
                "google"
        );

        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    @Test
    void me_withValidOAuth2Auth_returnsUserInfo() throws Exception {
        authenticateAs(testUser.getGoogleId(), testUser.getName(), testUser.getEmail());

        mockMvc.perform(get("/auth/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(testUser.getId()))
                .andExpect(jsonPath("$.email").value("authtest@example.com"))
                .andExpect(jsonPath("$.name").value("Auth Test User"));
    }

    @Test
    void me_withMissingAuth_returns401Or403() throws Exception {
        // /auth/me requires authentication
        SecurityContextHolder.clearContext();

        mockMvc.perform(get("/auth/me"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void me_withOAuth2AuthButUnmappedGoogleId_throws500() throws Exception {
        // User exists in OAuth2 but not in the app_user table
        authenticateAs("nonexistent-google-id", "Ghost User", "ghost@example.com");

        mockMvc.perform(get("/auth/me"))
                .andExpect(status().isInternalServerError());
    }

    @Test
    void me_returnsMapWithExpectedKeys() throws Exception {
        authenticateAs(testUser.getGoogleId(), testUser.getName(), testUser.getEmail());

        mockMvc.perform(get("/auth/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isMap())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.email").isString())
                .andExpect(jsonPath("$.name").isString());
    }

    @Test
    void me_returnsCorrectEmail() throws Exception {
        authenticateAs(testUser.getGoogleId(), testUser.getName(), testUser.getEmail());

        mockMvc.perform(get("/auth/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("authtest@example.com"));
    }
}
