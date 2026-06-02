package com.nomnomnow.nnnbackend.user;

import com.nomnomnow.nnnbackend.repository.RecipeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AccountServiceTest {

    @Mock
    private CurrentUserService currentUserService;

    @Mock
    private RecipeRepository recipeRepository;

    @Mock
    private AppUserRepository appUserRepository;

    private AccountService accountService;

    @BeforeEach
    void setUp() {
        accountService = new AccountService(currentUserService, recipeRepository, appUserRepository);
    }

    @Test
    void deleteCurrentUserAccountDeletesRecipesBeforeDeletingUser() {
        var user = new AppUser();
        user.setId(42L);
        when(currentUserService.getCurrentUser()).thenReturn(user);

        accountService.deleteCurrentUserAccount();

        InOrder inOrder = inOrder(recipeRepository, appUserRepository);
        inOrder.verify(recipeRepository).deleteByOwner(user);
        inOrder.verify(appUserRepository).delete(user);
    }
}
