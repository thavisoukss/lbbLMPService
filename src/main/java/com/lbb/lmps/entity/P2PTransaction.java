package com.lbb.lmps.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name = "P2P_TRANSACTION")
public class P2PTransaction {

    @Id
    @Column(name = "TRANSACTION_ID", nullable = false, length = 100)
    private String transactionId;

    @Column(name = "CUSTOMER_ID", nullable = false, length = 50)
    private String customerId;

    @Column(name = "CORE_BANKING_SEQNO", nullable = false, length = 50)
    private String coreBankingSeqno;

    @Column(name = "DR_ACCOUNT_NO", nullable = false, length = 50)
    private String drAccountNo;

    @Column(name = "DR_CURRENCY_CODE", nullable = false, length = 3)
    private String drCurrencyCode;

    @Column(name = "CR_ACCOUNT_NO", nullable = false, length = 50)
    private String crAccountNo;

    @Column(name = "CR_CURRENCY_CODE", nullable = false, length = 3)
    private String crCurrencyCode;

    @Column(name = "GOLD_WEIGHT", nullable = false, precision = 12, scale = 4)
    private BigDecimal goldWeight;

    @Column(name = "FEE_AMOUNT", nullable = false, precision = 12, scale = 4)
    private BigDecimal feeAmount;

    @Column(name = "FEE_CURRENCY_CODE", nullable = false, length = 3)
    private String feeCurrencyCode;

    @Column(name = "TRANSACTION_DATE", nullable = false)
    private LocalDateTime transactionDate;

    @Column(name = "CREATED_AT", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "REMARK", length = 100)
    private String remark;
}
