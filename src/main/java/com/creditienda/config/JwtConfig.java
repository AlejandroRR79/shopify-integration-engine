package com.creditienda.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
public class JwtConfig {

    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.expiration}")
    private long expiration;

    @Value("${jwt.issuer}")
    private String issuer;

    public String getSecret() {
        return secret;
    }

    public long getExpiration() {
        return expiration;
    }

    public String getIssuer() {
        return issuer;
    }
}