package com.lbb.lmps.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lbb.lmps.dto.*;
import com.lbb.lmps.entity.*;
import com.lbb.lmps.exception.ResourceNotFoundException;
import com.lbb.lmps.remote.ApiMSmart;
import com.lbb.lmps.remote.ApiNotification;
import com.lbb.lmps.repository.CustomerRepository;
import com.lbb.lmps.repository.LmpsTxnDetailRepository;
import com.lbb.lmps.repository.SecurityQuestionRepository;
import com.lbb.lmps.repository.WithdrawTxnRepository;
import com.lbb.lmps.service.impl.TransferOutServiceImpl;
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

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TransferOutServiceImplTest {

    @Mock ApiMSmart apiMSmart;
    @Mock ApiNotification apiNotification;
    @Mock ObjectMapper mapper;
    @Mock MessageSource messageSource;
    @Mock WithdrawTxnRepository withdrawTxnRepository;
    @Mock LmpsTxnDetailRepository lmpsTxnDetailRepository;
    @Mock SecurityQuestionRepository securityQuestionRepository;
    @Mock CustomerRepository customerRepository;

    @InjectMocks TransferOutServiceImpl service;

    @Mock Claims claims;
    @Mock Authentication authentication;
    @Mock SecurityContext securityContext;

    private static final String CUSTOMER_ID = "CUST-001";

    @BeforeEach
    void setupSecurityContext() {
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getDetails()).thenReturn(claims);
        when(claims.get("user-id")).thenReturn(CUSTOMER_ID);
        SecurityContextHolder.setContext(securityContext);
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void transferOutQrBio_customerNotFound_throwsResourceNotFoundException() {
        TransferOutQrBioRequest request = new TransferOutQrBioRequest();
        when(customerRepository.findById(CUSTOMER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.transferOutQrBio(request, "device-123"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Customer not found: " + CUSTOMER_ID);
    }

    @Test
    void transferOutQrBio_bioKeyNull_throwsRuntimeException() {
        TransferOutQrBioRequest request = new TransferOutQrBioRequest();
        Customer customer = mock(Customer.class);
        when(customer.getBioKey()).thenReturn(null);
        when(customerRepository.findById(CUSTOMER_ID)).thenReturn(Optional.of(customer));

        assertThatThrownBy(() -> service.transferOutQrBio(request, "device-123"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Biometric key not registered");
    }

    @Test
    void transferOutAccountBio_customerNotFound_throwsResourceNotFoundException() {
        TransferOutAccountBioRequest request = new TransferOutAccountBioRequest();
        when(customerRepository.findById(CUSTOMER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.transferOutAccountBio(request, "device-123"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Customer not found: " + CUSTOMER_ID);
    }

    @Test
    void transferOutAccountBio_bioKeyNull_throwsRuntimeException() {
        TransferOutAccountBioRequest request = new TransferOutAccountBioRequest();
        Customer customer = mock(Customer.class);
        when(customer.getBioKey()).thenReturn(null);
        when(customerRepository.findById(CUSTOMER_ID)).thenReturn(Optional.of(customer));

        assertThatThrownBy(() -> service.transferOutAccountBio(request, "device-123"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Biometric key not registered");
    }

    @Test
    void transferOutAccount_success() throws Exception {
        TransferOutAccountRequest request = new TransferOutAccountRequest();
        request.setXNonce("x-nonce-123");
        request.setAmount(BigDecimal.TEN);
        request.setFirstQuestionId("Q1");
        request.setFirstAnswer("A1");
        request.setSecondQuestionId("Q2");
        request.setSecondAnswer("A2");
        request.setThirdQuestionId("Q3");
        request.setThirdAnswer("A3");
        request.setToAccount("CR-ACCT");
        request.setPurpose("test purpose");

        when(claims.getSubject()).thenReturn("user-123");
        when(claims.get("user-phone")).thenReturn("02012345678");

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

        when(securityQuestionRepository.findAnswersByCustomerId(CUSTOMER_ID)).thenReturn(List.of(ans1, ans2, ans3));

        WithdrawTxn withdrawTxn = new WithdrawTxn();
        withdrawTxn.setId(10L);
        withdrawTxn.setTransactionId("TXN-123");
        withdrawTxn.setCustomerId(CUSTOMER_ID);
        withdrawTxn.setStatus("DEBIT_PENDING");
        withdrawTxn.setRemark("MEMBER-CODE");
        withdrawTxn.setCurrencyCode("LAK");
        withdrawTxn.setDrAccountNo("DR-ACCT");
        withdrawTxn.setDrAccountName("John");
        withdrawTxn.setDrCif(CUSTOMER_ID);
        withdrawTxn.setCrAccountNo("CR-ACCT");
        withdrawTxn.setCrAccountName("Jane");

        when(withdrawTxnRepository.findByNonce("x-nonce-123")).thenReturn(Optional.of(withdrawTxn));

        LmpsTxnDetail lmpsTxnDetail = new LmpsTxnDetail();
        lmpsTxnDetail.setId(20L);
        lmpsTxnDetail.setTransactionId("TXN-123");

        when(lmpsTxnDetailRepository.findByTransactionId("TXN-123")).thenReturn(Optional.of(lmpsTxnDetail));

        SmartTransferOutResponse mockResponse = new SmartTransferOutResponse();
        mockResponse.setResponseCode("0000");
        SmartTransferOutData data = new SmartTransferOutData();
        data.setTxnId("TXN-123");
        data.setCbsRefNo("CBS-REF-999");
        data.setTxnAmount(BigDecimal.TEN);
        data.setTxnFee(BigDecimal.ZERO);
        data.setTxnCcy("LAK");
        data.setFromAcctId("DR-ACCT");
        data.setFromCustName("John");
        data.setToAcctId("CR-ACCT");
        data.setToCustName("Jane");
        data.setPurpose("test purpose");
        mockResponse.setData(data);

        when(apiMSmart.callTransferOut(org.mockito.ArgumentMatchers.any())).thenReturn("some-raw-json");
        when(mapper.readValue(org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.eq(SmartTransferOutResponse.class)))
                .thenReturn(mockResponse);

        TransferOutQrResponse response = service.transferOutAccount(request, "device-123");

        org.assertj.core.api.Assertions.assertThat(response.getTransactionId()).isEqualTo("CBS-REF-999");
        org.assertj.core.api.Assertions.assertThat(response.getSlipCode()).isEqualTo("CBS-REF-999");
        org.assertj.core.api.Assertions.assertThat(response.getProviderRef()).isEqualTo("CBS-REF-999");
        org.assertj.core.api.Assertions.assertThat(withdrawTxn.getStatus()).isEqualTo("COMPLETED");
        org.assertj.core.api.Assertions.assertThat(withdrawTxn.getCoreBankingRef()).isEqualTo("CBS-REF-999");
        org.assertj.core.api.Assertions.assertThat(lmpsTxnDetail.getStatus()).isEqualTo("COMPLETED");
        org.assertj.core.api.Assertions.assertThat(lmpsTxnDetail.getCoreBankingRef()).isEqualTo("CBS-REF-999");
    }

    @Test
    void transferOutQr_success() throws Exception {
        TransferOutQrRequest request = new TransferOutQrRequest();
        request.setXNonce("x-nonce-123");
        request.setAmount(BigDecimal.TEN);
        request.setFirstQuestionId("Q1");
        request.setFirstAnswer("A1");
        request.setSecondQuestionId("Q2");
        request.setSecondAnswer("A2");
        request.setThirdQuestionId("Q3");
        request.setThirdAnswer("A3");
        request.setQrString("00020101021138670016A00526628466257701082771041802030010324AHOMALYBLVREXGTFJEOYBULW53034185802LA63048D13");
        request.setPurpose("test purpose");

        when(claims.getSubject()).thenReturn("user-123");
        when(claims.get("user-phone")).thenReturn("02012345678");

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

        when(securityQuestionRepository.findAnswersByCustomerId(CUSTOMER_ID)).thenReturn(List.of(ans1, ans2, ans3));

        WithdrawTxn withdrawTxn = new WithdrawTxn();
        withdrawTxn.setId(10L);
        withdrawTxn.setTransactionId("TXN-123");
        withdrawTxn.setCustomerId(CUSTOMER_ID);
        withdrawTxn.setStatus("DEBIT_PENDING");
        withdrawTxn.setRemark("QR");
        withdrawTxn.setCurrencyCode("LAK");
        withdrawTxn.setDrAccountNo("DR-ACCT");
        withdrawTxn.setDrAccountName("John");
        withdrawTxn.setDrCif(CUSTOMER_ID);
        withdrawTxn.setCrAccountNo("CR-ACCT");
        withdrawTxn.setCrAccountName("Jane");

        when(withdrawTxnRepository.findByNonce("x-nonce-123")).thenReturn(Optional.of(withdrawTxn));

        SmartQrInfoResponse qrInfoResponse = new SmartQrInfoResponse();
        qrInfoResponse.setResponseCode("0000");
        QrInfoData qrInfoData = new QrInfoData();
        qrInfoData.setMemberId("BCEL");
        qrInfoResponse.setData(qrInfoData);

        when(apiMSmart.callQrInfo(org.mockito.ArgumentMatchers.any())).thenReturn("some-qr-info-raw");
        when(mapper.readValue(org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.eq(SmartQrInfoResponse.class)))
                .thenReturn(qrInfoResponse);

        LmpsTxnDetail lmpsTxnDetail = new LmpsTxnDetail();
        lmpsTxnDetail.setId(20L);
        lmpsTxnDetail.setTransactionId("TXN-123");

        when(lmpsTxnDetailRepository.findByTransactionId("TXN-123")).thenReturn(Optional.of(lmpsTxnDetail));

        SmartTransferOutResponse mockResponse = new SmartTransferOutResponse();
        mockResponse.setResponseCode("0000");
        SmartTransferOutData data = new SmartTransferOutData();
        data.setTxnId("TXN-123");
        data.setCbsRefNo("CBS-REF-777");
        data.setTxnAmount(BigDecimal.TEN);
        data.setTxnFee(BigDecimal.ZERO);
        data.setTxnCcy("LAK");
        data.setFromAcctId("DR-ACCT");
        data.setFromCustName("John");
        data.setToAcctId(request.getQrString());
        data.setToCustName("Jane");
        data.setPurpose("test purpose");
        mockResponse.setData(data);

        when(apiMSmart.callTransferOut(org.mockito.ArgumentMatchers.any())).thenReturn("some-raw-json");
        when(mapper.readValue(org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.eq(SmartTransferOutResponse.class)))
                .thenReturn(mockResponse);

        TransferOutQrResponse response = service.transferOutQr(request, "device-123");

        org.assertj.core.api.Assertions.assertThat(response.getTransactionId()).isEqualTo("CBS-REF-777");
        org.assertj.core.api.Assertions.assertThat(response.getSlipCode()).isEqualTo("CBS-REF-777");
        org.assertj.core.api.Assertions.assertThat(response.getProviderRef()).isEqualTo("CBS-REF-777");
        org.assertj.core.api.Assertions.assertThat(response.getCrAccountNo()).isEqualTo(request.getQrString());
        org.assertj.core.api.Assertions.assertThat(withdrawTxn.getStatus()).isEqualTo("COMPLETED");
        org.assertj.core.api.Assertions.assertThat(withdrawTxn.getCoreBankingRef()).isEqualTo("CBS-REF-777");
        org.assertj.core.api.Assertions.assertThat(lmpsTxnDetail.getStatus()).isEqualTo("COMPLETED");
        org.assertj.core.api.Assertions.assertThat(lmpsTxnDetail.getCoreBankingRef()).isEqualTo("CBS-REF-777");
    }
}
