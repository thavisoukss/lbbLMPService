package com.lbb.lmps.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class P2PAccountInfoRequest {

    @JsonProperty("cr_phone")
    private String crPhone;
}