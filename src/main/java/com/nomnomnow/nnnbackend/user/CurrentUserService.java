package com.nomnomnow.nnnbackend.user;

import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CurrentUserService {

    private final AppUserRepository appUserRepository;
    private final AppUserService appUserService;

    public AppUser getCurrentUser() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof AppUser user) {
            return appUserRepository.findById(user.getId())
                    .orElseThrow(() -> new IllegalStateException("User not found"));
        }
        if (auth instanceof OAuth2AuthenticationToken token) {
            return appUserService.findOrCreate(token.getPrincipal());
        }
        throw new AuthenticationCredentialsNotFoundException("Not authenticated");
    }
}
