package com.lbb.lmps.service;

import com.lbb.lmps.dto.GoldRateResponse;
import com.lbb.lmps.dto.P2PInquiryRequest;
import com.lbb.lmps.dto.P2PInquiryResponse;
import com.lbb.lmps.entity.Account;
import com.lbb.lmps.entity.Customer;
import com.lbb.lmps.exception.ResourceNotFoundException;
import com.lbb.lmps.remote.ApiCoreBanking;
import com.lbb.lmps.repository.AccountRepository;
import com.lbb.lmps.repository.CustomerRepository;
import com.lbb.lmps.repository.SecurityQuestionRepository;
import com.lbb.lmps.repository.SecurityQuestionRepository.SecurityQuestionProjection;
import com.lbb.lmps.service.impl.P2PServiceImpl;
import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class P2PServiceImplTest {

    @Mock CustomerRepository customerRepository;
    @Mock AccountRepository accountRepository;
    @Mock SecurityQuestionRepository securityQuestionRepository;
    @Mock ApiCoreBanking apiCoreBanking;
    @Mock MinioStorageService minioStorageService;

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

        P2PInquiryResponse response = service.inquiry(request);

        assertThat(response.getStatus()).isEqualTo("success");
        P2PInquiryResponse.P2PInquiryData data = response.getData();
        assertThat(data.getRef()).isNotBlank();
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
        when(customerRepository.findById(DEBTOR_ID)).thenReturn(Optional.of(customerWithId(DEBTOR_ID)));
        when(accountRepository.findLbiCurrentByCustomerId(DEBTOR_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.inquiry(requestFor(CREDITOR_PHONE)))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void inquiry_creditorNotFound_throwsResourceNotFoundException() {
        when(customerRepository.findById(DEBTOR_ID)).thenReturn(Optional.of(customerWithId(DEBTOR_ID)));
        when(accountRepository.findLbiCurrentByCustomerId(DEBTOR_ID)).thenReturn(Optional.of(account("DR-001", "John", "LBI")));
        when(customerRepository.findByPhone(CREDITOR_PHONE)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.inquiry(requestFor(CREDITOR_PHONE)))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void inquiry_creditorAccountNotFound_throwsResourceNotFoundException() {
        when(customerRepository.findById(DEBTOR_ID)).thenReturn(Optional.of(customerWithId(DEBTOR_ID)));
        when(accountRepository.findLbiCurrentByCustomerId(DEBTOR_ID)).thenReturn(Optional.of(account("DR-001", "John", "LBI")));
        when(customerRepository.findByPhone(CREDITOR_PHONE)).thenReturn(Optional.of(customerWithId(CREDITOR_ID)));
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
}