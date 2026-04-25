package com.lbb.lmps.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lbb.lmps.dto.*;
import com.lbb.lmps.dto.SmartBuildQrRequest.BuildQrData;
import com.lbb.lmps.exception.MSmartException;
import com.lbb.lmps.exception.ResourceNotFoundException;
import com.lbb.lmps.remote.ApiMSmart;
import com.lbb.lmps.repository.AccountRepository;
import com.lbb.lmps.service.BuildQrService;
import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@RequiredArgsConstructor
@Slf4j
@Service
public class BuildQrServiceImpl implements BuildQrService {

    private static final String DR_ACCOUNT_CURRENCY = "LAK";
    private static final String DR_ACCOUNT_CCY_CODE = "418"; // ISO 4217 numeric for LAK

    private final ApiMSmart apiMSmart;
    private final ObjectMapper mapper;
    private final AccountRepository accountRepository;

    @Override
    public BuildQrResponse buildStaticQr(String deviceId) throws Exception {
        log.info("[buildStaticQr] deviceId={}", deviceId);
        long start = System.currentTimeMillis();

        Claims claims = (Claims) SecurityContextHolder.getContext().getAuthentication().getDetails();
        String userId = claims.getSubject();
        String customerId = (String) claims.get("user-id");
        String mobileNo = (String) claims.get("user-phone");
        log.info("[buildStaticQr] userId={} customerId={}", userId, customerId);

        var account = accountRepository.findCurrentLakAccount(customerId)
                .orElseThrow(() -> {
                    log.warn("[buildStaticQr] no current LAK account found customerId={}", customerId);
                    return new ResourceNotFoundException("No account found for customer: " + customerId);
                });
        log.info("[buildStaticQr] loaded account accountNo={}", account.getAccountNo());

        ClientInfo clientInfo = new ClientInfo();
        clientInfo.setDeviceId(deviceId);
        clientInfo.setMobileNo(mobileNo);
        clientInfo.setUserId(userId);

        SecurityContext securityContext = new SecurityContext();
        securityContext.setChannel("MOBILE");

        BuildQrData data = new BuildQrData();
        data.setQrFor("A");
        data.setCustAccountCcy(DR_ACCOUNT_CCY_CODE);
        data.setCustAccount(account.getAccountNo());
        data.setAdditionalCustomerDataRequest(null);

        SmartBuildQrRequest smartRequest = new SmartBuildQrRequest();
        smartRequest.setClientInfo(clientInfo);
        smartRequest.setSecurityContext(securityContext);
        smartRequest.setData(data);

        log.info("[buildStaticQr] calling m-smart build-qr accountNo={}", account.getAccountNo());
        String rawResponse = apiMSmart.callBuildQr(smartRequest);
        SmartBuildQrResponse smartResponse = mapper.readValue(rawResponse, SmartBuildQrResponse.class);
        if (!"0000".equals(smartResponse.getResponseCode())) {
            log.warn("[buildStaticQr] m-smart error code={} msg={}", smartResponse.getResponseCode(), smartResponse.getResponseMessage());
            throw new MSmartException(smartResponse.getResponseCode(), smartResponse.getResponseMessage());
        }
        log.info("[buildStaticQr] m-smart success accountNo={}", account.getAccountNo());

        BuildQrResponse.BuildQrData responseData = new BuildQrResponse.BuildQrData();
        responseData.setAccountName(account.getAccountName());
        responseData.setAccountNo(account.getAccountNo());
        responseData.setCurrency(DR_ACCOUNT_CURRENCY);
        responseData.setQrString(smartResponse.getData().getQrString());

        BuildQrResponse response = new BuildQrResponse();
        response.setStatus("success");
        response.setData(responseData);

        log.info("[buildStaticQr] completed accountNo={} duration_ms={}", account.getAccountNo(), System.currentTimeMillis() - start);
        return response;
    }
}