package com.nomnomnow.nnnbackend.user;

import com.nomnomnow.nnnbackend.repository.RecipeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AccountService {

    private final CurrentUserService currentUserService;
    private final RecipeRepository recipeRepository;
    private final AppUserRepository appUserRepository;

    @Transactional
    public void deleteCurrentUserAccount() {
        var currentUser = currentUserService.getCurrentUser();

        recipeRepository.deleteByOwner(currentUser);
        appUserRepository.delete(currentUser);
    }
}
