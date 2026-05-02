package com.lbb.lmps.dto;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class GoldRateResponse {
    private String code;
    private int status;
    private String message;
    private long journalNo;
    private RateData data;

    @Data
    public static class RateData {
        private BigDecimal midRate;
        private BigDecimal buyRate;
        private BigDecimal sellRate;
    }
}
