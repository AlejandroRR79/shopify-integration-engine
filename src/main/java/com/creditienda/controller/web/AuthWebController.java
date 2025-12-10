package com.creditienda.controller.web;

import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.creditienda.security.JwtUtil;

@RestController
@RequestMapping("/auth/web")
public class AuthWebController {

    private static final Logger log = LogManager.getLogger(AuthWebController.class);

    @Value("${auth.users.admin.username}")
    private String adminUser;

    @Value("${auth.users.admin.password}")
    private String adminPass;

    @Value("${auth.app.user}")
    private String appUser;

    @Value("${auth.app.pass}")
    private String appPass;

    @Autowired
    private JwtUtil jwtUtil;

    /**
     * Endpoint para autenticación de token (admin)
     */
    @PostMapping("/loginToken")
    public ResponseEntity<?> loginToken(@RequestParam String username, @RequestParam String password) {
        try {
            log.info("Intento de loginToken con usuario={}", username);

            if (adminUser.equals(username) && adminPass.equals(password)) {
                String jwt = jwtUtil.generarToken(username);
                log.info("LoginToken exitoso para usuario={}, token generado", username);
                return ResponseEntity.ok(Map.of("token", jwt));
            } else {
                log.warn("Credenciales inválidas para loginToken usuario={}", username);
                return ResponseEntity.status(401).body(Map.of("error", "Credenciales inválidas para token"));
            }
        } catch (Exception e) {
            log.error("Error en loginToken usuario={}", username, e);
            return ResponseEntity.status(500).body(Map.of("error", "Error interno en loginToken"));
        }
    }

    /**
     * Endpoint para autenticación de la app
     */
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestParam String username, @RequestParam String password) {
        try {
            log.info("Intento de login con usuario={}", username);

            if (appUser.equals(username) && appPass.equals(password)) {
                String jwt = jwtUtil.generarToken(username);
                log.info("Login exitoso para usuario={}, token generado", username);
                return ResponseEntity.ok(Map.of("token", jwt));
            } else {
                log.warn("Credenciales inválidas para usuario={}", username);
                return ResponseEntity.status(401).body(Map.of("error", "Usuario o contraseña incorrectos"));
            }
        } catch (Exception e) {
            log.error("Error en login usuario={}", username, e);
            return ResponseEntity.status(500).body(Map.of("error", "Error interno en login"));
        }
    }
}