package com.lbb.lmps.remote;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lbb.lmps.dto.CbsGetRateRequest;
import com.lbb.lmps.dto.CbsInternalTransferRequest;
import com.lbb.lmps.dto.CbsInternalTransferResponse;
import com.lbb.lmps.dto.GoldRateResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.io.IOException;
import java.math.BigDecimal;
import java.net.HttpURLConnection;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.util.List;

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
    private final ObjectMapper objectMapper;

    public ApiCoreBanking(
            RestClient.Builder restClientBuilder,
            ObjectMapper objectMapper,
            @Value("${external.api.core-banking.url}") String cbsUrl
    ) {
        this.objectMapper = objectMapper;
        this.restClient = restClientBuilder
                .baseUrl(cbsUrl)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .requestFactory(trustAllRequestFactory())
                .build();
    }

    private SimpleClientHttpRequestFactory trustAllRequestFactory() {
        try {
            SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(null, new TrustManager[]{new X509TrustManager() {
                public void checkClientTrusted(X509Certificate[] chain, String authType) {}
                public void checkServerTrusted(X509Certificate[] chain, String authType) {}
                public X509Certificate[] getAcceptedIssuers() { return new X509Certificate[0]; }
            }}, new SecureRandom());

            return new SimpleClientHttpRequestFactory() {
                @Override
                protected void prepareConnection(HttpURLConnection conn, String httpMethod) throws IOException {
                    if (conn instanceof HttpsURLConnection httpsConn) {
                        httpsConn.setSSLSocketFactory(sslContext.getSocketFactory());
                        httpsConn.setHostnameVerifier((host, session) -> true);
                    }
                    super.prepareConnection(conn, httpMethod);
                }
            };
        } catch (Exception e) {
            throw new RuntimeException("Failed to build trust-all SSL factory for CBS", e);
        }
    }

    public GoldRateResponse getRate() {
        CbsGetRateRequest request = new CbsGetRateRequest();
        request.setBranch("100");
        request.setCcy("LBI");
        request.setHistoryYn(false);
        request.setXrateType("CSG");

        String uri = pathRoot + pathGetRate;
        log.info("[ApiCoreBanking] CBS getRate request uri={} body={}", uri, toJson(request));
        try {
            String raw = restClient.post()
                    .uri(uri)
                    .body(request)
                    .retrieve()
                    .body(String.class);
            log.info("[ApiCoreBanking] CBS getRate response: {}", raw);
            return objectMapper.readValue(raw, GoldRateResponse.class);
        } catch (Exception e) {
            log.error("[ApiCoreBanking] CBS getRate error: {}", e.getMessage());
            throw new RuntimeException("CBS error: " + e.getMessage());
        }
    }

    public CbsP2PTransferResult p2pTransfer(String transactionId, String customerId,
                                             String drAccountNo, String crAccountNo,
                                             BigDecimal goldWeight, String memo) {
        CbsInternalTransferRequest.TfrDetail detail = new CbsInternalTransferRequest.TfrDetail();
        detail.setAcctNo(drAccountNo);
        detail.setCpartyAcctNo(crAccountNo);
        detail.setCpartyAcctCcy("LBI");
        detail.setCpartyAcctStatus("A");
        detail.setEffectDate("2025-11-03T00:00:00+07:00");
        detail.setCpartyAvailBal(BigDecimal.ZERO);
        detail.setCpartyLedgerBal(BigDecimal.ZERO);
        detail.setRemCcy(true);
        detail.setAmount(goldWeight);
        detail.setEquivAmount(goldWeight);
        detail.setDrNarrative(memo);
        detail.setCrossRate(BigDecimal.ONE);
        detail.setTranDate("2025-11-03T00:00:00+07:00");
        detail.setBranch("100");

        CbsInternalTransferRequest request = new CbsInternalTransferRequest();
        request.setAcctNo(drAccountNo);
        request.setTransferMode("DR");
        request.setDebitTranType("TRW2");
        request.setCreditTranType("TRD2");
        request.setCheckTellerLimit("N");
        request.setTfrDetailList(List.of(detail));

        String uri = pathRoot + pathP2PTransfer;
        log.info("[ApiCoreBanking] CBS internalTransfer request txnId={} customerId={} uri={} body={}", transactionId, customerId, uri, toJson(request));

        CbsInternalTransferResponse response;
        try {
            String raw = restClient.post()
                    .uri(uri)
                    .body(request)
                    .retrieve()
                    .body(String.class);
            log.info("[ApiCoreBanking] CBS internalTransfer response txnId={} body={}", transactionId, raw);
            response = objectMapper.readValue(raw, CbsInternalTransferResponse.class);
        } catch (Exception e) {
            log.error("[ApiCoreBanking] CBS internalTransfer error txnId={}: {}", transactionId, e.getMessage());
            throw new RuntimeException("CBS error: " + e.getMessage());
        }

        if (!"CBS.MESSAGE.SUCCESS".equals(response.getCode())) {
            log.warn("[ApiCoreBanking] CBS internalTransfer failed txnId={} code={} message={} journalNo={}",
                    transactionId, response.getCode(), response.getMessage(), response.getJournalNo());
            throw new RuntimeException("CBS error: " + response.getCode() + " " + response.getMessage());
        }

        CbsInternalTransferResponse.TfrDetail resultDetail = response.getDetails().getTfrDetailList().getFirst();
        log.info("[ApiCoreBanking] CBS internalTransfer success txnId={} journalNo={} drSeqNo={} crSeqNo={}",
                transactionId, response.getJournalNo(), resultDetail.getDrSeqNo(), resultDetail.getCrSeqNo());

        return new CbsP2PTransferResult(
                String.valueOf(response.getJournalNo()),
                String.valueOf(response.getDetails().getSeqNo()),
                String.valueOf(resultDetail.getDrSeqNo()),
                String.valueOf(resultDetail.getCrSeqNo())
        );
    }

    private String toJson(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (Exception e) {
            return obj.toString();
        }
    }

    public record CbsP2PTransferResult(String transactionId, String slipCode, String drCbsSeqno, String crCbsSeqno) {}
}
