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

    public void setTxnAmount(BigDecimal txnAmount) {
        this.txnAmount = txnAmount != null ? txnAmount.setScale(0, java.math.RoundingMode.DOWN) : null;
    }

    public void setTxnFee(BigDecimal txnFee) {
        this.txnFee = txnFee != null ? txnFee.setScale(0, java.math.RoundingMode.DOWN) : null;
    }
}