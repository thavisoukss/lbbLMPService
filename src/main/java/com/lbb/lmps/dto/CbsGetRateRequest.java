package com.lbb.lmps.dto;

import lombok.Data;

@Data
public class CbsGetRateRequest {
    private String branch;
    private String ccy;
    private boolean historyYn;
    private String xrateType;
}