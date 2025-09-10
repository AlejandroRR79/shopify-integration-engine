package com.creditienda.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
public class SecurityConfig {

    private final AccessTokenFilter accessTokenFilter;

    public SecurityConfig(AccessTokenFilter accessTokenFilter) {
        this.accessTokenFilter = accessTokenFilter;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf().disable()
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/public/**").permitAll()
                        .requestMatchers("/webhook/**").permitAll() // ✅ Permitir acceso público al webhook
                        .requestMatchers("/api/secure/**").authenticated()
                        .requestMatchers("/api/shopify/secure/**").authenticated()
                        .anyRequest().denyAll())
                .addFilterBefore(accessTokenFilter, UsernamePasswordAuthenticationFilter.class)
                .httpBasic().disable()
                .formLogin().disable();

        return http.build();
    }
}