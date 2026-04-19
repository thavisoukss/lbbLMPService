package com.lbb.lmps.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.lbb.lmps.dto.*;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import com.lbb.lmps.dto.SmartQrInfoRequest.QrData;
import com.lbb.lmps.entity.WithdrawTxn;
import com.lbb.lmps.exception.BusinessException;
import com.lbb.lmps.exception.MSmartException;
import com.lbb.lmps.exception.ResourceNotFoundException;
import com.lbb.lmps.remote.ApiMSmart;
import com.lbb.lmps.repository.SecurityQuestionRepository;
import com.lbb.lmps.repository.WithdrawTxnRepository;
import com.lbb.lmps.service.TransferOutService;
import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@Slf4j
@Service
public class TransferOutServiceImpl implements TransferOutService {

    private static final ObjectMapper MAPPER = new ObjectMapper()
            .findAndRegisterModules()
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    private static final DateTimeFormatter TRAN_DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final BCryptPasswordEncoder PASSWORD_ENCODER = new BCryptPasswordEncoder(12);

    private final ApiMSmart apiMSmart;
    private final WithdrawTxnRepository withdrawTxnRepository;
    private final SecurityQuestionRepository securityQuestionRepository;

    @Override
    @Transactional
    public TransferOutQrResponse transferOutQr(TransferOutQrRequest request, String deviceId) throws Exception {
        log.info("[transferOutQr] deviceId={} amount={} nonce={}", deviceId, request.getAmount(), request.getXNonce());
        long start = System.currentTimeMillis();

        Claims claims = (Claims) SecurityContextHolder.getContext().getAuthentication().getDetails();
        String userId = claims.getSubject();
        String customerId = (String) claims.get("user-id");
        String mobileNo = (String) claims.get("user-phone");
        log.info("[transferOutQr] userId={} customerId={}", userId, customerId);

        // Security question verification
        Map<String, String> storedAnswers = securityQuestionRepository.findAnswersByCustomerId(customerId)
                .stream()
                .collect(Collectors.toMap(
                        SecurityQuestionRepository.CustomerAnswerProjection::getQuestionId,
                        SecurityQuestionRepository.CustomerAnswerProjection::getAnswer));
        log.info("[transferOutQr] verifying security questions customerId={}", customerId);
        verifySecurityAnswer(storedAnswers, request.getFirstQuestionId(), request.getFirstAnswer(), customerId, "ER_FIRST_ANSWER_INVALID", "Invalid first security question answer");
        verifySecurityAnswer(storedAnswers, request.getSecondQuestionId(), request.getSecondAnswer(), customerId, "ER_SECOND_ANSWER_INVALID", "Invalid second security question answer");
        verifySecurityAnswer(storedAnswers, request.getThirdQuestionId(), request.getThirdAnswer(), customerId, "ER_THIRD_ANSWER_INVALID", "Invalid third security question answer");
        log.info("[transferOutQr] security questions verified ok customerId={}", customerId);

        // Step 1: fetch WITHDRAW_TXN by x_nonce
        WithdrawTxn withdrawTxn = withdrawTxnRepository.findByNonce(request.getXNonce())
                .orElseThrow(() -> {
                    log.warn("[transferOutQr] no WITHDRAW_TXN found for nonce={}", request.getXNonce());
                    return new ResourceNotFoundException("Invalid or expired transaction nonce");
                });

        if (!customerId.equals(withdrawTxn.getCustomerId())) {
            log.warn("[transferOutQr] nonce ownership mismatch caller={} owner={}", customerId, withdrawTxn.getCustomerId());
            throw new ResourceNotFoundException("Invalid or expired transaction nonce");
        }

        if (!"DEBIT_PENDING".equals(withdrawTxn.getStatus())) {
            log.warn("[transferOutQr] unexpected status={} for nonce={}", withdrawTxn.getStatus(), request.getXNonce());
            throw new BusinessException("4001", "Transaction is not in pending state");
        }

        log.info("[transferOutQr] loaded WITHDRAW_TXN id={} txnId={}", withdrawTxn.getId(), withdrawTxn.getTransactionId());

        ClientInfo clientInfo = new ClientInfo();
        clientInfo.setDeviceId(deviceId);
        clientInfo.setMobileNo(mobileNo);
        clientInfo.setUserId(userId);

        SecurityContext mobileCtx = new SecurityContext();
        mobileCtx.setChannel("MOBILE");

        // Step 2: call QR info to resolve memberId
        QrData qrData = new QrData();
        qrData.setQrString(request.getQrString());
        SmartQrInfoRequest qrInfoRequest = new SmartQrInfoRequest();
        qrInfoRequest.setClientInfo(clientInfo);
        qrInfoRequest.setSecurityContext(mobileCtx);
        qrInfoRequest.setData(qrData);

        log.info("[transferOutQr] calling m-smart QR info qrString={}", request.getQrString());
        String rawQrInfo = apiMSmart.callQrInfo(qrInfoRequest);
        SmartQrInfoResponse qrInfoResponse = MAPPER.readValue(rawQrInfo, SmartQrInfoResponse.class);
        if (!"0000".equals(qrInfoResponse.getResponseCode())) {
            log.warn("[transferOutQr] m-smart QR info error | code={} msg={}", qrInfoResponse.getResponseCode(), qrInfoResponse.getResponseMessage());
            throw new MSmartException(qrInfoResponse.getResponseCode(), qrInfoResponse.getResponseMessage());
        }
        String memberId = qrInfoResponse.getData().getMemberId();
        log.info("[transferOutQr] qrInfo memberId={}", memberId);

        // Step 3: load fee list from stored WITHDRAW_TXN snapshot
        FeeList feeList = MAPPER.readValue(withdrawTxn.getFeeList(), FeeList.class);

        // Step 4: calculate fee
        BigDecimal txnFee = calculateFee(feeList, request.getAmount(), withdrawTxn.getCurrencyCode());
        log.info("[transferOutQr] txnFee={} amount={} ccy={}", txnFee, request.getAmount(), withdrawTxn.getCurrencyCode());

        // Step 5: execute transfer-out
        SecurityContext msCtx = new SecurityContext();
        msCtx.setChannel("MSMART");

        SmartTransferOutData transferData = new SmartTransferOutData();
        transferData.setTxnId(withdrawTxn.getTransactionId());
        transferData.setCbsRefNo(null);
        transferData.setTxnType("LMPOTA");
        transferData.setTxnAmount(request.getAmount());
        transferData.setTxnFee(txnFee);
        transferData.setTxnCcy(withdrawTxn.getCurrencyCode());
        transferData.setPurpose(request.getPurpose());
        transferData.setFromUserId(userId);
        transferData.setFromCustName(withdrawTxn.getDrAccountName());
        transferData.setFromAcctId(withdrawTxn.getDrAccountNo());
        transferData.setFromCif(withdrawTxn.getDrCif());
        transferData.setToCustName(withdrawTxn.getCrAccountName());
        transferData.setToAcctId(request.getQrString());
        transferData.setToCif(null);
        transferData.setToType("QR");
        transferData.setToMemberId(memberId);

        SmartTransferOutRequest transferRequest = new SmartTransferOutRequest();
        transferRequest.setRequestId(null);
        transferRequest.setClientInfo(clientInfo);
        transferRequest.setSecurityContext(msCtx);
        transferRequest.setData(transferData);

        log.info("[transferOutQr] calling m-smart transfer-out txnId={} amount={} fee={} ccy={}", withdrawTxn.getTransactionId(), request.getAmount(), txnFee, withdrawTxn.getCurrencyCode());
        String rawTransfer = apiMSmart.callTransferOut(transferRequest);
        SmartTransferOutResponse transferResponse = MAPPER.readValue(rawTransfer, SmartTransferOutResponse.class);
        if (!"0000".equals(transferResponse.getResponseCode())) {
            log.warn("[transferOutQr] m-smart transfer error | code={} msg={}", transferResponse.getResponseCode(), transferResponse.getResponseMessage());
            throw new MSmartException(transferResponse.getResponseCode(), transferResponse.getResponseMessage());
        }

        SmartTransferOutData result = transferResponse.getData();
        log.info("[transferOutQr] m-smart transfer-out success cbsRefNo={} txnId={}", result.getCbsRefNo(), result.getTxnId());

        // Step 6: update WITHDRAW_TXN with final result
        withdrawTxn.setStatus("COMPLETED");
        withdrawTxn.setAmount(result.getTxnAmount() != null ? result.getTxnAmount() : request.getAmount());
        withdrawTxn.setFeeAmt(result.getTxnFee() != null ? result.getTxnFee() : txnFee);
        withdrawTxn.setFeeProviderAmt(result.getTxnFee() != null ? result.getTxnFee() : txnFee);
        withdrawTxn.setCoreBankingRef(result.getCbsRefNo());
        withdrawTxn.setVersion(withdrawTxn.getVersion() + 1);
        withdrawTxnRepository.save(withdrawTxn);
        log.info("[transferOutQr] WITHDRAW_TXN updated id={} status=COMPLETED cbsRefNo={} txnId={}", withdrawTxn.getId(), result.getCbsRefNo(), withdrawTxn.getTransactionId());

        // Step 7: build response
        TransferOutQrResponse response = new TransferOutQrResponse();
        response.setTransactionId(result.getTxnId());
        response.setSlipCode(result.getTxnId());
        response.setTranDate(LocalDateTime.now().format(TRAN_DATE_FMT));
        response.setTotalAmount(result.getTxnAmount());
        response.setCurrencyCode(result.getTxnCcy());
        response.setFeeAmt(result.getTxnFee());
        response.setDrAccountNo(result.getFromAcctId());
        response.setDrAccountName(result.getFromCustName());
        response.setCrAccountNo(result.getToAcctId());
        response.setCrAccountName(result.getToCustName());
        response.setProviderRef(result.getCbsRefNo());
        response.setPurpose(result.getPurpose());

        log.info("[transferOutQr] completed txnId={} totalAmount={} feeAmt={} cbsRefNo={} duration_ms={}", result.getTxnId(), response.getTotalAmount(), response.getFeeAmt(), response.getProviderRef(), System.currentTimeMillis() - start);
        return response;
    }

