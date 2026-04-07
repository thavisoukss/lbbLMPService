package com.lbb.lmps.remote;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.lbb.lmps.dto.MemberListRequest;
import com.lbb.lmps.dto.SmartInquiryOutRequest;
import com.lbb.lmps.dto.SmartQrInfoRequest;
import io.micrometer.tracing.Tracer;
import io.micrometer.tracing.propagation.Propagator;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.nio.charset.StandardCharsets;
import java.util.Optional;

@Service
public class ApiMSmart {

    private static final Logger log = LogManager.getLogger(ApiMSmart.class);

    private static final ObjectMapper MAPPER = new ObjectMapper()
            .findAndRegisterModules()
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    @Value("${external.api.m-smart.path-root}")
    private String pathRoot;

    @Value("${external.api.m-smart.path-member-list}")
    private String pathMemberList;

    @Value("${external.api.m-smart.path-inq-out}")
    private String pathInqOut;

    @Value("${external.api.m-smart.path-qr-info}")
    private String pathQrInfo;

    private final RestClient restClient;
    private final Tracer tracer;
    private final Propagator propagator;

    public ApiMSmart(
            RestClient.Builder restClientBuilder,
            Tracer tracer,
            Propagator propagator,
            @Value("${external.api.m-smart.url}") String mSmartUrl
    ) {
        this.restClient = restClientBuilder
                .baseUrl(mSmartUrl)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();
        this.tracer = tracer;
        this.propagator = propagator;
    }

    public String callMemberList(MemberListRequest request) throws Exception {
        String uriPath = pathRoot + pathMemberList;
        log.info(":: Calling m-smart member-list at URI: {}", uriPath);
        return post(uriPath, MAPPER.writeValueAsString(request));
    }

    public String callInquiryOut(SmartInquiryOutRequest request) throws Exception {
        String uriPath = pathRoot + pathInqOut;
        log.info(":: Calling m-smart inquiry-out at URI: {}", uriPath);
        return post(uriPath, MAPPER.writeValueAsString(request));
    }

    public String callQrInfo(SmartQrInfoRequest request) throws Exception {
        String uriPath = pathRoot + pathQrInfo;
        log.info(":: Calling m-smart qr-info at URI: {}", uriPath);
        return post(uriPath, MAPPER.writeValueAsString(request));
    }

    private String post(String uriPath, String body) throws Exception {
        log.info(":: http request body: {}", body);
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
                    log.info(":: http response body: {}", responseBody);
                    return responseBody;
                });
    }
}
