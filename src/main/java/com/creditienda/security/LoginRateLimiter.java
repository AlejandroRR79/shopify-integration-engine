package com.creditienda.security;

import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class LoginRateLimiter {

    private static final Logger log = LoggerFactory.getLogger(LoginRateLimiter.class);

    @Value("${auth.rate-limit.max-attempts:5}")
    private int maxAttempts;

    @Value("${auth.rate-limit.window-minutes:5}")
    private long windowMinutes;

    @Value("${auth.rate-limit.block-minutes:15}")
    private long blockMinutes;

    private final ConcurrentHashMap<String, AttemptRecord> attempts = new ConcurrentHashMap<>();

    public boolean isBlocked(String ip) {
        AttemptRecord record = attempts.get(ip);
        if (record == null) return false;

        long now = System.currentTimeMillis();

        if (record.blockedUntil > 0 && now < record.blockedUntil) {
            log.warn("[RATE-LIMIT] IP bloqueada ip={} bloqueadaHasta={}", ip,
                    new java.util.Date(record.blockedUntil));
            return true;
        }

        // Bloqueo expirado — limpiar
        if (record.blockedUntil > 0 && now >= record.blockedUntil) {
            attempts.remove(ip);
        }

        return false;
    }

    public void recordFailure(String ip) {
        long now = System.currentTimeMillis();
        long windowMs = windowMinutes * 60 * 1000L;

        AttemptRecord record = attempts.compute(ip, (key, existing) -> {
            if (existing == null) {
                AttemptRecord r = new AttemptRecord();
                r.count = 1;
                r.windowStart = now;
                return r;
            }
            // Ventana expirada — reiniciar
            if (now - existing.windowStart > windowMs) {
                existing.count = 1;
                existing.windowStart = now;
                existing.blockedUntil = 0;
                return existing;
            }
            existing.count++;
            return existing;
        });

        if (record.count >= maxAttempts) {
            record.blockedUntil = now + (blockMinutes * 60 * 1000L);
            log.warn("[RATE-LIMIT] IP bloqueada por {} intentos fallidos ip={}", record.count, ip);
        } else {
            log.warn("[RATE-LIMIT] intento fallido ip={} count={}/{}", ip, record.count, maxAttempts);
        }
    }

    public void reset(String ip) {
        attempts.remove(ip);
    }

    private static class AttemptRecord {
        volatile int count;
        volatile long windowStart;
        volatile long blockedUntil;
    }
}
