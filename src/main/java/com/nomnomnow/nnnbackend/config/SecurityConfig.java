package com.nomnomnow.nnnbackend.config;

import com.nomnomnow.nnnbackend.user.AppUserService;
import com.nomnomnow.nnnbackend.user.DevAuthenticationFilter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AnonymousAuthenticationFilter;

import java.util.Arrays;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Value("${app.frontend-url}")
    private String frontendUrl;

    @Bean
    public SecurityFilterChain filterChain(
            HttpSecurity http,
            AppUserService appUserService,
            Environment environment,
            ObjectProvider<DevAuthenticationFilter> devAuthenticationFilter
    ) throws Exception {
        if (isDevProfile(environment)) {
            http
                    .csrf(AbstractHttpConfigurer::disable)
                    .cors(Customizer.withDefaults())
                    .authorizeHttpRequests(auth -> auth.anyRequest().permitAll())
                    .oauth2Login(AbstractHttpConfigurer::disable)
                    .formLogin(AbstractHttpConfigurer::disable)
                    .httpBasic(AbstractHttpConfigurer::disable)
                    .logout(AbstractHttpConfigurer::disable)
                    .addFilterBefore(devAuthenticationFilter.getObject(), AnonymousAuthenticationFilter.class);

            return http.build();
        }

        http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(Customizer.withDefaults())
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/swagger-ui/**", "/swagger-ui.html", "/v3/api-docs/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/recipes/**", "/categories/**").permitAll()
                        .anyRequest().authenticated()
                )
                .oauth2Login(oauth2 -> oauth2
                        .userInfoEndpoint(userInfo -> userInfo
                                .userService(oAuth2UserService(appUserService))
                        )
                        .defaultSuccessUrl(frontendUrl + "/home", true)
                );
        return http.build();
    }

    private boolean isDevProfile(Environment environment) {
        return Arrays.asList(environment.getActiveProfiles()).contains("dev");
    }

    private OAuth2UserService<OAuth2UserRequest, OAuth2User> oAuth2UserService(AppUserService appUserService) {
        var delegate = new DefaultOAuth2UserService();
        return request -> {
            var oauth2User = delegate.loadUser(request);
            appUserService.findOrCreate(oauth2User);
            return oauth2User;
        };
    }
}

