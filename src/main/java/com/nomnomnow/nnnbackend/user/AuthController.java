package com.nomnomnow.nnnbackend.user;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final CurrentUserService currentUserService;

    @GetMapping("/me")
    public AuthUserResponse me() {
        var user = currentUserService.getCurrentUser();
        return new AuthUserResponse(
                user.getId(),
                user.getEmail(),
                user.getName(),
                user.getCreatedAt()
        );
    }
}
