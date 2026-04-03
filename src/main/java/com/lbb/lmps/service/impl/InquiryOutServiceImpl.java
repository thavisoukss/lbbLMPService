package com.lbb.lmps.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.lbb.lmps.dto.*;
import com.lbb.lmps.remote.ApiMSmart;
import com.lbb.lmps.entity.Account;
import com.lbb.lmps.repository.AccountRepository;
import com.lbb.lmps.repository.SecurityQuestionRepository;
import com.lbb.lmps.service.InquiryOutService;
import com.lbb.lmps.utils.CommonInfo;
import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@RequiredArgsConstructor
@Slf4j
@Service
public class InquiryOutServiceImpl implements InquiryOutService {

    private static final ObjectMapper MAPPER = new ObjectMapper()
            .findAndRegisterModules()
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    private final ApiMSmart apiMSmart;
    private final AccountRepository accountRepository;
    private final SecurityQuestionRepository securityQuestionRepository;
    private final CommonInfo commonInfo;

    @Override
    public InquiryOutResponse inquiryOut(InquiryOutRequest request, String deviceId) throws Exception {
        log.info("[inquiryOut] deviceId={} toAccount={} toMember={}", deviceId, request.getToAccount(), request.getToMember());
        long start = System.currentTimeMillis();

        Claims claims = (Claims) SecurityContextHolder.getContext().getAuthentication().getDetails();
        String userId = claims.getSubject();
        String customerId = (String) claims.get("customerId");
        String mobileNo = (String) claims.get("mobileNo");

        String accountNo = accountRepository.findByCustomerId(customerId)
                .map(Account::getAccountNo)
                .orElseThrow(() -> {
                    log.warn("[inquiryOut] no account found for customerId={}", customerId);
                    return new RuntimeException("No account found for customer: " + customerId);
                });

        List<SecurityQuestionRepository.SecurityQuestionProjection> projections =
                securityQuestionRepository.findByCustomerId(customerId);
        if (projections.isEmpty()) {
            log.warn("[inquiryOut] no security questions found for customerId={}", customerId);
            throw new RuntimeException("No security questions found for customer: " + customerId);
        }
        List<SecurityQuestionDto> questions = projections.stream()
                .map(p -> new SecurityQuestionDto(p.getId(), p.getDescription()))
                .toList();

        String txnId = commonInfo.genTransactionId("");

        ClientInfo clientInfo = new ClientInfo();
        clientInfo.setDeviceId(deviceId);
        clientInfo.setMobileNo(mobileNo);
        clientInfo.setUserId(userId);

        SecurityContext securityContext = new SecurityContext();
        securityContext.setChannel("MOBILE");

        SmartInquiryDataRequest data = new SmartInquiryDataRequest();
        data.setTxnId(txnId);
        data.setFromuser(userId);
        data.setFromaccount(accountNo);
        data.setFromCif(customerId);
        data.setToType("ACCOUNT");
        data.setToaccount(request.getToAccount());
        data.setTomember(request.getToMember());

        SmartInquiryOutRequest smartRequest = new SmartInquiryOutRequest();
        smartRequest.setClientInfo(clientInfo);
        smartRequest.setSecurityContext(securityContext);
        smartRequest.setData(data);

        String rawResponse = apiMSmart.callInquiryOut(smartRequest);
        SmartInquiryOutResponse smartResponse = MAPPER.readValue(rawResponse, SmartInquiryOutResponse.class);

        InquiryOutResponse response = new InquiryOutResponse();
        response.setStatus("SUCCESS".equalsIgnoreCase(smartResponse.getResponseStatus()) ? "success" : "failed");
        response.setData(smartResponse.getData());
        response.setXNonce(UUID.randomUUID().toString());
        response.setQuestions(questions);

        log.info("[inquiryOut] status={} duration_ms={}", response.getStatus(), System.currentTimeMillis() - start);
        return response;
    }
}