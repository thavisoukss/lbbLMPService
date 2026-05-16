package com.lbb.lmps.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import java.math.BigDecimal;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class GoldRateResponse {
    private String code;
    private int status;
    private String message;
    private long journalNo;
    private RateData data;

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class RateData {
        private BigDecimal midRate;
        private BigDecimal buyRate;
        private BigDecimal sellRate;
    }
}
