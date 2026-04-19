package com.lbb.lmps.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.lbb.lmps.dto.*;
import com.lbb.lmps.dto.SmartQrInfoRequest.QrData;
import com.lbb.lmps.entity.WithdrawTxn;
import com.lbb.lmps.exception.MSmartException;
import com.lbb.lmps.exception.ResourceNotFoundException;
import com.lbb.lmps.remote.ApiMSmart;
import com.lbb.lmps.repository.AccountRepository;
import com.lbb.lmps.repository.CustomerRepository;
import com.lbb.lmps.repository.SecurityQuestionRepository;
import com.lbb.lmps.repository.WithdrawTxnRepository;
import com.lbb.lmps.service.InquiryOutService;
import com.lbb.lmps.utils.CommonInfo;
import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@RequiredArgsConstructor
@Slf4j
@Service
public class InquiryOutServiceImpl implements InquiryOutService {

    private static final ObjectMapper MAPPER = new ObjectMapper()
            .findAndRegisterModules()
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    // 25 = PAYMENT_CHANNEL.ID for 'Lao QR' — used for all outward QR/account transfers
    private static final long PAYMENT_CHANNEL_ID = 25L;

    private final ApiMSmart apiMSmart;
    private final AccountRepository accountRepository;
    private final CustomerRepository customerRepository;
    private final SecurityQuestionRepository securityQuestionRepository;
    private final WithdrawTxnRepository withdrawTxnRepository;
    private final CommonInfo commonInfo;

    @Override
    public InquiryOutResponse inquiryOut(InquiryOutRequest request, String deviceId) throws Exception {
        log.info("[inquiryOut] deviceId={} toAccount={} toMember={}", deviceId, request.getToAccount(), request.getToMember());
        long start = System.currentTimeMillis();

        Claims claims = (Claims) SecurityContextHolder.getContext().getAuthentication().getDetails();
        String userId = claims.getSubject();
        String customerId = (String) claims.get("user-id");
        String mobileNo = (String) claims.get("user-phone");
        log.info("[inquiryOut] userId={} customerId={} mobileNo={}", userId, customerId, mobileNo);

        CustomerContext ctx = loadCustomerContext(customerId, deviceId, userId, mobileNo, "inquiryOut");

        String txnId = commonInfo.genTransactionId("");

        SmartInquiryDataRequest data = new SmartInquiryDataRequest();
        data.setTxnId(txnId);
        data.setFromuser(userId);
        data.setFromaccount(ctx.accountNo());
        data.setFromCif(customerId);
        data.setToType("ACCOUNT");
        data.setToaccount(request.getToAccount());
        data.setTomember(request.getToMember());

        SmartInquiryOutRequest smartRequest = new SmartInquiryOutRequest();
        smartRequest.setClientInfo(ctx.clientInfo());
        smartRequest.setSecurityContext(ctx.securityContext());
        smartRequest.setData(data);

        String rawResponse = apiMSmart.callInquiryOut(smartRequest);
        SmartInquiryOutResponse smartResponse = MAPPER.readValue(rawResponse, SmartInquiryOutResponse.class);
        if (!"0000".equals(smartResponse.getResponseCode())) {
            log.warn("[inquiryOut] m-smart error | code={} msg={}", smartResponse.getResponseCode(), smartResponse.getResponseMessage());
            throw new MSmartException(smartResponse.getResponseCode(), smartResponse.getResponseMessage());
        }

        String xNonce = UUID.randomUUID().toString();
        saveInquiryRecord(customerId, ctx.customerName(), ctx.accountNo(), xNonce, smartResponse.getData(), "ACCOUNT");

        InquiryOutResponse response = new InquiryOutResponse();
        response.setStatus("success");
        response.setData(smartResponse.getData());
        response.setXNonce(xNonce);
        response.setQuestions(ctx.questions());

        log.info("[inquiryOut] status={} duration_ms={}", response.getStatus(), System.currentTimeMillis() - start);
        return response;
    }

