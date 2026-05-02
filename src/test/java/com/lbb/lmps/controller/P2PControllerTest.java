package com.lbb.lmps.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lbb.lmps.dto.P2PInquiryRequest;
import com.lbb.lmps.dto.P2PInquiryResponse;
import com.lbb.lmps.exception.ResourceNotFoundException;
import com.lbb.lmps.service.P2PService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(P2PController.class)
@AutoConfigureMockMvc(addFilters = false)
class P2PControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @MockBean P2PService p2pService;

    @Test
    void inquiry_returns200_withSuccessBody() throws Exception {
        when(p2pService.inquiry(any())).thenReturn(successResponse());

        mockMvc.perform(post("/p2p/inquiry")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(inquiryRequest())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.data.ref").value("REF-123"))
                .andExpect(jsonPath("$.data.ttl").value(180))
                .andExpect(jsonPath("$.data.dr_account_no").value("DR-001"))
                .andExpect(jsonPath("$.data.cr_account_no").value("CR-002"))
                .andExpect(jsonPath("$.data.total_amount").value(250.00))
                .andExpect(jsonPath("$.data.gold_weight").value(2.5))
                .andExpect(jsonPath("$.data.memo").value("test memo"));
    }

    @Test
    void inquiry_returns404_whenResourceNotFound() throws Exception {
        when(p2pService.inquiry(any()))
                .thenThrow(new ResourceNotFoundException("Creditor not found"));

        mockMvc.perform(post("/p2p/inquiry")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(inquiryRequest())))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value("error"))
                .andExpect(jsonPath("$.error.code").value("RESOURCE_NOT_FOUND"));
    }

    // --- helpers ---

    private P2PInquiryRequest inquiryRequest() {
        P2PInquiryRequest req = new P2PInquiryRequest();
        req.setGoldWeight(new BigDecimal("2.5"));
        req.setCrPhone("02099999999");
        req.setMemo("test memo");
        return req;
    }

    private P2PInquiryResponse successResponse() {
        P2PInquiryResponse.P2PInquiryData data = new P2PInquiryResponse.P2PInquiryData();
        data.setRef("REF-123");
        data.setTtl(180);
        data.setDrAccountNo("DR-001");
        data.setDrAccountName("John Debtor");
        data.setDrAccountCurrency("LBI");
        data.setCrAccountNo("CR-002");
        data.setCrAccountName("Jane Creditor");
        data.setCrAccountCurrency("LBI");
        data.setTotalAmount(new BigDecimal("250.00"));
        data.setGoldWeight(new BigDecimal("2.5"));
        data.setMemo("test memo");
        data.setQuestions(List.of());

        P2PInquiryResponse response = new P2PInquiryResponse();
        response.setStatus("success");
        response.setData(data);
        return response;
    }
}