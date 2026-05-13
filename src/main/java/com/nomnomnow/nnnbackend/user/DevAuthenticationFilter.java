package com.nomnomnow.nnnbackend.user;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Component
@Profile("dev")
@RequiredArgsConstructor
public class DevAuthenticationFilter extends OncePerRequestFilter {

    private final AppUserService appUserService;

    @Value("${app.dev-user.email:dev@nomnomnow.local}")
    private String devUserEmail;

    @Value("${app.dev-user.name:Local Dev User}")
    private String devUserName;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        var context = SecurityContextHolder.getContext();

        if (context.getAuthentication() == null) {
            var user = appUserService.findOrCreateDevUser(devUserEmail, devUserName);
            var auth = new UsernamePasswordAuthenticationToken(
                    user,
                    "dev-profile",
                    List.of(new SimpleGrantedAuthority("ROLE_USER"))
            );
            auth.setDetails(request);
            context.setAuthentication(auth);
        }

        filterChain.doFilter(request, response);
    }
}
