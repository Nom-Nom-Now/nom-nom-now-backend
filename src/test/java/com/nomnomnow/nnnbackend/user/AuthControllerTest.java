package com.nomnomnow.nnnbackend.user;

import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AuthControllerTest {

    @Test
    void meReturnsCurrentUserWithCreationDate() {
        var currentUserService = mock(CurrentUserService.class);
        var controller = new AuthController(currentUserService);
        var createdAt = OffsetDateTime.parse("2026-05-01T12:30:00Z");
        var user = new AppUser();
        user.setId(42L);
        user.setEmail("user@example.test");
        user.setName("Test User");
        user.setCreatedAt(createdAt);

        when(currentUserService.getCurrentUser()).thenReturn(user);

        var response = controller.me();

        assertThat(response.id()).isEqualTo(42L);
        assertThat(response.email()).isEqualTo("user@example.test");
        assertThat(response.name()).isEqualTo("Test User");
        assertThat(response.createdAt()).isEqualTo(createdAt);
    }
}
