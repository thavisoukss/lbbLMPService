package com.lbb.lmps.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import java.math.BigDecimal;
import java.util.List;

@Data
public class P2PInquiryResponse {
    private String status;
    private P2PInquiryData data;

    @Data
    public static class P2PInquiryData {
        private String ref;
        private int ttl;

        @JsonProperty("dr_account_no")
        private String drAccountNo;
        @JsonProperty("dr_account_name")
        private String drAccountName;
        @JsonProperty("dr_account_currency")
        private String drAccountCurrency;

        @JsonProperty("cr_account_no")
        private String crAccountNo;
        @JsonProperty("cr_account_name")
        private String crAccountName;
        @JsonProperty("cr_account_currency")
        private String crAccountCurrency;

        @JsonProperty("total_amount")
        private BigDecimal totalAmount;
        @JsonProperty("gold_weight")
        private BigDecimal goldWeight;
        private String memo;

        private List<SecurityQuestionDto> questions;
    }
}
