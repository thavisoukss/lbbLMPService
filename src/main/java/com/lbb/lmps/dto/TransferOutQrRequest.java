package com.lbb.lmps.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.math.BigDecimal;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class TransferOutQrRequest {
    @JsonProperty("x_nonce")
    private String xNonce;
    @JsonProperty("qr_string")
    private String qrString;
    private BigDecimal amount;
    private String purpose;
    @JsonProperty("first_question_id")
    private String firstQuestionId;
    @JsonProperty("first_answer")
    private String firstAnswer;
    @JsonProperty("second_question_id")
    private String secondQuestionId;
    @JsonProperty("second_answer")
    private String secondAnswer;
    @JsonProperty("third_question_id")
    private String thirdQuestionId;
    @JsonProperty("third_answer")
    private String thirdAnswer;
}