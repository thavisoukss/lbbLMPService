package com.lbb.lmps.remote;

import com.lbb.lmps.dto.CbsGetRateRequest;
import com.lbb.lmps.dto.GoldRateResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;

@Slf4j
@Service
public class ApiCoreBanking {

    @Value("${external.api.cbs.path-root}")
    private String pathRoot;

    @Value("${external.api.cbs.path-getrate}")
    private String pathGetRate;

    @Value("${external.api.cbs.path-p2p-transfer}")
    private String pathP2PTransfer;

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

    /**
     * MOCKUP — CBS P2P transfer endpoint details not yet confirmed.
     * Replace this implementation once CBS /p2p/transfer spec is available.
     */
    public CbsP2PTransferResult p2pTransfer(String transactionId, String customerId,
                                             String drAccountNo, String crAccountNo,
                                             BigDecimal goldWeight, String memo) {
        log.info("[ApiCoreBanking] [MOCKUP] p2pTransfer txnId={} drAccountNo={} crAccountNo={} goldWeight={}",
                transactionId, drAccountNo, crAccountNo, goldWeight);
        String slipCode = "SLP" + transactionId.substring(Math.max(0, transactionId.length() - 8));
        log.info("[ApiCoreBanking] [MOCKUP] p2pTransfer success slipCode={}", slipCode);
        return new CbsP2PTransferResult(transactionId, slipCode);
    }

    public record CbsP2PTransferResult(String transactionId, String slipCode) {}
}
