package com.lbb.lmps.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.math.BigDecimal;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class TransferOutAccountBioRequest {
    @NotBlank
    @JsonProperty("x_nonce")
    private String xNonce;
    @NotBlank
    @JsonProperty("to_account")
    private String toAccount;
    @NotNull @Positive
    private BigDecimal amount;
    private String purpose;
    @NotBlank
    private String timestamp;
    @NotBlank
    private String secret;
    @NotBlank
    private String signature;
}
