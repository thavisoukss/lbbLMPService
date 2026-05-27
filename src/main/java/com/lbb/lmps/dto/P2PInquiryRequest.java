package com.lbb.lmps.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import java.math.BigDecimal;

@Data
public class P2PInquiryRequest {
    @JsonProperty("gold_weight")
    private BigDecimal goldWeight;

    @JsonProperty("cr_phone")
    private String crPhone;

    private String memo;
}
