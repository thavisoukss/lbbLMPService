package com.lbb.lmps.dto;

import lombok.Data;
import java.math.BigDecimal;
import java.util.List;

@Data
public class CbsInternalTransferRequest {
    private String acctNo;
    private String transferMode;
    private String debitTranType;
    private String creditTranType;
    private String checkTellerLimit;
    private List<TfrDetail> tfrDetailList;

    @Data
    public static class TfrDetail {
        private String acctNo;
        private String cpartyAcctNo;
        private String cpartyAcctCcy;
        private String cpartyAcctStatus;
        private String effectDate;
        private BigDecimal cpartyAvailBal;
        private BigDecimal cpartyLedgerBal;
        private boolean remCcy;
        private BigDecimal amount;
        private BigDecimal equivAmount;
        private String drNarrative;
        private BigDecimal crossRate;
        private String tranDate;
        private String branch;
    }
}
