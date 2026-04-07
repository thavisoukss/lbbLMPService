package com.lbb.lmps.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.lbb.lmps.dto.*;
import com.lbb.lmps.dto.SmartQrInfoRequest.QrData;
import com.lbb.lmps.exception.MSmartException;
import com.lbb.lmps.remote.ApiMSmart;
import com.lbb.lmps.repository.AccountRepository;
import com.lbb.lmps.repository.SecurityQuestionRepository;
import com.lbb.lmps.service.InquiryOutService;
import com.lbb.lmps.utils.CommonInfo;
import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
    @Transactional(readOnly = true)
    public InquiryOutResponse inquiryOut(InquiryOutRequest request, String deviceId) throws Exception {
        log.info("[inquiryOut] deviceId={} toAccount={} toMember={}", deviceId, request.getToAccount(), request.getToMember());
        long start = System.currentTimeMillis();

        Claims claims = (Claims) SecurityContextHolder.getContext().getAuthentication().getDetails();
        String userId = claims.getSubject();
        String customerId = (String) claims.get("user-id");
        String mobileNo = (String) claims.get("user-phone");

        // print log output for token value
        log.info("[inquiryOut] userId={} customerId={} mobileNo={}", userId, customerId, mobileNo);

        String accountNo = accountRepository.findAccountNoByCustomerId(customerId)
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
        if (!"0000".equals(smartResponse.getResponseCode())) {
            log.warn("[inquiryOut] m-smart error | code={} msg={}", smartResponse.getResponseCode(), smartResponse.getResponseMessage());
            throw new MSmartException(smartResponse.getResponseCode(), smartResponse.getResponseMessage());
        }

        InquiryOutResponse response = new InquiryOutResponse();
        response.setStatus("success");
        response.setData(smartResponse.getData());
        response.setXNonce(UUID.randomUUID().toString());
        response.setQuestions(questions);

        log.info("[inquiryOut] status={} duration_ms={}", response.getStatus(), System.currentTimeMillis() - start);
        return response;
    }

    @Override
    @Transactional(readOnly = true)
    public InquiryOutResponse inquiryOutQr(String qr, String deviceId) throws Exception {
        log.info("[inquiryOutQr] deviceId={} qr={}", deviceId, qr);
        long start = System.currentTimeMillis();

        Claims claims = (Claims) SecurityContextHolder.getContext().getAuthentication().getDetails();
        String userId = claims.getSubject();
        String customerId = (String) claims.get("user-id");
        String mobileNo = (String) claims.get("user-phone");

        log.info("[inquiryOutQr] userId={} customerId={} mobileNo={}", userId, customerId, mobileNo);

        String accountNo = accountRepository.findAccountNoByCustomerId(customerId)
                .orElseThrow(() -> {
                    log.warn("[inquiryOutQr] no account found for customerId={}", customerId);
                    return new RuntimeException("No account found for customer: " + customerId);
                });

        List<SecurityQuestionRepository.SecurityQuestionProjection> projections =
                securityQuestionRepository.findByCustomerId(customerId);
        if (projections.isEmpty()) {
            log.warn("[inquiryOutQr] no security questions found for customerId={}", customerId);
            throw new RuntimeException("No security questions found for customer: " + customerId);
        }
        List<SecurityQuestionDto> questions = projections.stream()
                .map(p -> new SecurityQuestionDto(p.getId(), p.getDescription()))
                .toList();

        ClientInfo clientInfo = new ClientInfo();
        clientInfo.setDeviceId(deviceId);
        clientInfo.setMobileNo(mobileNo);
        clientInfo.setUserId(userId);

        SecurityContext securityContext = new SecurityContext();
        securityContext.setChannel("MOBILE");

        // Step 1: call m-smart QR info to resolve toAccount and toMember from qrString
        QrData qrData = new QrData();
        qrData.setQrString(qr);
        SmartQrInfoRequest qrInfoRequest = new SmartQrInfoRequest();
        qrInfoRequest.setClientInfo(clientInfo);
        qrInfoRequest.setSecurityContext(securityContext);
        qrInfoRequest.setData(qrData);

        String rawQrInfo = apiMSmart.callQrInfo(qrInfoRequest);
        SmartQrInfoResponse qrInfoResponse = MAPPER.readValue(rawQrInfo, SmartQrInfoResponse.class);
        if (!"0000".equals(qrInfoResponse.getResponseCode())) {
            log.warn("[inquiryOutQr] m-smart QR info error | code={} msg={}", qrInfoResponse.getResponseCode(), qrInfoResponse.getResponseMessage());
            throw new MSmartException(qrInfoResponse.getResponseCode(), qrInfoResponse.getResponseMessage());
        }
        QrInfoData qrInfo = qrInfoResponse.getData();
        log.info("[inquiryOutQr] qrInfo receiverId={} memberId={}", qrInfo.getReceiverId(), qrInfo.getMemberId());

        // Step 2: call m-smart inquiry-out using resolved account/member from QR info
        String txnId = commonInfo.genTransactionId("");

        SmartInquiryDataRequest data = new SmartInquiryDataRequest();
        data.setTxnId(txnId);
        data.setFromuser(userId);
        data.setFromaccount(accountNo);
        data.setFromCif(customerId);
        data.setToType("QR");
        data.setToaccount(qr);
        data.setTomember(qrInfo.getMemberId());

        SmartInquiryOutRequest smartRequest = new SmartInquiryOutRequest();
        smartRequest.setClientInfo(clientInfo);
        smartRequest.setSecurityContext(securityContext);
        smartRequest.setData(data);

        String rawResponse = apiMSmart.callInquiryOut(smartRequest);
        SmartInquiryOutResponse smartResponse = MAPPER.readValue(rawResponse, SmartInquiryOutResponse.class);
        if (!"0000".equals(smartResponse.getResponseCode())) {
            log.warn("[inquiryOutQr] m-smart inquiry error | code={} msg={}", smartResponse.getResponseCode(), smartResponse.getResponseMessage());
            throw new MSmartException(smartResponse.getResponseCode(), smartResponse.getResponseMessage());
        }

        // Merge QR-specific fields into the inquiry data
        InquiryOutData inquiryData = smartResponse.getData() != null ? smartResponse.getData() : new InquiryOutData();
        inquiryData.setTxnCurrency(qrInfo.getTxnCurrency());
        inquiryData.setPurposeOfTxn(qrInfo.getPurposeOfTxn());
        inquiryData.setCity(qrInfo.getCity());

        InquiryOutResponse response = new InquiryOutResponse();
        response.setStatus("success");
        response.setData(inquiryData);
        response.setXNonce(UUID.randomUUID().toString());
        response.setQuestions(questions);

        log.info("[inquiryOutQr] status={} duration_ms={}", response.getStatus(), System.currentTimeMillis() - start);
        return response;
    }
}