    @Override
    public InquiryOutResponse inquiryOutQr(String qr, String deviceId) throws Exception {
        log.info("[inquiryOutQr] deviceId={} qr={}", deviceId, qr);
        long start = System.currentTimeMillis();

        Claims claims = (Claims) SecurityContextHolder.getContext().getAuthentication().getDetails();
        String userId = claims.getSubject();
        String customerId = (String) claims.get("user-id");
        String mobileNo = (String) claims.get("user-phone");
        log.info("[inquiryOutQr] userId={} customerId={} mobileNo={}", userId, customerId, mobileNo);

        CustomerContext ctx = loadCustomerContext(customerId, deviceId, userId, mobileNo, "inquiryOutQr");

        // Step 1: call m-smart QR info to resolve toAccount and toMember from qrString
        QrData qrData = new QrData();
        qrData.setQrString(qr);
        SmartQrInfoRequest qrInfoRequest = new SmartQrInfoRequest();
        qrInfoRequest.setClientInfo(ctx.clientInfo());
        qrInfoRequest.setSecurityContext(ctx.securityContext());
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
        data.setFromaccount(ctx.accountNo());
        data.setFromCif(customerId);
        data.setToType("QR");
        data.setToaccount(qr);
        data.setTomember(qrInfo.getMemberId());

        SmartInquiryOutRequest smartRequest = new SmartInquiryOutRequest();
        smartRequest.setClientInfo(ctx.clientInfo());
        smartRequest.setSecurityContext(ctx.securityContext());
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

        String xNonce = UUID.randomUUID().toString();
        saveInquiryRecord(customerId, ctx.customerName(), ctx.accountNo(), xNonce, inquiryData, "QR");

        InquiryOutResponse response = new InquiryOutResponse();
        response.setStatus("success");
        response.setData(inquiryData);
        response.setXNonce(xNonce);
        response.setQuestions(ctx.questions());

        log.info("[inquiryOutQr] status={} duration_ms={}", response.getStatus(), System.currentTimeMillis() - start);
        return response;
    }

    private void saveInquiryRecord(String customerId, String customerName, String accountNo,
                                   String nonce, InquiryOutData data, String toType) throws Exception {
        if (data.getTxnId() == null) {
            log.warn("[saveInquiryRecord] txnId is null, skipping WITHDRAW_TXN save toType={}", toType);
            return;
        }

        String ccy = (data.getAccountccy() != null && !data.getAccountccy().isBlank())
                ? data.getAccountccy() : "LAK";

        WithdrawTxn txn = new WithdrawTxn();
        txn.setPaymentChannelId(PAYMENT_CHANNEL_ID);
        txn.setCustomerId(customerId);
        txn.setTransactionId(data.getTxnId());
        txn.setNonce(nonce);
        txn.setProviderCode("LMPS");
        txn.setStatus("DEBIT_PENDING");
        txn.setDrAccountNo(accountNo);
        txn.setDrCif(customerId);
        txn.setDrAccountName(customerName);
        txn.setCrAccountNo("QR".equals(toType) ? data.getAccountname() : data.getToaccount());
        txn.setCrAccountName(data.getAccountname());
        txn.setAmount(BigDecimal.ZERO);
        txn.setFeeAmt(BigDecimal.ZERO);
        txn.setFeeProviderAmt(BigDecimal.ZERO);
        txn.setCurrencyCode(ccy);
        txn.setFeeCurrencyCode(ccy);
        txn.setFeeProviderCurrencyCode(ccy);
        // For ACCOUNT transfers: REMARK stores the recipient member code (needed at transfer time).
        // For QR transfers: REMARK stores "QR" as a type marker.
        txn.setRemark("ACCOUNT".equals(toType) ? data.getTomember() : toType);
        txn.setCreatedAt(LocalDateTime.now());
        txn.setVersion(1L);
        if (data.getFeelist() != null) {
            txn.setFeeList(MAPPER.writeValueAsString(data.getFeelist()));
        }

        withdrawTxnRepository.save(txn);
        log.info("[saveInquiryRecord] saved WITHDRAW_TXN txnId={} toType={}", data.getTxnId(), toType);
    }

    private record CustomerContext(
            String accountNo,
            String customerName,
            List<SecurityQuestionDto> questions,
            ClientInfo clientInfo,
            SecurityContext securityContext
    ) {}

    private CustomerContext loadCustomerContext(String customerId, String deviceId, String userId, String mobileNo, String logTag) {
        String accountNo = accountRepository.findAccountNoByCustomerId(customerId)
                .orElseThrow(() -> {
                    log.warn("[{}] no account found for customerId={}", logTag, customerId);
                    return new ResourceNotFoundException("No account found for customer: " + customerId);
                });

        String customerName = customerRepository.findById(customerId)
                .map(c -> {
                    if (c.getName() != null && !c.getName().isBlank()) return c.getName();
                    String en = ((c.getFirstNameEn() != null ? c.getFirstNameEn() : "") + " "
                            + (c.getLastNameEn() != null ? c.getLastNameEn() : "")).trim();
                    return en.isBlank() ? null : en;
                })
                .orElse(userId);

        List<SecurityQuestionRepository.SecurityQuestionProjection> projections =
                securityQuestionRepository.findByCustomerId(customerId);
        if (projections.isEmpty()) {
            log.warn("[{}] no security questions found for customerId={}", logTag, customerId);
            throw new ResourceNotFoundException("No security questions found for customer: " + customerId);
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

        return new CustomerContext(accountNo, customerName, questions, clientInfo, securityContext);
    }
}