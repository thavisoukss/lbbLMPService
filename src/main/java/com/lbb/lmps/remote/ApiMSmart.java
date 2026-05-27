package com.lbb.lmps.remote;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lbb.lmps.dto.MemberListRequest;
import com.lbb.lmps.dto.SmartBuildQrRequest;
import com.lbb.lmps.dto.SmartInquiryOutRequest;
import com.lbb.lmps.dto.SmartQrInfoRequest;
import com.lbb.lmps.dto.SmartTransferOutRequest;
import io.micrometer.tracing.Tracer;
import io.micrometer.tracing.propagation.Propagator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.nio.charset.StandardCharsets;
import java.util.Optional;

@Slf4j
@Service
public class ApiMSmart {

    @Value("${external.api.m-smart.path-root}")
    private String pathRoot;

    @Value("${external.api.m-smart.path-member-list}")
    private String pathMemberList;

    @Value("${external.api.m-smart.path-inq-out}")
    private String pathInqOut;

    @Value("${external.api.m-smart.path-qr-info}")
    private String pathQrInfo;

    @Value("${external.api.m-smart.path-build-qr}")
    private String pathBuildQr;

    @Value("${external.api.m-smart.path-trf-out}")
    private String pathTrfOut;

    private final RestClient restClient;
    private final Tracer tracer;
    private final Propagator propagator;
    private final ObjectMapper mapper;

    public ApiMSmart(
            RestClient.Builder restClientBuilder,
            Tracer tracer,
            Propagator propagator,
            ObjectMapper mapper,
            @Value("${external.api.m-smart.url}") String mSmartUrl
    ) {
        this.restClient = restClientBuilder
                .baseUrl(mSmartUrl)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();
        this.tracer = tracer;
        this.propagator = propagator;
        this.mapper = mapper;
    }

    public String callMemberList(MemberListRequest request) throws Exception {
        String uriPath = pathRoot + pathMemberList;
        log.info(":: Calling m-smart member-list at URI: {}", uriPath);
        return post(uriPath, mapper.writeValueAsString(request));
    }

    public String callInquiryOut(SmartInquiryOutRequest request) throws Exception {
        String uriPath = pathRoot + pathInqOut;
        log.info(":: Calling m-smart inquiry-out at URI: {}", uriPath);
        return post(uriPath, mapper.writeValueAsString(request));
    }

    public String callQrInfo(SmartQrInfoRequest request) throws Exception {
        String uriPath = pathRoot + pathQrInfo;
        log.info(":: Calling m-smart qr-info at URI: {}", uriPath);
        return post(uriPath, mapper.writeValueAsString(request));
    }

    public String callBuildQr(SmartBuildQrRequest request) throws Exception {
        String uriPath = pathRoot + pathBuildQr;
        log.info(":: Calling m-smart build-qr at URI: {}", uriPath);
        return post(uriPath, mapper.writeValueAsString(request));
    }

    public String callTransferOut(SmartTransferOutRequest request) throws Exception {
        String uriPath = pathRoot + pathTrfOut;
        log.info(":: Calling m-smart transfer-out at URI: {}", uriPath);
        return post(uriPath, mapper.writeValueAsString(request));
    }

    private String post(String uriPath, String body) throws Exception {
        return restClient
                .post()
                .uri(uriPath)
                .headers(headers -> {
                    headers.set(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);
                    Optional.ofNullable(tracer.currentSpan())
                            .ifPresent(span -> propagator.inject(span.context(), headers, HttpHeaders::add));
                })
                .body(body)
                .exchange((req, response) -> {
                    String responseBody = new String(response.getBody().readAllBytes(), StandardCharsets.UTF_8);
                    if (response.getStatusCode().isError()) {
                        log.error(":: !! m-smart API Error - Status: {}, Body: {}", response.getStatusCode(), responseBody);
                        throw new RuntimeException("m-smart error: " + response.getStatusCode() + " | " + responseBody);
                    }
                    log.info(":: m-smart API Success - Status: {}", response.getStatusCode());
                    return responseBody;
                });
    }
}
