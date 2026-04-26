package com.lbb.lmps.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lbb.lmps.dto.*;
import org.springframework.context.MessageSource;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import com.lbb.lmps.dto.SmartQrInfoRequest.QrData;
import com.lbb.lmps.entity.Customer;
import com.lbb.lmps.entity.WithdrawTxn;
import com.lbb.lmps.exception.BusinessException;
import com.lbb.lmps.exception.MSmartException;
import com.lbb.lmps.exception.ResourceNotFoundException;
import com.lbb.lmps.remote.ApiMSmart;
import com.lbb.lmps.repository.CustomerRepository;
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
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.Signature;
import java.security.spec.X509EncodedKeySpec;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.context.i18n.LocaleContextHolder;

@RequiredArgsConstructor
@Slf4j
@Service
public class TransferOutServiceImpl implements TransferOutService {

    private static final DateTimeFormatter TRAN_DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final BCryptPasswordEncoder PASSWORD_ENCODER = new BCryptPasswordEncoder(12);

    private final ApiMSmart apiMSmart;
    private final ObjectMapper mapper;
    private final MessageSource messageSource;
    private final WithdrawTxnRepository withdrawTxnRepository;
    private final SecurityQuestionRepository securityQuestionRepository;
    private final CustomerRepository customerRepository;

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
        verifySecurityAnswer(storedAnswers, request.getFirstQuestionId(), request.getFirstAnswer(), customerId, "ER_FIRST_ANSWER_INVALID", "transferOutQr");
        verifySecurityAnswer(storedAnswers, request.getSecondQuestionId(), request.getSecondAnswer(), customerId, "ER_SECOND_ANSWER_INVALID", "transferOutQr");
        verifySecurityAnswer(storedAnswers, request.getThirdQuestionId(), request.getThirdAnswer(), customerId, "ER_THIRD_ANSWER_INVALID", "transferOutQr");
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
            throw new BusinessException("4001", messageSource.getMessage("error.4001.message", null, LocaleContextHolder.getLocale()));
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
        SmartQrInfoResponse qrInfoResponse = mapper.readValue(rawQrInfo, SmartQrInfoResponse.class);
        if (!"0000".equals(qrInfoResponse.getResponseCode())) {
            log.warn("[transferOutQr] m-smart QR info error | code={} msg={}", qrInfoResponse.getResponseCode(), qrInfoResponse.getResponseMessage());
            throw new MSmartException(qrInfoResponse.getResponseCode(), qrInfoResponse.getResponseMessage());
        }
        String memberId = qrInfoResponse.getData().getMemberId();
        log.info("[transferOutQr] qrInfo memberId={}", memberId);

