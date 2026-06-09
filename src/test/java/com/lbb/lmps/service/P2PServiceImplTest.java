package com.lbb.lmps.service;

import com.lbb.lmps.dto.*;
import com.lbb.lmps.entity.*;
import com.lbb.lmps.exception.*;
import com.lbb.lmps.remote.ApiCoreBanking;
import com.lbb.lmps.repository.AccountRepository;
import com.lbb.lmps.repository.CustomerRepository;
import com.lbb.lmps.remote.ApiNotification;
import com.lbb.lmps.repository.P2PTxnDetailRepository;
import com.lbb.lmps.repository.P2PTransactionRepository;
import com.lbb.lmps.repository.SecurityQuestionRepository;
import com.lbb.lmps.repository.SecurityQuestionRepository.SecurityQuestionProjection;
import com.lbb.lmps.service.impl.P2PServiceImpl;
import com.lbb.lmps.utils.CommonInfo;
import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.MessageSource;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class P2PServiceImplTest {

    @Mock CustomerRepository customerRepository;
    @Mock AccountRepository accountRepository;
    @Mock SecurityQuestionRepository securityQuestionRepository;
    @Mock P2PTxnDetailRepository p2pTxnDetailRepository;
    @Mock ApiCoreBanking apiCoreBanking;
    @Mock MinioStorageService minioStorageService;
    @Mock CommonInfo commonInfo;
    @Mock MessageSource messageSource;
    @Mock P2PTransactionRepository p2pTransactionRepository;
    @Mock ApiNotification apiNotification;

    @InjectMocks P2PServiceImpl service;

    @Mock Claims claims;
    @Mock Authentication authentication;
    @Mock SecurityContext securityContext;

    private static final String DEBTOR_ID = "CUST-001";
    private static final String CREDITOR_ID = "CUST-002";
    private static final String CREDITOR_PHONE = "02099999999";

    @BeforeEach
    void setupSecurityContext() {
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getDetails()).thenReturn(claims);
        when(claims.get("user-id")).thenReturn(DEBTOR_ID);
        SecurityContextHolder.setContext(securityContext);

        lenient().when(messageSource.getMessage(any(), any(), anyString(), any()))
                .thenAnswer(invocation -> invocation.getArgument(2));
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    // --- happy path ---

    @Test
    void inquiry_success_returnsResponseWithCalculatedTotalAmount() {
        P2PInquiryRequest request = new P2PInquiryRequest();
        request.setGoldWeight(new BigDecimal("2.5"));
        request.setCrPhone(CREDITOR_PHONE);
        request.setMemo("birthday gift");

        Customer debtor = customerWithId(DEBTOR_ID);
        Account drAccount = account("DR-001", "John Debtor", "LBI");
        Customer creditor = customerWithId(CREDITOR_ID);
        Account crAccount = account("CR-002", "Jane Creditor", "LBI");

        SecurityQuestionProjection q = mock(SecurityQuestionProjection.class);
        when(q.getId()).thenReturn("SQ-1");
        when(q.getDescription()).thenReturn("Mother's maiden name?");

        when(customerRepository.findById(DEBTOR_ID)).thenReturn(Optional.of(debtor));
        when(accountRepository.findLbiCurrentByCustomerId(DEBTOR_ID)).thenReturn(Optional.of(drAccount));
        when(customerRepository.findByPhone(CREDITOR_PHONE)).thenReturn(Optional.of(creditor));
        when(accountRepository.findLbiCurrentByCustomerId(CREDITOR_ID)).thenReturn(Optional.of(crAccount));
        when(apiCoreBanking.getRate()).thenReturn(goldRate(new BigDecimal("100.00")));
        when(securityQuestionRepository.findByCustomerId(DEBTOR_ID)).thenReturn(List.of(q));
        when(commonInfo.genTransactionId(anyString())).thenReturn("P2P-REF-001");

        P2PInquiryResponse response = service.inquiry(request);

        assertThat(response.getStatus()).isEqualTo("success");
        P2PInquiryResponse.P2PInquiryData data = response.getData();
        assertThat(data.getRef()).isEqualTo("P2P-REF-001");
        assertThat(data.getTtl()).isEqualTo(180);
        assertThat(data.getDrAccountNo()).isEqualTo("DR-001");
        assertThat(data.getDrAccountName()).isEqualTo("John Debtor");
        assertThat(data.getCrAccountNo()).isEqualTo("CR-002");
        assertThat(data.getCrAccountName()).isEqualTo("Jane Creditor");
        assertThat(data.getTotalAmount()).isEqualByComparingTo(new BigDecimal("250.00"));
        assertThat(data.getGoldWeight()).isEqualByComparingTo(new BigDecimal("2.5"));
        assertThat(data.getMemo()).isEqualTo("birthday gift");
        assertThat(data.getQuestions()).hasSize(1);
        assertThat(data.getQuestions().get(0).getId()).isEqualTo("SQ-1");
    }

    // --- not-found cases ---

    @Test
    void inquiry_debtorNotFound_throwsResourceNotFoundException() {
        when(customerRepository.findById(DEBTOR_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.inquiry(requestFor(CREDITOR_PHONE)))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void inquiry_debtorAccountNotFound_throwsResourceNotFoundException() {
        Customer debtor = customerWithId(DEBTOR_ID);
        when(customerRepository.findById(DEBTOR_ID)).thenReturn(Optional.of(debtor));
        when(accountRepository.findLbiCurrentByCustomerId(DEBTOR_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.inquiry(requestFor(CREDITOR_PHONE)))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void inquiry_creditorNotFound_throwsResourceNotFoundException() {
        Customer debtor = customerWithId(DEBTOR_ID);
        when(customerRepository.findById(DEBTOR_ID)).thenReturn(Optional.of(debtor));
        when(accountRepository.findLbiCurrentByCustomerId(DEBTOR_ID)).thenReturn(Optional.of(account("DR-001", "John", "LBI")));
        when(customerRepository.findByPhone(CREDITOR_PHONE)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.inquiry(requestFor(CREDITOR_PHONE)))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void inquiry_creditorAccountNotFound_throwsResourceNotFoundException() {
        Customer debtor = customerWithId(DEBTOR_ID);
        Customer creditor = customerWithId(CREDITOR_ID);
        when(customerRepository.findById(DEBTOR_ID)).thenReturn(Optional.of(debtor));
        when(accountRepository.findLbiCurrentByCustomerId(DEBTOR_ID)).thenReturn(Optional.of(account("DR-001", "John", "LBI")));
        when(customerRepository.findByPhone(CREDITOR_PHONE)).thenReturn(Optional.of(creditor));
        when(accountRepository.findLbiCurrentByCustomerId(CREDITOR_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.inquiry(requestFor(CREDITOR_PHONE)))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // --- helpers ---

    private P2PInquiryRequest requestFor(String crPhone) {
        P2PInquiryRequest req = new P2PInquiryRequest();
        req.setGoldWeight(BigDecimal.ONE);
        req.setCrPhone(crPhone);
        return req;
    }

    private Customer customerWithId(String id) {
        Customer c = mock(Customer.class);
        when(c.getId()).thenReturn(id);
        return c;
    }

    private Account account(String accountNo, String accountName, String currency) {
        Account a = new Account();
        a.setAccountNo(accountNo);
        a.setAccountName(accountName);
        a.setAccountCurrency(currency);
        return a;
    }

    private GoldRateResponse goldRate(BigDecimal sellRate) {
        GoldRateResponse.RateData rateData = new GoldRateResponse.RateData();
        rateData.setSellRate(sellRate);
        GoldRateResponse rate = new GoldRateResponse();
        rate.setData(rateData);
        return rate;
    }

    // --- transferQuotationVerify tests ---

    @Test
    void transferQuotationVerify_success() {
        P2PTransferVerifyRequest req = new P2PTransferVerifyRequest();
        req.setRef("REF-001");
        req.setFirstQuestionId("Q1");
        req.setFirstAnswer("A1");
        req.setSecondQuestionId("Q2");
        req.setSecondAnswer("A2");
        req.setThirdQuestionId("Q3");
        req.setThirdAnswer("A3");

        P2PTxnDetail txnDetail = new P2PTxnDetail();
        txnDetail.setTxnId("REF-001");
        txnDetail.setCustomerId(DEBTOR_ID);
        txnDetail.setStatus("PENDING");
        txnDetail.setExpiredAt(java.time.LocalDateTime.now().plusMinutes(5));
        txnDetail.setDrAccountNo("DR-001");
        txnDetail.setDrAccountName("John");
        txnDetail.setDrCcy("LBI");
        txnDetail.setCrAccountNo("CR-002");
        txnDetail.setCrAccountName("Jane");
        txnDetail.setCrCcy("LBI");
        txnDetail.setGoldWeight(BigDecimal.TEN);
        txnDetail.setPurpose("p2p");

        when(p2pTxnDetailRepository.findByIdForUpdate("REF-001")).thenReturn(Optional.of(txnDetail));

        org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder encoder = new org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder(12);
        String hash1 = encoder.encode("A1");
        String hash2 = encoder.encode("A2");
        String hash3 = encoder.encode("A3");

        SecurityQuestionRepository.CustomerAnswerProjection ans1 = mock(SecurityQuestionRepository.CustomerAnswerProjection.class);
        when(ans1.getQuestionId()).thenReturn("Q1");
        when(ans1.getAnswer()).thenReturn(hash1);

        SecurityQuestionRepository.CustomerAnswerProjection ans2 = mock(SecurityQuestionRepository.CustomerAnswerProjection.class);
        when(ans2.getQuestionId()).thenReturn("Q2");
        when(ans2.getAnswer()).thenReturn(hash2);

        SecurityQuestionRepository.CustomerAnswerProjection ans3 = mock(SecurityQuestionRepository.CustomerAnswerProjection.class);
        when(ans3.getQuestionId()).thenReturn("Q3");
        when(ans3.getAnswer()).thenReturn(hash3);

        when(securityQuestionRepository.findAnswersByCustomerId(DEBTOR_ID)).thenReturn(List.of(ans1, ans2, ans3));
        when(commonInfo.genTransactionId("P2P")).thenReturn("P2P-TXN-001");
        when(apiCoreBanking.p2pTransfer("P2P-TXN-001", DEBTOR_ID, "DR-001", "CR-002", BigDecimal.TEN, "p2p"))
                .thenReturn(new ApiCoreBanking.CbsP2PTransferResult("CBS-REF-123", "SLP-456", "DR-SEQ", "CR-SEQ"));

        P2PTransferVerifyResponse response = service.transferQuotationVerify(req);
        assertThat(response.getStatus()).isEqualTo("success");
        assertThat(response.getData().getTransactionId()).isEqualTo("CBS-REF-123");
        assertThat(response.getData().getSlipCode()).isEqualTo("SLP-456");
        assertThat(txnDetail.getStatus()).isEqualTo("COMPLETED");

        org.mockito.ArgumentCaptor<P2PTransaction> captor = org.mockito.ArgumentCaptor.forClass(P2PTransaction.class);
        org.mockito.Mockito.verify(p2pTransactionRepository).save(captor.capture());
        P2PTransaction savedTx = captor.getValue();
        assertThat(savedTx.getTransactionId()).isEqualTo("CBS-REF-123");
        assertThat(savedTx.getCustomerId()).isEqualTo(DEBTOR_ID);
        assertThat(savedTx.getCoreBankingSeqno()).isEqualTo("DR-SEQ");
        assertThat(savedTx.getDrAccountNo()).isEqualTo("DR-001");
        assertThat(savedTx.getDrCurrencyCode()).isEqualTo("LBI");
        assertThat(savedTx.getCrAccountNo()).isEqualTo("CR-002");
        assertThat(savedTx.getCrCurrencyCode()).isEqualTo("LBI");
        assertThat(savedTx.getGoldWeight()).isEqualByComparingTo(BigDecimal.TEN);
        assertThat(savedTx.getFeeAmount()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(savedTx.getFeeCurrencyCode()).isEqualTo("LBI");
        assertThat(savedTx.getCrCbkSeqno()).isEqualTo("CR-SEQ");
        assertThat(savedTx.getRemark()).isEqualTo("p2p");
        assertThat(savedTx.getTransactionDate()).isNotNull();
        assertThat(savedTx.getCreatedAt()).isNotNull();
    }

    @Test
    void transferQuotationVerify_duplicateQuestionIds_throwsSecurityQuestionException() {
        P2PTransferVerifyRequest req = new P2PTransferVerifyRequest();
        req.setRef("REF-001");
        req.setFirstQuestionId("Q1");
        req.setFirstAnswer("A1");
        req.setSecondQuestionId("Q1"); // Duplicate!
        req.setSecondAnswer("A2");
        req.setThirdQuestionId("Q3");
        req.setThirdAnswer("A3");

        P2PTxnDetail txnDetail = new P2PTxnDetail();
        txnDetail.setTxnId("REF-001");
        txnDetail.setCustomerId(DEBTOR_ID);
        txnDetail.setStatus("PENDING");
        txnDetail.setExpiredAt(java.time.LocalDateTime.now().plusMinutes(5));

        when(p2pTxnDetailRepository.findByIdForUpdate("REF-001")).thenReturn(Optional.of(txnDetail));

        assertThatThrownBy(() -> service.transferQuotationVerify(req))
                .isInstanceOf(SecurityQuestionException.class)
                .hasMessageContaining("Security question IDs must be distinct");
    }

    @Test
    void transferQuotationVerify_missingQuestionId_throwsSecurityQuestionException() {
        P2PTransferVerifyRequest req = new P2PTransferVerifyRequest();
        req.setRef("REF-001");
        req.setFirstQuestionId("Q1");
        req.setFirstAnswer("A1");
        req.setSecondQuestionId(null); // Missing!
        req.setSecondAnswer("A2");
        req.setThirdQuestionId("Q3");
        req.setThirdAnswer("A3");

        P2PTxnDetail txnDetail = new P2PTxnDetail();
        txnDetail.setTxnId("REF-001");
        txnDetail.setCustomerId(DEBTOR_ID);
        txnDetail.setStatus("PENDING");
        txnDetail.setExpiredAt(java.time.LocalDateTime.now().plusMinutes(5));

        when(p2pTxnDetailRepository.findByIdForUpdate("REF-001")).thenReturn(Optional.of(txnDetail));

        assertThatThrownBy(() -> service.transferQuotationVerify(req))
                .isInstanceOf(SecurityQuestionException.class)
                .hasMessageContaining("Security question IDs are missing");
    }

    @Test
    void transferQuotationVerify_nullAnswer_throwsSecurityQuestionException() {
        P2PTransferVerifyRequest req = new P2PTransferVerifyRequest();
        req.setRef("REF-001");
        req.setFirstQuestionId("Q1");
        req.setFirstAnswer(null); // Null!
        req.setSecondQuestionId("Q2");
        req.setSecondAnswer("A2");
        req.setThirdQuestionId("Q3");
        req.setThirdAnswer("A3");

        P2PTxnDetail txnDetail = new P2PTxnDetail();
        txnDetail.setTxnId("REF-001");
        txnDetail.setCustomerId(DEBTOR_ID);
        txnDetail.setStatus("PENDING");
        txnDetail.setExpiredAt(java.time.LocalDateTime.now().plusMinutes(5));

        when(p2pTxnDetailRepository.findByIdForUpdate("REF-001")).thenReturn(Optional.of(txnDetail));

        SecurityQuestionRepository.CustomerAnswerProjection ans1 = mock(SecurityQuestionRepository.CustomerAnswerProjection.class);
        when(ans1.getQuestionId()).thenReturn("Q1");
        when(ans1.getAnswer()).thenReturn("somehash");

        SecurityQuestionRepository.CustomerAnswerProjection ans2 = mock(SecurityQuestionRepository.CustomerAnswerProjection.class);
        when(ans2.getQuestionId()).thenReturn("Q2");
        when(ans2.getAnswer()).thenReturn("somehash");

        SecurityQuestionRepository.CustomerAnswerProjection ans3 = mock(SecurityQuestionRepository.CustomerAnswerProjection.class);
        when(ans3.getQuestionId()).thenReturn("Q3");
        when(ans3.getAnswer()).thenReturn("somehash");

        when(securityQuestionRepository.findAnswersByCustomerId(DEBTOR_ID)).thenReturn(List.of(ans1, ans2, ans3));

        assertThatThrownBy(() -> service.transferQuotationVerify(req))
                .isInstanceOf(SecurityQuestionException.class)
                .hasMessageContaining("Security answer is incorrect");
    }

    @Test
    void transferQuotationVerify_expired_throwsBusinessException() {
        P2PTransferVerifyRequest req = new P2PTransferVerifyRequest();
        req.setRef("REF-001");

        P2PTxnDetail txnDetail = new P2PTxnDetail();
        txnDetail.setTxnId("REF-001");
        txnDetail.setCustomerId(DEBTOR_ID);
        txnDetail.setStatus("PENDING");
        txnDetail.setExpiredAt(java.time.LocalDateTime.now().minusMinutes(1)); // Expired!

        when(p2pTxnDetailRepository.findByIdForUpdate("REF-001")).thenReturn(Optional.of(txnDetail));

        assertThatThrownBy(() -> service.transferQuotationVerify(req))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Quotation has expired");
    }

    @Test
    void transferQuotationVerify_alreadyUsed_throwsBusinessException() {
        P2PTransferVerifyRequest req = new P2PTransferVerifyRequest();
        req.setRef("REF-001");

        P2PTxnDetail txnDetail = new P2PTxnDetail();
        txnDetail.setTxnId("REF-001");
        txnDetail.setCustomerId(DEBTOR_ID);
        txnDetail.setStatus("COMPLETED"); // Already completed!
        txnDetail.setExpiredAt(java.time.LocalDateTime.now().plusMinutes(5));

        when(p2pTxnDetailRepository.findByIdForUpdate("REF-001")).thenReturn(Optional.of(txnDetail));

        assertThatThrownBy(() -> service.transferQuotationVerify(req))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Quotation has already been used");
    }

    @Test
    void transferQuotationVerify_ownershipMismatch_throwsResourceNotFoundException() {
        P2PTransferVerifyRequest req = new P2PTransferVerifyRequest();
        req.setRef("REF-001");

        P2PTxnDetail txnDetail = new P2PTxnDetail();
        txnDetail.setTxnId("REF-001");
        txnDetail.setCustomerId("OTHER-CUSTOMER"); // Ownership mismatch!
        txnDetail.setStatus("PENDING");
        txnDetail.setExpiredAt(java.time.LocalDateTime.now().plusMinutes(5));

        when(p2pTxnDetailRepository.findByIdForUpdate("REF-001")).thenReturn(Optional.of(txnDetail));

        assertThatThrownBy(() -> service.transferQuotationVerify(req))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Quotation not found or expired");
    }
}
