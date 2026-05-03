package com.lbb.lmps.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name = "P2P_TXN_DETAIL")
public class P2PTxnDetail {

    @Id
    @Column(name = "TXN_ID", nullable = false, length = 50)
    private String txnId;

    @Column(name = "CUSTOMER_ID", nullable = false, length = 50)
    private String customerId;

    @Column(name = "CBS_REF_NO", length = 100)
    private String cbsRefNo;

    @Column(name = "DR_ACCOUNT_NO", nullable = false, length = 50)
    private String drAccountNo;

    @Column(name = "DR_ACCOUNT_NAME", nullable = false, length = 200)
    private String drAccountName;

    @Column(name = "DR_CCY", nullable = false, length = 3)
    private String drCcy;

    @Column(name = "CR_ACCOUNT_NO", nullable = false, length = 50)
    private String crAccountNo;

    @Column(name = "CR_ACCOUNT_NAME", nullable = false, length = 200)
    private String crAccountName;

    @Column(name = "CR_CCY", nullable = false, length = 3)
    private String crCcy;

    @Column(name = "GOLD_WEIGHT", nullable = false, precision = 12, scale = 4)
    private BigDecimal goldWeight;

    @Column(name = "TOTAL_AMOUNT", nullable = false, precision = 18, scale = 4)
    private BigDecimal totalAmount;

    @Column(name = "MEMO", length = 500)
    private String memo;

    @Column(name = "STATUS", nullable = false, length = 20)
    private String status;

    @Column(name = "CREATED_AT", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "EXPIRED_AT", nullable = false)
    private LocalDateTime expiredAt;
}
