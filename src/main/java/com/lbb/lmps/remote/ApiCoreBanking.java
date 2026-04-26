package com.lbb.lmps.remote;

import com.lbb.lmps.dto.GoldRateResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Slf4j
@Service
public class ApiCoreBanking {

    private final RestClient restClient;

    public ApiCoreBanking(
            RestClient.Builder restClientBuilder,
            @Value("${external.api.core-banking.url}") String cbsUrl
    ) {
        this.restClient = restClientBuilder
                .baseUrl(cbsUrl)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();
    }

    public GoldRateResponse getGoldRate() {
        log.info(":: Calling CBS get-gold-rate");
        try {
            return restClient.get()
                    .uri("/gold-rate")
                    .retrieve()
                    .body(GoldRateResponse.class);
        } catch (Exception e) {
            log.error(":: !! CBS API Error - getGoldRate: {}", e.getMessage());
            throw new RuntimeException("CBS error: " + e.getMessage());
        }
    }
}
