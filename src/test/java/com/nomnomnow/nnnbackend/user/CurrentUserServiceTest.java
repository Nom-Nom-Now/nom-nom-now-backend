package com.nomnomnow.nnnbackend.user;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CurrentUserServiceTest {

    @Mock
    private AppUserRepository appUserRepository;

    @Mock
    private AppUserService appUserService;

    private CurrentUserService currentUserService;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.clearContext();
        currentUserService = new CurrentUserService(appUserRepository, appUserService);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void getCurrentUserReloadsAppUserPrincipalById() {
        var principal = user(10L, "google-10", "old@example.test", "Old Name");
        var hydratedUser = user(10L, "google-10", "new@example.test", "New Name");

        SecurityContextHolder.getContext().setAuthentication(
                new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(
                        principal,
                        "credentials",
                        List.of(new SimpleGrantedAuthority("ROLE_USER"))
                )
        );
        when(appUserRepository.findById(10L)).thenReturn(Optional.of(hydratedUser));

        assertThat(currentUserService.getCurrentUser()).isSameAs(hydratedUser);
    }

    @Test
    void getCurrentUserRehydratesOauthUserWhenDatabaseRowIsMissing() {
        var oauth2User = oauth2User();
        var appUser = user(20L, "google-20", "user@example.test", "OAuth User");

        SecurityContextHolder.getContext().setAuthentication(
                new OAuth2AuthenticationToken(
                        oauth2User,
                        oauth2User.getAuthorities(),
                        "google"
                )
        );
        when(appUserService.findOrCreate(oauth2User)).thenReturn(appUser);

        assertThat(currentUserService.getCurrentUser()).isSameAs(appUser);
        verify(appUserService).findOrCreate(oauth2User);
    }

    @Test
    void getCurrentUserRejectsMissingAuthentication() {
        assertThatThrownBy(() -> currentUserService.getCurrentUser())
                .isInstanceOf(AuthenticationCredentialsNotFoundException.class)
                .hasMessage("Not authenticated");
    }

    private OAuth2User oauth2User() {
        return new DefaultOAuth2User(
                List.of(new SimpleGrantedAuthority("ROLE_USER")),
                Map.of(
                        "sub", "google-20",
                        "email", "user@example.test",
                        "name", "OAuth User"
                ),
                "sub"
        );
    }

    private AppUser user(Long id, String googleId, String email, String name) {
        var user = new AppUser();
        user.setId(id);
        user.setGoogleId(googleId);
        user.setEmail(email);
        user.setName(name);
        return user;
    }
}
