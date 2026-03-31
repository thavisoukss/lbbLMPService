package com.lbb.lmps.dto.mobile;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class MobileLmpsInquiryRequest {

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
    @JsonProperty("fromAccount")
    private String fromAccount;

    @JsonProperty("fromCif")
    private String fromCif;

    /** ACCOUNT or QR */
    @NotBlank
    @JsonProperty("toType")
    private String toType;

    @NotBlank
    @JsonProperty("toAccount")
    private String toAccount;

    @NotBlank
    @JsonProperty("toMember")
    private String toMember;

    @JsonProperty("txnAmount")
    private BigDecimal txnAmount;

    @JsonProperty("txnId")
    private String txnId;
}