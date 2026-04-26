package com.lbb.lmps.dto;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class GoldRateResponse {
    private BigDecimal buyRate;
    private BigDecimal sellRate;
    private String currency;
}
