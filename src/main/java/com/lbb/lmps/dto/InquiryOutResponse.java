package com.lbb.lmps.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

@Data
public class InquiryOutResponse {
    private String status;
    private InquiryOutData data;
    @JsonProperty("x_nonce")
    private String xNonce;
    private List<SecurityQuestionDto> questions;
}