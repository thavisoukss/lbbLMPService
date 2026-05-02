package com.lbb.lmps.remote;

import com.lbb.lmps.dto.CbsGetRateRequest;
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

    @Value("${external.api.cbs.path-root}")
    private String pathRoot;

    @Value("${external.api.cbs.path-getrate}")
    private String pathGetRate;

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

    public GoldRateResponse getRate() {
        CbsGetRateRequest request = new CbsGetRateRequest();
        request.setBranch("100");
        request.setCcy("LBI");
        request.setHistoryYn(false);
        request.setXrateType("CSG");

        String uri = pathRoot + pathGetRate;
        log.info("[ApiCoreBanking] calling CBS getRate uri={} branch={} ccy={} xrateType={}", uri, request.getBranch(), request.getCcy(), request.getXrateType());
        try {
            GoldRateResponse response = restClient.post()
                    .uri(uri)
                    .body(request)
                    .retrieve()
                    .body(GoldRateResponse.class);
            log.info("[ApiCoreBanking] CBS getRate success code={} sellRate={}", response.getCode(), response.getData().getSellRate());
            return response;
        } catch (Exception e) {
            log.error("[ApiCoreBanking] CBS getRate error: {}", e.getMessage());
            throw new RuntimeException("CBS error: " + e.getMessage());
        }
    }
}