    @Override
    @Transactional
    public TransferOutQrResponse transferOutAccount(TransferOutAccountRequest request, String deviceId) throws Exception {
        log.info("[transferOutAccount] deviceId={} amount={} nonce={}", deviceId, request.getAmount(), request.getXNonce());
        long start = System.currentTimeMillis();

        Claims claims = (Claims) SecurityContextHolder.getContext().getAuthentication().getDetails();
        String userId = claims.getSubject();
        String customerId = (String) claims.get("user-id");
        String mobileNo = (String) claims.get("user-phone");
        log.info("[transferOutAccount] userId={} customerId={}", userId, customerId);

        // Security question verification
        Map<String, String> storedAnswers = securityQuestionRepository.findAnswersByCustomerId(customerId)
                .stream()
                .collect(Collectors.toMap(
                        SecurityQuestionRepository.CustomerAnswerProjection::getQuestionId,
                        SecurityQuestionRepository.CustomerAnswerProjection::getAnswer));
        log.info("[transferOutAccount] verifying security questions customerId={}", customerId);
        verifySecurityAnswer(storedAnswers, request.getFirstQuestionId(), request.getFirstAnswer(), customerId, "ER_FIRST_ANSWER_INVALID", "Invalid first security question answer");
        verifySecurityAnswer(storedAnswers, request.getSecondQuestionId(), request.getSecondAnswer(), customerId, "ER_SECOND_ANSWER_INVALID", "Invalid second security question answer");
        verifySecurityAnswer(storedAnswers, request.getThirdQuestionId(), request.getThirdAnswer(), customerId, "ER_THIRD_ANSWER_INVALID", "Invalid third security question answer");
        log.info("[transferOutAccount] security questions verified ok customerId={}", customerId);

        // Load WITHDRAW_TXN by nonce
        WithdrawTxn withdrawTxn = withdrawTxnRepository.findByNonce(request.getXNonce())
                .orElseThrow(() -> {
                    log.warn("[transferOutAccount] no WITHDRAW_TXN found for nonce={}", request.getXNonce());
                    return new ResourceNotFoundException("Invalid or expired transaction nonce");
                });

        if (!customerId.equals(withdrawTxn.getCustomerId())) {
            log.warn("[transferOutAccount] nonce ownership mismatch caller={} owner={}", customerId, withdrawTxn.getCustomerId());
            throw new ResourceNotFoundException("Invalid or expired transaction nonce");
        }

        if (!"DEBIT_PENDING".equals(withdrawTxn.getStatus())) {
            log.warn("[transferOutAccount] unexpected status={} for nonce={}", withdrawTxn.getStatus(), request.getXNonce());
            throw new BusinessException("4001", "Transaction is not in pending state");
        }

        log.info("[transferOutAccount] loaded WITHDRAW_TXN id={} txnId={}", withdrawTxn.getId(), withdrawTxn.getTransactionId());

        // toMember was stored in REMARK at inquiry time
        String toMember = withdrawTxn.getRemark();

        // Calculate fee from stored snapshot
        FeeList feeList = MAPPER.readValue(withdrawTxn.getFeeList(), FeeList.class);
        BigDecimal txnFee = calculateFee(feeList, request.getAmount(), withdrawTxn.getCurrencyCode());
        log.info("[transferOutAccount] txnFee={} amount={} ccy={}", txnFee, request.getAmount(), withdrawTxn.getCurrencyCode());

        // Execute transfer-out
        ClientInfo clientInfo = new ClientInfo();
        clientInfo.setDeviceId(deviceId);
        clientInfo.setMobileNo(mobileNo);
        clientInfo.setUserId(userId);

        SecurityContext msCtx = new SecurityContext();
        msCtx.setChannel("MSMART");

        SmartTransferOutData transferData = new SmartTransferOutData();
        transferData.setTxnId(withdrawTxn.getTransactionId());
        transferData.setCbsRefNo(null);
        transferData.setTxnType("LMPOTA");
        transferData.setTxnAmount(request.getAmount());
        transferData.setTxnFee(txnFee);
        transferData.setTxnCcy(withdrawTxn.getCurrencyCode());
        transferData.setPurpose(request.getPurpose());
        transferData.setFromUserId(userId);
        transferData.setFromCustName(withdrawTxn.getDrAccountName());
        transferData.setFromAcctId(withdrawTxn.getDrAccountNo());
        transferData.setFromCif(withdrawTxn.getDrCif());
        transferData.setToCustName(withdrawTxn.getCrAccountName());
        transferData.setToAcctId(withdrawTxn.getCrAccountNo());
        transferData.setToCif(null);
        transferData.setToType("ACCOUNT");
        transferData.setToMemberId(toMember);

        SmartTransferOutRequest transferRequest = new SmartTransferOutRequest();
        transferRequest.setRequestId(null);
        transferRequest.setClientInfo(clientInfo);
        transferRequest.setSecurityContext(msCtx);
        transferRequest.setData(transferData);

        log.info("[transferOutAccount] calling m-smart transfer-out txnId={} amount={} fee={} ccy={} toMember={}", withdrawTxn.getTransactionId(), request.getAmount(), txnFee, withdrawTxn.getCurrencyCode(), toMember);
        String rawTransfer = apiMSmart.callTransferOut(transferRequest);
        SmartTransferOutResponse transferResponse = MAPPER.readValue(rawTransfer, SmartTransferOutResponse.class);
        if (!"0000".equals(transferResponse.getResponseCode())) {
            log.warn("[transferOutAccount] m-smart transfer error | code={} msg={}", transferResponse.getResponseCode(), transferResponse.getResponseMessage());
            throw new MSmartException(transferResponse.getResponseCode(), transferResponse.getResponseMessage());
        }

        SmartTransferOutData result = transferResponse.getData();
        log.info("[transferOutAccount] m-smart transfer-out success cbsRefNo={} txnId={}", result.getCbsRefNo(), result.getTxnId());

        // Update WITHDRAW_TXN
        withdrawTxn.setStatus("COMPLETED");
        withdrawTxn.setAmount(result.getTxnAmount() != null ? result.getTxnAmount() : request.getAmount());
        withdrawTxn.setFeeAmt(result.getTxnFee() != null ? result.getTxnFee() : txnFee);
        withdrawTxn.setFeeProviderAmt(result.getTxnFee() != null ? result.getTxnFee() : txnFee);
        withdrawTxn.setCoreBankingRef(result.getCbsRefNo());
        withdrawTxn.setVersion(withdrawTxn.getVersion() + 1);
        withdrawTxnRepository.save(withdrawTxn);
        log.info("[transferOutAccount] WITHDRAW_TXN updated id={} status=COMPLETED cbsRefNo={} txnId={}", withdrawTxn.getId(), result.getCbsRefNo(), withdrawTxn.getTransactionId());

        TransferOutQrResponse response = new TransferOutQrResponse();
        response.setTransactionId(result.getTxnId());
        response.setSlipCode(result.getTxnId());
        response.setTranDate(LocalDateTime.now().format(TRAN_DATE_FMT));
        response.setTotalAmount(result.getTxnAmount());
        response.setCurrencyCode(result.getTxnCcy());
        response.setFeeAmt(result.getTxnFee());
        response.setDrAccountNo(result.getFromAcctId());
        response.setDrAccountName(result.getFromCustName());
        response.setCrAccountNo(result.getToAcctId());
        response.setCrAccountName(result.getToCustName());
        response.setProviderRef(result.getCbsRefNo());
        response.setPurpose(result.getPurpose());

        log.info("[transferOutAccount] completed txnId={} totalAmount={} feeAmt={} cbsRefNo={} duration_ms={}", result.getTxnId(), response.getTotalAmount(), response.getFeeAmt(), response.getProviderRef(), System.currentTimeMillis() - start);
        return response;
    }

    private void verifySecurityAnswer(Map<String, String> stored, String questionId, String answer, String customerId, String errorCode, String errorMessage) {
        String hash = stored.get(questionId);
        if (hash == null || !PASSWORD_ENCODER.matches(answer, hash)) {
            log.warn("[transferOutQr] security question failed customerId={} questionId={} errorCode={}", customerId, questionId, errorCode);
            throw new BusinessException(errorCode, errorMessage);
        }
    }

    private BigDecimal calculateFee(FeeList feeList, BigDecimal amount, String ccy) {
        List<FeeEntry> tiers = null;
        if ("THB".equals(ccy) && feeList.getTHB() != null) {
            tiers = feeList.getTHB();
        } else if ("USD".equals(ccy) && feeList.getUSD() != null) {
            tiers = feeList.getUSD();
        } else if (feeList.getLAK() != null) {
            tiers = feeList.getLAK();
        }
        if (tiers == null || tiers.isEmpty()) {
            return BigDecimal.ZERO;
        }
        BigDecimal fee = BigDecimal.ZERO;
        for (FeeEntry tier : tiers) {
            if (amount.compareTo(tier.getFrom()) >= 0) {
                fee = tier.getFeeamount();
            }
        }
        return fee;
    }
}