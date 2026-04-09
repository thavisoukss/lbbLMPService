package com.lbb.lmps.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class TransferOutQrResponse {
    @JsonProperty("transaction_id")
    private String transactionId;
    @JsonProperty("slip_code")
    private String slipCode;
    @JsonProperty("tran_date")
    private String tranDate;
    @JsonProperty("total_amount")
    private BigDecimal totalAmount;
    @JsonProperty("currency_code")
    private String currencyCode;
    @JsonProperty("fee_amt")
    private BigDecimal feeAmt;
    @JsonProperty("dr_account_no")
    private String drAccountNo;
    @JsonProperty("dr_account_name")
    private String drAccountName;
    @JsonProperty("cr_account_no")
    private String crAccountNo;
    @JsonProperty("cr_account_name")
    private String crAccountName;
    @JsonProperty("provider_ref")
    private String providerRef;
    private String purpose;
}