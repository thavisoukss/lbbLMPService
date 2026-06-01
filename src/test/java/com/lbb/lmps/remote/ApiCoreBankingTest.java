package com.lbb.lmps.remote;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lbb.lmps.dto.CbsInternalTransferResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ApiCoreBankingTest {

    @Mock RestClient restClient;
    @Mock RestClient.Builder builder;

    private ApiCoreBanking apiCoreBanking;

    @BeforeEach
    void setUp() {
        when(builder.baseUrl(anyString())).thenReturn(builder);
        when(builder.defaultHeader(any(), any())).thenReturn(builder);
        when(builder.requestFactory(any())).thenReturn(builder);
        when(builder.build()).thenReturn(restClient);

        apiCoreBanking = new ApiCoreBanking(builder, new ObjectMapper(), "http://test-cbs");
        ReflectionTestUtils.setField(apiCoreBanking, "pathRoot", "/api/corebanking");
        ReflectionTestUtils.setField(apiCoreBanking, "pathGetRate", "/getRate");
        ReflectionTestUtils.setField(apiCoreBanking, "pathP2PTransfer", "/internalTransfer");
    }

    @Test
    void p2pTransfer_success_mapsJournalNoAndSeqNosToResult() {
        CbsInternalTransferResponse.TfrDetail tfrDetail = new CbsInternalTransferResponse.TfrDetail();
        tfrDetail.setDrSeqNo(51944L);
        tfrDetail.setCrSeqNo(51945L);

        CbsInternalTransferResponse.Details details = new CbsInternalTransferResponse.Details();
        details.setSeqNo(51944L);
        details.setTfrDetailList(List.of(tfrDetail));

        CbsInternalTransferResponse cbsResponse = new CbsInternalTransferResponse();
        cbsResponse.setCode("CBS.MESSAGE.SUCCESS");
        cbsResponse.setStatus(200);
        cbsResponse.setJournalNo(4116112L);
        cbsResponse.setDetails(details);

        mockRestClientChain(cbsResponse);

        ApiCoreBanking.CbsP2PTransferResult result = apiCoreBanking.p2pTransfer(
                "TXN-001", "CUST-001", "DR-ACCT-001", "CR-ACCT-002",
                new BigDecimal("1.5"), "test memo");

        assertThat(result.transactionId()).isEqualTo("4116112");
        assertThat(result.slipCode()).isEqualTo("51944");
        assertThat(result.drCbsSeqno()).isEqualTo("51944");
        assertThat(result.crCbsSeqno()).isEqualTo("51945");
    }

    @Test
    void p2pTransfer_cbsNonSuccessCode_throwsRuntimeException() {
        CbsInternalTransferResponse cbsResponse = new CbsInternalTransferResponse();
        cbsResponse.setCode("CBS.MESSAGE.ERROR");
        cbsResponse.setMessage("Insufficient balance");

        mockRestClientChain(cbsResponse);

        assertThatThrownBy(() -> apiCoreBanking.p2pTransfer(
                "TXN-001", "CUST-001", "DR-ACCT-001", "CR-ACCT-002",
                new BigDecimal("1.5"), "test memo"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("CBS error:");
    }

    @Test
    void p2pTransfer_networkError_throwsRuntimeException() {
        RestClient.RequestBodyUriSpec postSpec = mock(RestClient.RequestBodyUriSpec.class);
        RestClient.RequestBodySpec bodySpec = mock(RestClient.RequestBodySpec.class);
        RestClient.ResponseSpec responseSpec = mock(RestClient.ResponseSpec.class);

        doReturn(postSpec).when(restClient).post();
        doReturn(bodySpec).when(postSpec).uri(anyString());
        doReturn(bodySpec).when(bodySpec).body(any(Object.class));
        doReturn(responseSpec).when(bodySpec).retrieve();
        doThrow(new RuntimeException("Connection refused")).when(responseSpec).body(String.class);

        assertThatThrownBy(() -> apiCoreBanking.p2pTransfer(
                "TXN-001", "CUST-001", "DR-ACCT-001", "CR-ACCT-002",
                new BigDecimal("1.5"), "test memo"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("CBS error:");
    }

    private void mockRestClientChain(CbsInternalTransferResponse response) {
        RestClient.RequestBodyUriSpec postSpec = mock(RestClient.RequestBodyUriSpec.class);
        RestClient.RequestBodySpec bodySpec = mock(RestClient.RequestBodySpec.class);
        RestClient.ResponseSpec responseSpec = mock(RestClient.ResponseSpec.class);

        doReturn(postSpec).when(restClient).post();
        doReturn(bodySpec).when(postSpec).uri(anyString());
        doReturn(bodySpec).when(bodySpec).body(any(Object.class));
        doReturn(responseSpec).when(bodySpec).retrieve();
        try {
            String json = new ObjectMapper().writeValueAsString(response);
            doReturn(json).when(responseSpec).body(String.class);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
