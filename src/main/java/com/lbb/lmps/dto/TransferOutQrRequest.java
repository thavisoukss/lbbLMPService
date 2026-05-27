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
public class TransferOutQrRequest {
    @NotBlank
    @JsonProperty("x_nonce")
    private String xNonce;
    @NotBlank
    @JsonProperty("qr_string")
    private String qrString;
    @NotNull @Positive
    private BigDecimal amount;
    private String purpose;
    @NotBlank
    @JsonProperty("first_question_id")
    private String firstQuestionId;
    @NotBlank
    @JsonProperty("first_answer")
    private String firstAnswer;
    @NotBlank
    @JsonProperty("second_question_id")
    private String secondQuestionId;
    @NotBlank
    @JsonProperty("second_answer")
    private String secondAnswer;
    @NotBlank
    @JsonProperty("third_question_id")
    private String thirdQuestionId;
    @NotBlank
    @JsonProperty("third_answer")
    private String thirdAnswer;
}