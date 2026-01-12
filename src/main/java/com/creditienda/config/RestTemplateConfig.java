package com.creditienda.config;

import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManager;
import org.apache.hc.core5.util.TimeValue;
import org.apache.hc.core5.util.Timeout;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

@Configuration
public class RestTemplateConfig {

    @Bean
    public RestTemplate restTemplate() {

        // Pool de conexiones
        PoolingHttpClientConnectionManager cm = new PoolingHttpClientConnectionManager();

        cm.setMaxTotal(50);
        cm.setDefaultMaxPerRoute(20);

        // Timeouts + desactivar Expect: 100-continue
        RequestConfig requestConfig = RequestConfig.custom()
                .setConnectTimeout(Timeout.ofSeconds(10))
                .setResponseTimeout(Timeout.ofSeconds(30))
                .setExpectContinueEnabled(false)
                .build();

        // HttpClient
        CloseableHttpClient httpClient = HttpClients.custom()
                .setConnectionManager(cm)
                .setDefaultRequestConfig(requestConfig)
                .evictExpiredConnections()
                .evictIdleConnections(TimeValue.ofSeconds(30))
                .build();

        // Factory (evita chunked)
        HttpComponentsClientHttpRequestFactory factory = new HttpComponentsClientHttpRequestFactory(httpClient);

        factory.setBufferRequestBody(true);

        return new RestTemplate(factory);
    }
}
