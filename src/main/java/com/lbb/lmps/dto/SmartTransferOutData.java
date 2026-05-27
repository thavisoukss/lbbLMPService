package com.lbb.lmps.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.math.BigDecimal;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class SmartTransferOutData {
    private String txnId;
    private String cbsRefNo;
    private String txnType;
    private BigDecimal txnAmount;
    private BigDecimal txnFee;
    private String txnCcy;
    private String purpose;
    private String fromUserId;
    private String fromCustName;
    private String fromAcctId;
    private String fromCif;
    private String toCustName;
    private String toAcctId;
    private String toCif;
    private String toType;
    private String toMemberId;
}