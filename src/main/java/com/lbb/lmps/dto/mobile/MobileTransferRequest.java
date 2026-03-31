package com.lbb.lmps.dto.mobile;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class MobileTransferRequest {

    @NotBlank
    @JsonProperty("deviceId")
    private String deviceId;

    @NotBlank
    @JsonProperty("userId")
    private String userId;

    @NotBlank
    @JsonProperty("mobileNo")
    private String mobileNo;

    @NotBlank
    @JsonProperty("txnId")
    private String txnId;

    /** LMPOTA, QRLO, QRAC, ACTOAC, MQRPYA */
    @NotBlank
    @JsonProperty("txnType")
    private String txnType;

    @NotBlank
    @JsonProperty("fromAcctId")
    private String fromAcctId;

    @JsonProperty("fromCif")
    private String fromCif;

    @JsonProperty("fromCustName")
    private String fromCustName;

    /** ACCOUNT or QR */
    @NotBlank
    @JsonProperty("toType")
    private String toType;

    @NotBlank
    @JsonProperty("toAcctId")
    private String toAcctId;

    @JsonProperty("toCif")
    private String toCif;

    @JsonProperty("toCustName")
    private String toCustName;

    @JsonProperty("toMemberId")
    private String toMemberId;

    @JsonProperty("txnAmount")
    private BigDecimal txnAmount;

    @JsonProperty("txnCcy")
    private String txnCcy;

    @JsonProperty("purpose")
    private String purpose;
}