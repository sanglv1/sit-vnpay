package com.vnpay.sit.runner;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Map;

@Component
public class CallbackHttpRunner {

    private static final String VNPAY_SANDBOX_IP = "113.160.92.202";

    private final RestTemplate restTemplate;
    private final boolean simulateVnPayIp;

    public CallbackHttpRunner(
            @Value("${sit.callback-timeout-ms:15000}") int timeoutMs,
            @Value("${sit.simulate-vnpay-ip:true}") boolean simulateVnPayIp
    ) {
        this.simulateVnPayIp = simulateVnPayIp;
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(timeoutMs);
        factory.setReadTimeout(timeoutMs);
        this.restTemplate = new RestTemplate(factory);
    }

    public CallbackResponse execute(String baseUrl, Map<String, String> params, boolean asIpn) {
        long start = System.currentTimeMillis();
        URI uri = buildRequestUri(baseUrl, params);
        String requestUrl = uri.toString();
        try {
            HttpHeaders headers = new HttpHeaders();
            if (asIpn && simulateVnPayIp) {
                headers.set("X-Forwarded-For", VNPAY_SANDBOX_IP);
            }
            HttpEntity<Void> entity = new HttpEntity<>(headers);

            ResponseEntity<String> response = restTemplate.exchange(uri, HttpMethod.GET, entity, String.class);
            long duration = System.currentTimeMillis() - start;
            return new CallbackResponse(
                    requestUrl,
                    response.getStatusCode().value(),
                    response.getBody(),
                    duration,
                    null
            );
        } catch (RestClientException ex) {
            long duration = System.currentTimeMillis() - start;
            return new CallbackResponse(requestUrl, 0, null, duration, ex.getMessage());
        }
    }

    private static URI buildRequestUri(String baseUrl, Map<String, String> params) {
        UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(baseUrl);
        params.forEach(builder::queryParam);
        return builder.encode(StandardCharsets.UTF_8).build().toUri();
    }

    public record CallbackResponse(
            String requestUrl,
            int httpStatus,
            String responseBody,
            long durationMs,
            String errorMessage
    ) {
        public boolean hasError() {
            return errorMessage != null && !errorMessage.isBlank();
        }
    }
}
