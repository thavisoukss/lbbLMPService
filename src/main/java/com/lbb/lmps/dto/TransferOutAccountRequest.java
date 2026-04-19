package com.lbb.lmps.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.math.BigDecimal;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class TransferOutAccountRequest {
    @JsonProperty("x_nonce")
    private String xNonce;
    @JsonProperty("to_account")
    private String toAccount;
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