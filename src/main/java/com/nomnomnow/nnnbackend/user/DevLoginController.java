package com.nomnomnow.nnnbackend.user;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;

@RestController
@Profile("dev")
public class DevLoginController {

    @Value("${app.frontend-url}")
    private String frontendUrl;

    @GetMapping("/oauth2/authorization/google")
    public ResponseEntity<Void> devGoogleLogin() {
        var headers = new HttpHeaders();
        headers.setLocation(URI.create(frontendUrl + "/home"));
        return new ResponseEntity<>(headers, HttpStatus.FOUND);
    }
}
