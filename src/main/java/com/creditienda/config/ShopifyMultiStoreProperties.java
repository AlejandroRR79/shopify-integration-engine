package com.creditienda.config;

import java.util.ArrayList;
import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "shopify")
public class ShopifyMultiStoreProperties {

    private List<ShopifyStoreConfig> stores = new ArrayList<>();

    public List<ShopifyStoreConfig> getStores() {
        return stores;
    }

    public void setStores(List<ShopifyStoreConfig> stores) {
        this.stores = stores;
    }

    public static class ShopifyStoreConfig {

        private String alias;
        private String domain;
        private String apiVersion;

        /** TOKEN | OAUTH */
        private String authType;

        /** Solo si authType=TOKEN */
        private String accessToken;

        /** Solo si authType=OAUTH */
        private String clientId;
        private String clientSecret;
        private String tokenUrl;

        /** Token dedicado para sync de ordenes (sobrescribe el token principal) */
        private String ordersAccessToken;

        private int bulkChunkSize = 20;
        private boolean updatePrice = true;

        public String getAlias() { return alias; }
        public void setAlias(String alias) { this.alias = alias; }

        public String getDomain() { return domain; }
        public void setDomain(String domain) { this.domain = domain; }

        public String getApiVersion() { return apiVersion; }
        public void setApiVersion(String apiVersion) { this.apiVersion = apiVersion; }

        public String getAuthType() { return authType; }
        public void setAuthType(String authType) { this.authType = authType; }

        public String getAccessToken() { return accessToken; }
        public void setAccessToken(String accessToken) { this.accessToken = accessToken; }

        public String getClientId() { return clientId; }
        public void setClientId(String clientId) { this.clientId = clientId; }

        public String getClientSecret() { return clientSecret; }
        public void setClientSecret(String clientSecret) { this.clientSecret = clientSecret; }

        public String getTokenUrl() { return tokenUrl; }
        public void setTokenUrl(String tokenUrl) { this.tokenUrl = tokenUrl; }

        public String getOrdersAccessToken() { return ordersAccessToken; }
        public void setOrdersAccessToken(String ordersAccessToken) { this.ordersAccessToken = ordersAccessToken; }

        public int getBulkChunkSize() { return bulkChunkSize; }
        public void setBulkChunkSize(int bulkChunkSize) { this.bulkChunkSize = bulkChunkSize; }

        public boolean isUpdatePrice() { return updatePrice; }
        public void setUpdatePrice(boolean updatePrice) { this.updatePrice = updatePrice; }
    }
}
