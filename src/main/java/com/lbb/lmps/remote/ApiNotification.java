package com.lbb.lmps.remote;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lbb.lmps.dto.NotificationRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

@Slf4j
@Service
public class ApiNotification {

    @Value("${external.api.notification.path}")
    private String path;

    @Value("${external.api.notification.api-key}")
    private String apiKey;

    private final RestClient restClient;
    private final ObjectMapper mapper;

    public ApiNotification(
            RestClient.Builder restClientBuilder,
            ObjectMapper mapper,
            @Value("${external.api.notification.url}") String url
    ) {
        this.restClient = restClientBuilder
                .baseUrl(url)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();
        this.mapper = mapper;
    }

    public void send(String title, String desc, String phone) throws Exception {
        String nonce = UUID.randomUUID().toString();
        String body = mapper.writeValueAsString(new NotificationRequest(title, desc, phone));
        log.info("[ApiNotification] sending title={} phone={} nonce={}", title, phone, nonce);

        restClient
                .post()
                .uri(path)
                .headers(headers -> {
                    headers.set("API-KEY", apiKey);
                    headers.set("X-Nonce", nonce);
                    headers.set(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);
                })
                .body(body)
                .exchange((req, response) -> {
                    String responseBody = new String(response.getBody().readAllBytes(), StandardCharsets.UTF_8);
                    log.info("[ApiNotification] response status={} body={}", response.getStatusCode(), responseBody);
                    return responseBody;
                });
    }
}