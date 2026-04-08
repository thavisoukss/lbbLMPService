package com.lbb.lmps.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.lbb.lmps.dto.*;
import com.lbb.lmps.exception.MSmartException;
import com.lbb.lmps.remote.ApiMSmart;
import com.lbb.lmps.service.MemberListService;
import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class MemberListServiceImpl implements MemberListService {

    private static final ObjectMapper MAPPER = new ObjectMapper()
            .findAndRegisterModules()
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    private final ApiMSmart apiMSmart;

    @Override
    public MemberListResponse getMemberList(String deviceId) throws Exception {
        log.info("[getMemberList] deviceId={}", deviceId);
        long start = System.currentTimeMillis();

        Claims claims = (Claims) SecurityContextHolder.getContext().getAuthentication().getDetails();
        String userId = claims.getSubject();
        String mobileNo = (String) claims.get("user-phone");

        log.info("[getMemberList] userId={} mobileNo={}", userId, mobileNo);

        ClientInfo clientInfo = new ClientInfo();
        clientInfo.setDeviceId(deviceId);
        clientInfo.setMobileNo(mobileNo);
        clientInfo.setUserId(userId);

        SecurityContext securityContext = new SecurityContext();
        securityContext.setChannel("MOBILE");

        MemberListRequest request = new MemberListRequest();
        request.setClientInfo(clientInfo);
        request.setSecurityContext(securityContext);

        String rawResponse = apiMSmart.callMemberList(request);
        SmartMemberListResponse smartResponse = MAPPER.readValue(rawResponse, SmartMemberListResponse.class);
        if (!"0000".equals(smartResponse.getResponseCode())) {
            log.warn("[getMemberList] m-smart error | code={} msg={}", smartResponse.getResponseCode(), smartResponse.getResponseMessage());
            throw new MSmartException(smartResponse.getResponseCode(), smartResponse.getResponseMessage());
        }

        MemberListResponse response = new MemberListResponse();
        response.setData(smartResponse.getData());
        response.setStatus("success");

        log.info("[getMemberList] status={} duration_ms={}", response.getStatus(), System.currentTimeMillis() - start);
        return response;
    }
}
