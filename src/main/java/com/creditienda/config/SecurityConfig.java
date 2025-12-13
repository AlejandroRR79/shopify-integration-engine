package com.creditienda.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.multipart.MultipartResolver;
import org.springframework.web.multipart.support.StandardServletMultipartResolver;

@Configuration
public class SecurityConfig {

        private final AccessTokenFilter accessTokenFilter;

        public SecurityConfig(AccessTokenFilter accessTokenFilter) {
                this.accessTokenFilter = accessTokenFilter;
        }

        @Bean
        public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
                http
                                // ⬅⬅⬅ AGREGAR ESTO JUSTO AQUÍ
                                .securityContext(context -> context.requireExplicitSave(false))

                                .csrf().disable()
                                .authorizeHttpRequests(auth -> auth
                                                .requestMatchers(
                                                                "/auth/**",
                                                                "/api/public/**",
                                                                "/swagger-ui/**",
                                                                "/swagger-ui.html",
                                                                "/v3/api-docs/**",
                                                                "/webjars/**",
                                                                "/resources/**",
                                                                "/static/**",
                                                                "/public/**",
                                                                "/api/webhook/**")
                                                .permitAll()

                                                .requestMatchers("/auth/**").permitAll()
                                                .requestMatchers("/api/public/**").permitAll()
                                                .requestMatchers(
                                                                "/login.html",
                                                                "/upload.html",
                                                                "/css/**",
                                                                "/js/**",
                                                                "/images/**")
                                                .permitAll()
                                                .requestMatchers("/api/webhook/**").permitAll()

                                                .requestMatchers("/api/secure/uploadComplementoPago").authenticated()
                                                .requestMatchers("/api/secure/**").authenticated()
                                                .requestMatchers("/api/logs/**").authenticated()
                                                .requestMatchers("/api/shopify/secure/**").authenticated()
                                                .requestMatchers("/api/timbrado/secure/**").authenticated()
                                                .anyRequest().denyAll())
                                .addFilterBefore(accessTokenFilter, UsernamePasswordAuthenticationFilter.class)
                                .httpBasic().disable()
                                .formLogin().disable();

                return http.build();
        }

        @Bean
        public MultipartResolver multipartResolver() {
                return new StandardServletMultipartResolver();
        }
}
