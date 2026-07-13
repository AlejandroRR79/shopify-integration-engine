package com.creditienda.controller.auth;

import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.creditienda.security.JwtUtil;
import com.creditienda.security.LoginRateLimiter;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;

@Tag(name = "Autenticacion", description = "Login y generacion de JWT")
@RestController
@RequestMapping("/auth")
public class AuthController {

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private LoginRateLimiter rateLimiter;

    @Value("${auth.users.admin.username}")
    private String adminUser;

    @Value("${auth.users.admin.password}")
    private String adminPass;

    @Operation(summary = "Login", description = "Autentica usuario y devuelve JWT Bearer. Body: {username, password}")
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody Map<String, String> body,
            HttpServletRequest request) {

        String ip = obtenerIp(request);

        if (rateLimiter.isBlocked(ip)) {
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                    .body(Map.of(
                            "error", "TOO_MANY_REQUESTS",
                            "message", "Demasiados intentos fallidos. Intenta más tarde."));
        }

        String username = body.get("username");
        String password = body.get("password");

        if (adminUser.equals(username) && adminPass.equals(password)) {
            rateLimiter.reset(ip);
            String token = jwtUtil.generarToken(username);
            return ResponseEntity.ok(Map.of("token", token));
        } else {
            rateLimiter.recordFailure(ip);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body("Credenciales inválidas");
        }
    }

    private String obtenerIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
