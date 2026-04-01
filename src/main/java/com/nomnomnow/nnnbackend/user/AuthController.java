package com.nomnomnow.nnnbackend.user;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final CurrentUserService currentUserService;

    @GetMapping("/me")
    public Map<String, Object> me() {
        var user = currentUserService.getCurrentUser();
        return Map.of(
                "id", user.getId(),
                "email", user.getEmail(),
                "name", user.getName()
        );
    }
}

