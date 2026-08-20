package com.vnpay.sit.runner;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

/**
 * PaymentLink IPN nghiệm thu dùng contract demo:
 * - HTTP POST
 * - Content-Type: application/json
 * - Header: vnp-signature (HMAC-SHA512 computed over rawBody)
 */
@Component
public class PaymentLinkCallbackHttpRunner {

    private final RestTemplate restTemplate;

    public PaymentLinkCallbackHttpRunner(
            @Value("${sit.callback-timeout-ms:15000}") int timeoutMs
    ) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(timeoutMs);
        factory.setReadTimeout(timeoutMs);
        this.restTemplate = new RestTemplate(factory);
    }

    public CallbackHttpRunner.CallbackResponse execute(String url, String rawBody, String signature) {
        long start = System.currentTimeMillis();
        String requestUrl = "POST " + url;
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            if (signature != null) {
                headers.set("vnp-signature", signature);
            }
            HttpEntity<String> entity = new HttpEntity<>(rawBody, headers);

            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.POST, entity, String.class);
            long duration = System.currentTimeMillis() - start;
            return new CallbackHttpRunner.CallbackResponse(
                    requestUrl,
                    response.getStatusCode().value(),
                    response.getBody(),
                    duration,
                    null
            );
        } catch (RestClientException ex) {
            long duration = System.currentTimeMillis() - start;
            return new CallbackHttpRunner.CallbackResponse(
                    requestUrl,
                    0,
                    null,
                    duration,
                    ex.getMessage()
            );
        }
    }
}

