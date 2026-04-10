package com.lbb.lmps.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.lbb.lmps.dto.*;
import com.lbb.lmps.dto.SmartQrInfoRequest.QrData;
import com.lbb.lmps.entity.WithdrawTxn;
import com.lbb.lmps.exception.MSmartException;
import com.lbb.lmps.exception.ResourceNotFoundException;
import com.lbb.lmps.remote.ApiMSmart;
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

@RequiredArgsConstructor
@Slf4j
@Service
public class TransferOutServiceImpl implements TransferOutService {

    private static final ObjectMapper MAPPER = new ObjectMapper()
            .findAndRegisterModules()
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    private static final DateTimeFormatter TRAN_DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final ApiMSmart apiMSmart;
    private final WithdrawTxnRepository withdrawTxnRepository;

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

        // Step 1: fetch WITHDRAW_TXN by x_nonce
        WithdrawTxn withdrawTxn = withdrawTxnRepository.findByNonce(request.getXNonce())
                .orElseThrow(() -> {
                    log.warn("[transferOutQr] no WITHDRAW_TXN found for nonce={}", request.getXNonce());
                    return new ResourceNotFoundException("Invalid or expired transaction nonce");
                });

        if (!"DEBIT_PENDING".equals(withdrawTxn.getStatus())) {
            log.warn("[transferOutQr] unexpected status={} for nonce={}", withdrawTxn.getStatus(), request.getXNonce());
            throw new MSmartException("4001", "Transaction is not in pending state");
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

        String rawQrInfo = apiMSmart.callQrInfo(qrInfoRequest);
        SmartQrInfoResponse qrInfoResponse = MAPPER.readValue(rawQrInfo, SmartQrInfoResponse.class);
        if (!"0000".equals(qrInfoResponse.getResponseCode())) {
            log.warn("[transferOutQr] m-smart QR info error | code={} msg={}", qrInfoResponse.getResponseCode(), qrInfoResponse.getResponseMessage());
            throw new MSmartException(qrInfoResponse.getResponseCode(), qrInfoResponse.getResponseMessage());
        }
        String memberId = qrInfoResponse.getData().getMemberId();
        log.info("[transferOutQr] qrInfo memberId={}", memberId);

        // Step 3: call inquiry-out with stored txnId + resolved memberId — retrieves fee list
        SmartInquiryDataRequest inqData = new SmartInquiryDataRequest();
        inqData.setTxnId(withdrawTxn.getTransactionId());
        inqData.setFromuser(userId);
        inqData.setFromaccount(withdrawTxn.getDrAccountNo());
        inqData.setFromCif(withdrawTxn.getDrCif());
        inqData.setToType("QR");
        inqData.setToaccount(request.getQrString());
        inqData.setTomember(memberId);

        SmartInquiryOutRequest inqRequest = new SmartInquiryOutRequest();
        inqRequest.setClientInfo(clientInfo);
        inqRequest.setSecurityContext(mobileCtx);
        inqRequest.setData(inqData);

        String rawInquiry = apiMSmart.callInquiryOut(inqRequest);
        SmartInquiryOutResponse inqResponse = MAPPER.readValue(rawInquiry, SmartInquiryOutResponse.class);
        if (!"0000".equals(inqResponse.getResponseCode())) {
            log.warn("[transferOutQr] m-smart inquiry error | code={} msg={}", inqResponse.getResponseCode(), inqResponse.getResponseMessage());
            throw new MSmartException(inqResponse.getResponseCode(), inqResponse.getResponseMessage());
        }

        InquiryOutData inqResult = inqResponse.getData();
        log.info("[transferOutQr] inquiry txnId={} toCustName={}", inqResult.getTxnId(), inqResult.getAccountname());

        // Step 4: calculate fee
        BigDecimal txnFee = calculateFee(inqResult.getFeelist(), request.getAmount(), withdrawTxn.getCurrencyCode());
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

        String rawTransfer = apiMSmart.callTransferOut(transferRequest);
        SmartTransferOutResponse transferResponse = MAPPER.readValue(rawTransfer, SmartTransferOutResponse.class);
        if (!"0000".equals(transferResponse.getResponseCode())) {
            log.warn("[transferOutQr] m-smart transfer error | code={} msg={}", transferResponse.getResponseCode(), transferResponse.getResponseMessage());
            throw new MSmartException(transferResponse.getResponseCode(), transferResponse.getResponseMessage());
        }

        SmartTransferOutData result = transferResponse.getData();

        // Step 6: update WITHDRAW_TXN with final result
        withdrawTxn.setStatus("COMPLETED");
        withdrawTxn.setAmount(result.getTxnAmount() != null ? result.getTxnAmount() : request.getAmount());
        withdrawTxn.setFeeAmt(result.getTxnFee() != null ? result.getTxnFee() : txnFee);
        withdrawTxn.setFeeProviderAmt(result.getTxnFee() != null ? result.getTxnFee() : txnFee);
        withdrawTxn.setCoreBankingRef(result.getCbsRefNo());
        withdrawTxn.setVersion(withdrawTxn.getVersion() + 1);
        withdrawTxnRepository.save(withdrawTxn);
        log.info("[transferOutQr] WITHDRAW_TXN updated id={} status=COMPLETED cbsRefNo={} txnId={}", withdrawTxn.getId(), result.getCbsRefNo(), withdrawTxn.getTransactionId());

        // Step 6: build response
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

        log.info("[transferOutQr] txnId={} duration_ms={}", result.getTxnId(), System.currentTimeMillis() - start);
        return response;
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