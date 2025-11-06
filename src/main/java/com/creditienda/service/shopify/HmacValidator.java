package com.creditienda.service.shopify;

import java.security.MessageDigest;
import java.util.Base64;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.creditienda.config.ShopifyProperties;

@Component
public class HmacValidator {

    @Autowired
    private ShopifyProperties shopify;

    public boolean validar(String rawBody, String headerHmac) {
        if (!shopify.getWebhook().isEnabled())
            return true;

        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(shopify.getWebhook().getSecret().getBytes(), "HmacSHA256"));
            byte[] hash = mac.doFinal(rawBody.getBytes());
            String calculated = Base64.getEncoder().encodeToString(hash);

            // 🔐 Comparación robusta
            return MessageDigest.isEqual(
                    Base64.getDecoder().decode(headerHmac),
                    Base64.getDecoder().decode(calculated));
        } catch (Exception e) {
            return false;
        }
    }
}