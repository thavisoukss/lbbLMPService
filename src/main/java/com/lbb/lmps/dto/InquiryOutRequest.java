package com.lbb.lmps.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class InquiryOutRequest {

    @JsonProperty("to_account")
    private String toAccount;

    @JsonProperty("to_member")
    private String toMember;
}
