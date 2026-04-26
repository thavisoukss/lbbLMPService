package com.lbb.lmps.service.impl;

import com.lbb.lmps.dto.*;
import com.lbb.lmps.entity.Account;
import com.lbb.lmps.entity.Customer;
import com.lbb.lmps.entity.WithdrawTxn;
import com.lbb.lmps.exception.ResourceNotFoundException;
import com.lbb.lmps.remote.ApiCoreBanking;
import com.lbb.lmps.repository.AccountRepository;
import com.lbb.lmps.repository.CustomerRepository;
import com.lbb.lmps.repository.SecurityQuestionRepository;
import com.lbb.lmps.repository.WithdrawTxnRepository;
import com.lbb.lmps.service.MinioStorageService;
import com.lbb.lmps.service.P2PService;
import com.lbb.lmps.utils.CommonInfo;
import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class P2PServiceImpl implements P2PService {

    private final CustomerRepository customerRepository;
    private final AccountRepository accountRepository;
    private final MinioStorageService minioStorageService;
    private final ApiCoreBanking apiCoreBanking;
    private final SecurityQuestionRepository securityQuestionRepository;
    private final WithdrawTxnRepository withdrawTxnRepository;
    private final CommonInfo commonInfo;

    private static final String DEFAULT_CURRENCY = "LAK";
    private static final long P2P_PAYMENT_CHANNEL_ID = 30L; // Example ID for P2P

    @Override
    @Transactional(readOnly = true)
    public P2PAccountInfoResponse getAccountInfoByPhone(String crPhone) {
        log.info("[getAccountInfoByPhone] looking up customer by phone={}", crPhone);
        long start = System.currentTimeMillis();

        Customer customer = customerRepository.findByPhone(crPhone)
                .orElseThrow(() -> {
                    log.warn("[getAccountInfoByPhone] no customer found for phone={}", crPhone);
                    return new ResourceNotFoundException("AccountInfoNotFound", "Account info not found");
                });

        Account account = accountRepository.findLbiCurrentByCustomerId(customer.getId())
                .orElseThrow(() -> {
                    log.warn("[getAccountInfoByPhone] no LBI CURRENT account for customerId={}", customer.getId());
                    return new ResourceNotFoundException("AccountInfoNotFound", "Account info not found");
                });

        log.info("[getAccountInfoByPhone] account loaded accountNo={} customerId={}", account.getAccountNo(), account.getCustomerId());

        String profileImage = "";
        try {
            profileImage = minioStorageService.getFileURL("images", account.getCustomerId() + "_3.jpg");
        } catch (Exception e) {
            log.warn("[getAccountInfoByPhone] MinIO presign failed customerId={} error={}", account.getCustomerId(), e.getMessage());
        }

        P2PAccountInfoData data = new P2PAccountInfoData();
        data.setAccountNo(account.getAccountNo());
        data.setAccountName(account.getAccountName());
        data.setAccountCurrency(account.getAccountCurrency());
        data.setProfileImage(profileImage);

        P2PAccountInfoResponse response = new P2PAccountInfoResponse();
        response.setStatus("success");
        response.setData(data);

        log.info("[getAccountInfoByPhone] completed phone={} accountNo={} duration_ms={}", crPhone, account.getAccountNo(), System.currentTimeMillis() - start);
        return response;
    }

    @Override
    @Transactional
    public P2PInquiryResponse inquiry(P2PInquiryRequest request) {
        log.info("[inquiry] goldWeight={} crPhone={} memo={}", request.getGoldWeight(), request.getCrPhone(), request.getMemo());
        long start = System.currentTimeMillis();

        Claims claims = (Claims) SecurityContextHolder.getContext().getAuthentication().getDetails();
        String customerId = (String) claims.get("user-id");
        log.info("[inquiry] debtor customerId={}", customerId);

        // 1. Debtor Lookup
        Customer debtor = customerRepository.findById(customerId)
                .orElseThrow(() -> new ResourceNotFoundException("Debtor not found"));
        Account drAccount = accountRepository.findLbiCurrentByCustomerId(debtor.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Debtor account not found"));

        // 2. Creditor Lookup
        Customer creditor = customerRepository.findByPhone(request.getCrPhone())
                .orElseThrow(() -> new ResourceNotFoundException("Creditor not found"));
        Account crAccount = accountRepository.findLbiCurrentByCustomerId(creditor.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Creditor account not found"));

        // 3. Rate Inquiry
        GoldRateResponse goldRate = apiCoreBanking.getGoldRate();
        BigDecimal totalAmount = request.getGoldWeight().multiply(goldRate.getSellRate());
        log.info("[inquiry] goldRate={} totalAmount={}", goldRate.getSellRate(), totalAmount);

        // 4. Security Questions
        List<SecurityQuestionRepository.SecurityQuestionProjection> projections =
                securityQuestionRepository.findByCustomerId(customerId);
        List<SecurityQuestionDto> questions = projections.stream()
                .map(p -> new SecurityQuestionDto(p.getId(), p.getDescription()))
                .toList();

        // 5. State Persistence (WithdrawTxn)
        String ref = UUID.randomUUID().toString();
        String txnId = commonInfo.genTransactionId("P2P");

        WithdrawTxn txn = new WithdrawTxn();
        txn.setPaymentChannelId(P2P_PAYMENT_CHANNEL_ID);
        txn.setCustomerId(customerId);
        txn.setTransactionId(txnId);
        txn.setNonce(ref);
        txn.setProviderCode("LMPS");
        txn.setStatus("DEBIT_PENDING");
        txn.setDrAccountNo(drAccount.getAccountNo());
        txn.setDrCif(customerId);
        txn.setDrAccountName(drAccount.getAccountName());
        txn.setCrAccountNo(crAccount.getAccountNo());
        txn.setCrAccountName(crAccount.getAccountName());
        txn.setAmount(totalAmount);
        txn.setFeeAmt(BigDecimal.ZERO);
        txn.setFeeProviderAmt(BigDecimal.ZERO);
        txn.setCurrencyCode(DEFAULT_CURRENCY);
        txn.setFeeCurrencyCode(DEFAULT_CURRENCY);
        txn.setFeeProviderCurrencyCode(DEFAULT_CURRENCY);
        txn.setRemark("P2P|WEIGHT:" + request.getGoldWeight() + "|MEMO:" + request.getMemo());
        txn.setCreatedAt(LocalDateTime.now());
        withdrawTxnRepository.save(txn);

        // 6. Response
        P2PInquiryResponse.P2PInquiryData data = new P2PInquiryResponse.P2PInquiryData();
        data.setRef(ref);
        data.setTtl(180);
        data.setDrAccountNo(drAccount.getAccountNo());
        data.setDrAccountName(drAccount.getAccountName());
        data.setDrAccountCurrency(drAccount.getAccountCurrency());
        data.setCrAccountNo(crAccount.getAccountNo());
        data.setCrAccountName(crAccount.getAccountName());
        data.setCrAccountCurrency(crAccount.getAccountCurrency());
        data.setTotalAmount(totalAmount);
        data.setGoldWeight(request.getGoldWeight());
        data.setMemo(request.getMemo());
        data.setQuestions(questions);

        P2PInquiryResponse response = new P2PInquiryResponse();
        response.setStatus("success");
        response.setData(data);

        log.info("[inquiry] completed ref={} txnId={} duration_ms={}", ref, txnId, System.currentTimeMillis() - start);
        return response;
    }
}
