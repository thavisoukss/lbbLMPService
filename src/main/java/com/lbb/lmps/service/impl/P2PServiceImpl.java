package com.lbb.lmps.service.impl;

import com.lbb.lmps.dto.*;
import com.lbb.lmps.entity.Account;
import com.lbb.lmps.entity.Customer;
import com.lbb.lmps.entity.P2PQuotation;
import com.lbb.lmps.entity.P2PTransaction;
import com.lbb.lmps.exception.BusinessException;
import com.lbb.lmps.exception.ResourceNotFoundException;
import com.lbb.lmps.remote.ApiCoreBanking;
import com.lbb.lmps.repository.AccountRepository;
import com.lbb.lmps.repository.CustomerRepository;
import com.lbb.lmps.repository.P2PQuotationRepository;
import com.lbb.lmps.repository.P2PTransactionRepository;
import com.lbb.lmps.repository.SecurityQuestionRepository;
import com.lbb.lmps.service.MinioStorageService;
import com.lbb.lmps.service.P2PService;
import com.lbb.lmps.utils.CommonInfo;
import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class P2PServiceImpl implements P2PService {

    private static final DateTimeFormatter TRAN_DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final BCryptPasswordEncoder PASSWORD_ENCODER = new BCryptPasswordEncoder(12);
    private static final int QUOTATION_TTL_MINUTES = 3;

    private final CustomerRepository customerRepository;
    private final AccountRepository accountRepository;
    private final MinioStorageService minioStorageService;
    private final ApiCoreBanking apiCoreBanking;
    private final SecurityQuestionRepository securityQuestionRepository;
    private final P2PQuotationRepository p2pQuotationRepository;
    private final P2PTransactionRepository p2pTransactionRepository;
    private final CommonInfo commonInfo;

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

        // 1. Debtor lookup
        Customer debtor = customerRepository.findById(customerId)
                .orElseThrow(() -> new ResourceNotFoundException("AccountInfoNotFound", "Debtor not found"));
        Account drAccount = accountRepository.findLbiCurrentByCustomerId(debtor.getId())
                .orElseThrow(() -> new ResourceNotFoundException("AccountInfoNotFound", "Debtor account not found"));

        // 2. Creditor lookup
        Customer creditor = customerRepository.findByPhone(request.getCrPhone())
                .orElseThrow(() -> new ResourceNotFoundException("AccountInfoNotFound", "Creditor not found"));
        Account crAccount = accountRepository.findLbiCurrentByCustomerId(creditor.getId())
                .orElseThrow(() -> new ResourceNotFoundException("AccountInfoNotFound", "Creditor account not found"));

        // 3. Rate inquiry
        GoldRateResponse goldRate = apiCoreBanking.getRate();
        BigDecimal sellRate = goldRate.getData().getSellRate();
        BigDecimal totalAmount = request.getGoldWeight().multiply(sellRate);
        log.info("[inquiry] sellRate={} totalAmount={}", sellRate, totalAmount);

        // 4. Security questions for display
        List<SecurityQuestionRepository.SecurityQuestionProjection> projections =
                securityQuestionRepository.findByCustomerId(customerId);
        List<SecurityQuestionDto> questions = projections.stream()
                .map(p -> new SecurityQuestionDto(p.getId(), p.getDescription()))
                .toList();

        // 5. Save quotation to DB for use at verify time
        String ref = UUID.randomUUID().toString();
        LocalDateTime now = LocalDateTime.now();

        P2PQuotation quotation = new P2PQuotation();
        quotation.setRef(ref);
        quotation.setCustomerId(customerId);
        quotation.setDrAccountNo(drAccount.getAccountNo());
        quotation.setDrAccountName(drAccount.getAccountName());
        quotation.setDrCcy(drAccount.getAccountCurrency());
        quotation.setCrAccountNo(crAccount.getAccountNo());
        quotation.setCrAccountName(crAccount.getAccountName());
        quotation.setCrCcy(crAccount.getAccountCurrency());
        quotation.setGoldWeight(request.getGoldWeight());
        quotation.setTotalAmount(totalAmount);
        quotation.setMemo(request.getMemo());
        quotation.setStatus("PENDING");
        quotation.setCreatedAt(now);
        quotation.setExpiredAt(now.plusMinutes(QUOTATION_TTL_MINUTES));
        p2pQuotationRepository.save(quotation);
        log.info("[inquiry] quotation saved ref={} expiredAt={}", ref, quotation.getExpiredAt());

        // 6. Build response
        P2PInquiryResponse.P2PInquiryData data = new P2PInquiryResponse.P2PInquiryData();
        data.setRef(ref);
        data.setTtl(QUOTATION_TTL_MINUTES * 60);
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

        log.info("[inquiry] completed ref={} duration_ms={}", ref, System.currentTimeMillis() - start);
        return response;
    }

    @Override
    @Transactional
    public P2PTransferVerifyResponse transferQuotationVerify(P2PTransferVerifyRequest request) {
        log.info("[transferQuotationVerify] ref={}", request.getRef());
        long start = System.currentTimeMillis();

        Claims claims = (Claims) SecurityContextHolder.getContext().getAuthentication().getDetails();
        String customerId = (String) claims.get("user-id");
        log.info("[transferQuotationVerify] customerId={}", customerId);

        // 1. Load and validate quotation
        P2PQuotation quotation = p2pQuotationRepository.findById(request.getRef())
                .orElseThrow(() -> {
                    log.warn("[transferQuotationVerify] quotation not found ref={}", request.getRef());
                    return new ResourceNotFoundException("QuotationNotFound", "Quotation not found or expired");
                });

        if (!customerId.equals(quotation.getCustomerId())) {
            log.warn("[transferQuotationVerify] ownership mismatch caller={} owner={}", customerId, quotation.getCustomerId());
            throw new ResourceNotFoundException("QuotationNotFound", "Quotation not found or expired");
        }

        if (!"PENDING".equals(quotation.getStatus())) {
            log.warn("[transferQuotationVerify] quotation already used ref={} status={}", request.getRef(), quotation.getStatus());
            throw new BusinessException("QuotationAlreadyUsed", "Quotation has already been used");
        }

        if (LocalDateTime.now().isAfter(quotation.getExpiredAt())) {
            log.warn("[transferQuotationVerify] quotation expired ref={} expiredAt={}", request.getRef(), quotation.getExpiredAt());
            throw new BusinessException("QuotationExpired", "Quotation has expired, please start a new inquiry");
        }

        // 2. Verify security questions
        Map<String, String> storedAnswers = securityQuestionRepository.findAnswersByCustomerId(customerId)
                .stream()
                .collect(Collectors.toMap(
                        SecurityQuestionRepository.CustomerAnswerProjection::getQuestionId,
                        SecurityQuestionRepository.CustomerAnswerProjection::getAnswer));

        log.info("[transferQuotationVerify] verifying security questions customerId={}", customerId);
        verifySecurityAnswer(storedAnswers, request.getFirstQuestionId(), request.getFirstAnswer(), customerId, "ER_FIRST_ANSWER_INVALID");
        verifySecurityAnswer(storedAnswers, request.getSecondQuestionId(), request.getSecondAnswer(), customerId, "ER_SECOND_ANSWER_INVALID");
        verifySecurityAnswer(storedAnswers, request.getThirdQuestionId(), request.getThirdAnswer(), customerId, "ER_THIRD_ANSWER_INVALID");
        log.info("[transferQuotationVerify] security questions verified ok customerId={}", customerId);

        // 3. Call CBS P2P transfer (MOCKUP)
        String transactionId = commonInfo.genTransactionId("P2P");
        log.info("[transferQuotationVerify] calling CBS p2pTransfer txnId={} goldWeight={}", transactionId, quotation.getGoldWeight());
        ApiCoreBanking.CbsP2PTransferResult cbsResult = apiCoreBanking.p2pTransfer(
                transactionId, customerId,
                quotation.getDrAccountNo(), quotation.getCrAccountNo(),
                quotation.getGoldWeight(), quotation.getMemo());
        log.info("[transferQuotationVerify] CBS p2pTransfer success slipCode={}", cbsResult.slipCode());

        // 4. Save P2P_TRANSACTION record
        LocalDateTime now = LocalDateTime.now();
        P2PTransaction txn = new P2PTransaction();
        txn.setTransactionId(cbsResult.transactionId());
        txn.setCustomerId(customerId);
        txn.setCoreBankingSeqno(cbsResult.slipCode());
        txn.setDrAccountNo(quotation.getDrAccountNo());
        txn.setDrCurrencyCode(quotation.getDrCcy());
        txn.setCrAccountNo(quotation.getCrAccountNo());
        txn.setCrCurrencyCode(quotation.getCrCcy());
        txn.setGoldWeight(quotation.getGoldWeight());
        txn.setFeeAmount(BigDecimal.ZERO);
        txn.setFeeCurrencyCode(quotation.getDrCcy());
        txn.setTransactionDate(now);
        txn.setCreatedAt(now);
        txn.setRemark(quotation.getMemo());
        p2pTransactionRepository.save(txn);
        log.info("[transferQuotationVerify] P2P_TRANSACTION saved txnId={}", cbsResult.transactionId());

        // 5. Mark quotation as used
        quotation.setStatus("COMPLETED");
        p2pQuotationRepository.save(quotation);

        // 6. Build response
        P2PTransferVerifyResponse.TransferData data = new P2PTransferVerifyResponse.TransferData();
        data.setTransactionId(cbsResult.transactionId());
        data.setSlipCode(cbsResult.slipCode());
        data.setTranDate(now.format(TRAN_DATE_FMT));
        data.setDrAccountNo(quotation.getDrAccountNo());
        data.setDrAccountName(quotation.getDrAccountName());
        data.setDrAccountCcy(quotation.getDrCcy());
        data.setCrAccountNo(quotation.getCrAccountNo());
        data.setCrAccountName(quotation.getCrAccountName());
        data.setCrAccountCcy(quotation.getCrCcy());
        data.setGoldWeight(quotation.getGoldWeight());
        data.setMemo(quotation.getMemo());
        data.setFee(BigDecimal.ZERO);

        P2PTransferVerifyResponse response = new P2PTransferVerifyResponse();
        response.setStatus("success");
        response.setData(data);

        log.info("[transferQuotationVerify] completed txnId={} slipCode={} goldWeight={} duration_ms={}",
                cbsResult.transactionId(), cbsResult.slipCode(), quotation.getGoldWeight(),
                System.currentTimeMillis() - start);
        return response;
    }

    private void verifySecurityAnswer(Map<String, String> stored, String questionId, String answer,
                                      String customerId, String errorCode) {
        String hash = stored.get(questionId);
        if (hash == null || !PASSWORD_ENCODER.matches(answer, hash)) {
            log.warn("[transferQuotationVerify] security question failed customerId={} questionId={} errorCode={}",
                    customerId, questionId, errorCode);
            throw new BusinessException(errorCode, "Security answer is incorrect");
        }
    }
}
