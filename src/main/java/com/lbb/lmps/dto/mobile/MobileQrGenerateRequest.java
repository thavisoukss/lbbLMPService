package com.lbb.lmps.dto.mobile;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class MobileQrGenerateRequest {

    @NotBlank
    @JsonProperty("deviceId")
    private String deviceId;

    @NotBlank
    @JsonProperty("userId")
    private String userId;

    @NotBlank
    @JsonProperty("mobileNo")
    private String mobileNo;

    /** A = account, W = wallet */
    @NotBlank
    @JsonProperty("qrFor")
    private String qrFor;

    @NotBlank
    @JsonProperty("custAccount")
    private String custAccount;

    @NotBlank
    @JsonProperty("custAccountCcy")
    private String custAccountCcy;

    @JsonProperty("txnAmount")
    private String txnAmount;

    @JsonProperty("purposeOfTxn")
    private String purposeOfTxn;
}