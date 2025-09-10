package com.creditienda.config;

import java.io.IOException;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class AccessTokenFilter extends OncePerRequestFilter {

    @Value("${api.access.token}")
    private String expectedToken;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {

        String path = request.getRequestURI();
        String token = request.getHeader("X-Access-Token");

        System.out.println("🔐 Ruta solicitada: " + path);
        System.out.println("🔐 Token recibido: " + token);
        System.out.println("🔐 Token esperado: " + expectedToken);

        // ✅ Permitir rutas públicas y webhook
        if (isPublicPath(path)) {
            filterChain.doFilter(request, response);
            return;
        }

        // 🔐 Validar token en rutas protegidas
        if (isProtectedPath(path)) {
            if (expectedToken != null && expectedToken.equals(token)) {
                UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                        "authorized-client", null,
                        List.of(new SimpleGrantedAuthority("ROLE_USER")));

                SecurityContextHolder.getContext().setAuthentication(authentication);
                filterChain.doFilter(request, response);
                return;
            } else {
                deny(response, "Access Token inválido o ausente");
                return;
            }
        }

        // ❌ Bloquear todo lo demás
        deny(response, "Ruta no autorizada");
    }

    private boolean isPublicPath(String path) {
        return path.startsWith("/api/public") || path.startsWith("/webhook");
    }

    private boolean isProtectedPath(String path) {
        return path.startsWith("/api/secure") || path.startsWith("/api/shopify/secure");
    }

    private void deny(HttpServletResponse response, String message) throws IOException {
        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        response.setContentType("application/json");
        response.getWriter().write("{\"error\": \"" + message + "\"}");
    }
}