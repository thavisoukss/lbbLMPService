package com.lbb.lmps.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class P2PTransferVerifyRequest {

    private String ref;

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
