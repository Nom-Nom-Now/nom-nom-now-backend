package com.nomnomnow.nnnbackend.user;

import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AppUserService {

    private final AppUserRepository appUserRepository;

    @Transactional
    public AppUser findOrCreate(OAuth2User oauth2User) {
        String googleId = oauth2User.getAttribute("sub");
        return appUserRepository.findByGoogleId(googleId)
                .orElseGet(() -> {
                    var user = new AppUser();
                    user.setGoogleId(googleId);
                    user.setEmail(oauth2User.getAttribute("email"));
                    user.setName(oauth2User.getAttribute("name"));
                    return appUserRepository.save(user);
                });
    }
}

