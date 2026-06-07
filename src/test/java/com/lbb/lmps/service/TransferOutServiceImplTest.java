package com.lbb.lmps.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lbb.lmps.dto.TransferOutQrBioRequest;
import com.lbb.lmps.dto.TransferOutAccountBioRequest;
import com.lbb.lmps.entity.Customer;
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
}
