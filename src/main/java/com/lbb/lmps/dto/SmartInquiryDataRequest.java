package com.lbb.lmps.dto;

import lombok.Data;

@Data
public class SmartInquiryDataRequest {
    private String txnId;
    private String fromuser;
    private String fromaccount;
    private String fromCif;
    private String toType;
    private String toaccount;
    private String tomember;
}
