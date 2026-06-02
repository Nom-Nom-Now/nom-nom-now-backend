package com.nomnomnow.nnnbackend.user;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.OffsetDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AuthControllerTest {

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void meReturnsCurrentUserWithCreationDate() {
        var currentUserService = mock(CurrentUserService.class);
        var accountService = mock(AccountService.class);
        var controller = new AuthController(currentUserService, accountService);
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

    @Test
    void logoutInvalidatesSessionAndClearsSecurityContext() {
        var currentUserService = mock(CurrentUserService.class);
        var accountService = mock(AccountService.class);
        var controller = new AuthController(currentUserService, accountService);
        var request = new MockHttpServletRequest();
        request.getSession(true);
        SecurityContextHolder.getContext().setAuthentication(
                new TestingAuthenticationToken("user", "credentials")
        );

        controller.logout(request);

        assertThat(request.getSession(false)).isNull();
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    void deleteAccountDeletesCurrentUserAccountAndInvalidatesSession() {
        var currentUserService = mock(CurrentUserService.class);
        var accountService = mock(AccountService.class);
        var controller = new AuthController(currentUserService, accountService);
        var request = new MockHttpServletRequest();
        request.getSession(true);
        SecurityContextHolder.getContext().setAuthentication(
                new TestingAuthenticationToken("user", "credentials")
        );

        controller.deleteAccount(request);

        verify(accountService).deleteCurrentUserAccount();
        assertThat(request.getSession(false)).isNull();
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }
}
