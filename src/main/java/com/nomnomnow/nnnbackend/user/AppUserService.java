package com.nomnomnow.nnnbackend.user;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AppUserService {

    private final AppUserRepository appUserRepository;

    @PersistenceContext
    private EntityManager entityManager;

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

    @Transactional
    public AppUser findOrCreateDevUser(String email, String name) {
        var googleId = "dev:" + email;
        var id = ((Number) entityManager.createNativeQuery("""
                        INSERT INTO app.app_user (google_id, email, name)
                        VALUES (:googleId, :email, :name)
                        ON CONFLICT (google_id)
                        DO UPDATE SET email = EXCLUDED.email, name = EXCLUDED.name
                        RETURNING id
                        """)
                .setParameter("googleId", googleId)
                .setParameter("email", email)
                .setParameter("name", name)
                .getSingleResult()).longValue();

        return appUserRepository.findById(id)
                .orElseThrow(() -> new IllegalStateException("Dev user not found"));
    }
}