        // Step 3: load fee list from stored WITHDRAW_TXN snapshot
        FeeList feeList = withdrawTxn.getFeeList() != null
                ? mapper.readValue(withdrawTxn.getFeeList(), FeeList.class)
                : new FeeList();

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
        SmartTransferOutResponse transferResponse = mapper.readValue(rawTransfer, SmartTransferOutResponse.class);
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
        verifySecurityAnswer(storedAnswers, request.getFirstQuestionId(), request.getFirstAnswer(), customerId, "ER_FIRST_ANSWER_INVALID", "transferOutAccount");
        verifySecurityAnswer(storedAnswers, request.getSecondQuestionId(), request.getSecondAnswer(), customerId, "ER_SECOND_ANSWER_INVALID", "transferOutAccount");
        verifySecurityAnswer(storedAnswers, request.getThirdQuestionId(), request.getThirdAnswer(), customerId, "ER_THIRD_ANSWER_INVALID", "transferOutAccount");
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
            throw new BusinessException("4001", messageSource.getMessage("error.4001.message", null, LocaleContextHolder.getLocale()));
        }

        log.info("[transferOutAccount] loaded WITHDRAW_TXN id={} txnId={}", withdrawTxn.getId(), withdrawTxn.getTransactionId());

        // toMember was stored in REMARK at inquiry time
        String toMember = withdrawTxn.getRemark();

        // Calculate fee from stored snapshot
        FeeList feeList = withdrawTxn.getFeeList() != null
                ? mapper.readValue(withdrawTxn.getFeeList(), FeeList.class)
                : new FeeList();
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
        SmartTransferOutResponse transferResponse = mapper.readValue(rawTransfer, SmartTransferOutResponse.class);
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

    @Override
    @Transactional
    public TransferOutQrResponse transferOutQrBio(TransferOutQrBioRequest request, String deviceId) throws Exception {
        log.info("[transferOutQrBio] deviceId={} amount={} nonce={}", deviceId, request.getAmount(), request.getXNonce());
        long start = System.currentTimeMillis();

        Claims claims = (Claims) SecurityContextHolder.getContext().getAuthentication().getDetails();
        String userId = claims.getSubject();
        String customerId = (String) claims.get("user-id");
        String mobileNo = (String) claims.get("user-phone");
        log.info("[transferOutQrBio] userId={} customerId={}", userId, customerId);

        // Biometric signature verification
        log.info("[transferOutQrBio] verifying biometric signature customerId={}", customerId);
        String bioKeyPem = customerRepository.findById(customerId)
                .map(Customer::getBioKey)
                .orElseThrow(() -> {
                    log.warn("[transferOutQrBio] no customer found customerId={}", customerId);
                    return new ResourceNotFoundException("Customer not found: " + customerId);
                });
        verifyBioSignature(bioKeyPem, request.getTimestamp(), mobileNo, request.getSecret(), request.getSignature(), customerId);
        log.info("[transferOutQrBio] biometric signature verified ok customerId={}", customerId);

        // Load WITHDRAW_TXN by nonce
        WithdrawTxn withdrawTxn = withdrawTxnRepository.findByNonce(request.getXNonce())
                .orElseThrow(() -> {
                    log.warn("[transferOutQrBio] no WITHDRAW_TXN found for nonce={}", request.getXNonce());
                    return new ResourceNotFoundException("Invalid or expired transaction nonce");
                });

        if (!customerId.equals(withdrawTxn.getCustomerId())) {
            log.warn("[transferOutQrBio] nonce ownership mismatch caller={} owner={}", customerId, withdrawTxn.getCustomerId());
            throw new ResourceNotFoundException("Invalid or expired transaction nonce");
        }

        if (!"DEBIT_PENDING".equals(withdrawTxn.getStatus())) {
            log.warn("[transferOutQrBio] unexpected status={} for nonce={}", withdrawTxn.getStatus(), request.getXNonce());
            throw new BusinessException("4001", messageSource.getMessage("error.4001.message", null, LocaleContextHolder.getLocale()));
        }

        log.info("[transferOutQrBio] loaded WITHDRAW_TXN id={} txnId={}", withdrawTxn.getId(), withdrawTxn.getTransactionId());

        ClientInfo clientInfo = new ClientInfo();
        clientInfo.setDeviceId(deviceId);
        clientInfo.setMobileNo(mobileNo);
        clientInfo.setUserId(userId);

        SecurityContext mobileCtx = new SecurityContext();
        mobileCtx.setChannel("MOBILE");

        // Call QR info to resolve memberId
        QrData qrData = new QrData();
        qrData.setQrString(request.getQrString());
        SmartQrInfoRequest qrInfoRequest = new SmartQrInfoRequest();
        qrInfoRequest.setClientInfo(clientInfo);
        qrInfoRequest.setSecurityContext(mobileCtx);
        qrInfoRequest.setData(qrData);

        log.info("[transferOutQrBio] calling m-smart QR info qrString={}", request.getQrString());
        String rawQrInfo = apiMSmart.callQrInfo(qrInfoRequest);
        SmartQrInfoResponse qrInfoResponse = mapper.readValue(rawQrInfo, SmartQrInfoResponse.class);
        if (!"0000".equals(qrInfoResponse.getResponseCode())) {
            log.warn("[transferOutQrBio] m-smart QR info error | code={} msg={}", qrInfoResponse.getResponseCode(), qrInfoResponse.getResponseMessage());
            throw new MSmartException(qrInfoResponse.getResponseCode(), qrInfoResponse.getResponseMessage());
        }
        String memberId = qrInfoResponse.getData().getMemberId();
        log.info("[transferOutQrBio] qrInfo memberId={}", memberId);

        // Calculate fee from stored WITHDRAW_TXN snapshot
        FeeList feeList = withdrawTxn.getFeeList() != null
                ? mapper.readValue(withdrawTxn.getFeeList(), FeeList.class)
                : new FeeList();
        BigDecimal txnFee = calculateFee(feeList, request.getAmount(), withdrawTxn.getCurrencyCode());
        log.info("[transferOutQrBio] txnFee={} amount={} ccy={}", txnFee, request.getAmount(), withdrawTxn.getCurrencyCode());

        // Execute transfer-out
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

        log.info("[transferOutQrBio] calling m-smart transfer-out txnId={} amount={} fee={} ccy={}", withdrawTxn.getTransactionId(), request.getAmount(), txnFee, withdrawTxn.getCurrencyCode());
        String rawTransfer = apiMSmart.callTransferOut(transferRequest);
        SmartTransferOutResponse transferResponse = mapper.readValue(rawTransfer, SmartTransferOutResponse.class);
        if (!"0000".equals(transferResponse.getResponseCode())) {
            log.warn("[transferOutQrBio] m-smart transfer error | code={} msg={}", transferResponse.getResponseCode(), transferResponse.getResponseMessage());
            throw new MSmartException(transferResponse.getResponseCode(), transferResponse.getResponseMessage());
        }

        SmartTransferOutData result = transferResponse.getData();
        log.info("[transferOutQrBio] m-smart transfer-out success cbsRefNo={} txnId={}", result.getCbsRefNo(), result.getTxnId());

        // Update WITHDRAW_TXN
        withdrawTxn.setStatus("COMPLETED");
        withdrawTxn.setAmount(result.getTxnAmount() != null ? result.getTxnAmount() : request.getAmount());
        withdrawTxn.setFeeAmt(result.getTxnFee() != null ? result.getTxnFee() : txnFee);
        withdrawTxn.setFeeProviderAmt(result.getTxnFee() != null ? result.getTxnFee() : txnFee);
        withdrawTxn.setCoreBankingRef(result.getCbsRefNo());
        withdrawTxnRepository.save(withdrawTxn);
        log.info("[transferOutQrBio] WITHDRAW_TXN updated id={} status=COMPLETED cbsRefNo={} txnId={}", withdrawTxn.getId(), result.getCbsRefNo(), withdrawTxn.getTransactionId());

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

        log.info("[transferOutQrBio] completed txnId={} totalAmount={} feeAmt={} cbsRefNo={} duration_ms={}", result.getTxnId(), response.getTotalAmount(), response.getFeeAmt(), response.getProviderRef(), System.currentTimeMillis() - start);
        return response;
    }

    private void verifyBioSignature(String bioKeyPem, String timestamp, String mobileNo, String secret, String signature, String customerId) {
        try {
            String base64Key = bioKeyPem
                    .replace("-----BEGIN PUBLIC KEY-----", "")
                    .replace("-----END PUBLIC KEY-----", "")
                    .replaceAll("\\s+", "");
            byte[] derBytes = Base64.getDecoder().decode(base64Key);
            PublicKey publicKey = KeyFactory.getInstance("RSA").generatePublic(new X509EncodedKeySpec(derBytes));

            String message = timestamp + "|" + mobileNo + "|" + secret;
            byte[] sigBytes = Base64.getDecoder().decode(signature);

            Signature sig = Signature.getInstance("SHA256withRSA");
            sig.initVerify(publicKey);
            sig.update(message.getBytes(StandardCharsets.UTF_8));

            if (!sig.verify(sigBytes)) {
                log.warn("[verifyBioSignature] signature mismatch customerId={}", customerId);
                throw new BusinessException("ER_BIO_SIGNATURE_INVALID",
                        messageSource.getMessage("error.ER_BIO_SIGNATURE_INVALID.message", null, LocaleContextHolder.getLocale()));
            }
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("[verifyBioSignature] key processing error customerId={}", customerId, e);
            throw new RuntimeException("Bio key processing error", e);
        }
    }

    private void verifySecurityAnswer(Map<String, String> stored, String questionId, String answer, String customerId, String errorCode, String logTag) {
        String hash = stored.get(questionId);
        if (hash == null || !PASSWORD_ENCODER.matches(answer, hash)) {
            log.warn("[{}] security question failed customerId={} questionId={} errorCode={}", logTag, customerId, questionId, errorCode);
            String msg = messageSource.getMessage("error." + errorCode + ".message", null, LocaleContextHolder.getLocale());
            throw new BusinessException(errorCode, msg);
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
        return tiers.stream()
                .filter(t -> t.getFrom() != null && t.getFeeamount() != null)
                .filter(t -> amount.compareTo(t.getFrom()) >= 0)
                .max(Comparator.comparing(FeeEntry::getFrom))
                .map(FeeEntry::getFeeamount)
                .orElse(BigDecimal.ZERO);
    }
}