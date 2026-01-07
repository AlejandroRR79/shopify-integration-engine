package com.creditienda.config;

import java.io.IOException;
import java.util.Collections;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.creditienda.security.JwtUtil;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class AccessTokenFilter extends OncePerRequestFilter {

    private static final Logger log = LogManager.getLogger(AccessTokenFilter.class);

    @Autowired
    private JwtUtil jwtUtil;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {

        String path = request.getServletPath();
        log.debug("👉 Ejecutando AccessTokenFilter para ruta: {}", path);

        String authHeader = request.getHeader("Authorization");
        log.debug("🔍 Authorization header recibido: {}", authHeader);

        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            log.debug("🔐 Token detectado, iniciando validación...");

            try {
                Claims claims = jwtUtil.validarToken(token);
                String username = claims.getSubject();

                log.info("✅ Token válido. Usuario autenticado: {}", username);

                UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                        username,
                        null,
                        Collections.emptyList());

                authentication.setDetails(
                        new WebAuthenticationDetailsSource().buildDetails(request));

                SecurityContextHolder.getContext().setAuthentication(authentication);

                // 🔥 AGREGAR ESTO PARA QUE SPRING NO BORRE LA AUTENTICACIÓN
                request.setAttribute("SPRING_SECURITY_CONTEXT", SecurityContextHolder.getContext());

            } catch (JwtException e) {
                log.error("❌ Token inválido o expirado: {}", e.getMessage());
                SecurityContextHolder.clearContext();

                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.setContentType("application/json");
                response.setCharacterEncoding("UTF-8");

                response.getWriter().write("""
                            {
                              "error": "INVALID_TOKEN",
                              "message": "Access token inválido o expirado"
                            }
                        """);

                response.getWriter().flush();
                return; // 🔥 CORTA LA PETICIÓN
            }
        } else {
            log.debug("⚠ No se envió token en la cabecera Authorization.");
        }

        log.debug("➡ Continuando al siguiente filtro en la cadena...");
        filterChain.doFilter(request, response);
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) throws ServletException {

        String path = request.getServletPath();

        boolean skip = path.startsWith("/auth/")
                || path.startsWith("/api/public/")
                || path.startsWith("/api/webhook/")
                || path.startsWith("/swagger")
                || path.startsWith("/v3/api-docs")
                || path.startsWith("/resources/")
                || path.startsWith("/static/")
                || path.startsWith("/css/")
                || path.startsWith("/js/")
                || path.startsWith("/images/");

        log.debug("🔎 shouldNotFilter? ruta={} → {}", path, skip);

        return skip;
    }
}
