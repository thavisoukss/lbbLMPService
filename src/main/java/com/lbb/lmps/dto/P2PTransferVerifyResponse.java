package com.lbb.lmps.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class P2PTransferVerifyResponse {

    private String status;
    private TransferData data;

    @Data
    public static class TransferData {

        @JsonProperty("transaction_id")
        private String transactionId;

        @JsonProperty("slip_code")
        private String slipCode;

        @JsonProperty("tran_date")
        private String tranDate;

        @JsonProperty("dr_account_no")
        private String drAccountNo;

        @JsonProperty("dr_account_name")
        private String drAccountName;

        @JsonProperty("dr_account_ccy")
        private String drAccountCcy;

        @JsonProperty("cr_account_no")
        private String crAccountNo;

        @JsonProperty("cr_account_name")
        private String crAccountName;

        @JsonProperty("cr_account_ccy")
        private String crAccountCcy;

        @JsonProperty("gold_weight")
        private BigDecimal goldWeight;

        private String memo;

        private BigDecimal fee;
    }
}
