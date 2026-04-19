package com.lbb.lmps.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Objects;

@Getter
@Setter
@Entity
@Table(name = "WITHDRAW_TXN")
public class WithdrawTxn {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "withdrawTxnSeq")
    @SequenceGenerator(name = "withdrawTxnSeq", sequenceName = "WITHDRAW_TXN_SEQ", allocationSize = 1)
    @Column(name = "ID")
    private Long id;

    @Column(name = "PAYMENT_CHANNEL_ID", nullable = false)
    private Long paymentChannelId;

    @Column(name = "CUSTOMER_ID", nullable = false)
    private String customerId;

    @Column(name = "TRANSACTION_ID", nullable = false)
    private String transactionId;

    @Column(name = "NONCE", nullable = false)
    private String nonce;

    @Column(name = "PROVIDER_CODE", nullable = false)
    private String providerCode;

    @Column(name = "STATUS", nullable = false)
    private String status;

    @Column(name = "DR_ACCOUNT_NO", nullable = false)
    private String drAccountNo;

    @Column(name = "DR_CIF")
    private String drCif;

    @Column(name = "CR_ACCOUNT_NO")
    private String crAccountNo;

    @Column(name = "DR_ACCOUNT_NAME")
    private String drAccountName;

    @Column(name = "CR_ACCOUNT_NAME")
    private String crAccountName;

    @Column(name = "AMOUNT", nullable = false, precision = 18, scale = 4)
    private BigDecimal amount;

    @Column(name = "FEE_AMT", nullable = false, precision = 18, scale = 4)
    private BigDecimal feeAmt;

    @Column(name = "FEE_PROVIDER_AMT", nullable = false, precision = 18, scale = 4)
    private BigDecimal feeProviderAmt;

    @Column(name = "CURRENCY_CODE", nullable = false, length = 3)
    private String currencyCode;

    @Column(name = "FEE_CURRENCY_CODE", nullable = false, length = 3)
    private String feeCurrencyCode;

    @Column(name = "FEE_PROVIDER_CURRENCY_CODE", nullable = false, length = 3)
    private String feeProviderCurrencyCode;

    @Column(name = "REMARK")
    private String remark;

    // TO_MEMBER — reserved for future use (store recipient member ID to skip QR info re-call on verify)
    // @Column(name = "TO_MEMBER")
    // private String toMember;

    @Column(name = "CORE_BANKING_REF")
    private String coreBankingRef;

    @Column(name = "CREATED_AT", nullable = false)
    private LocalDateTime createdAt;

    @Version
    @Column(name = "VERSION", nullable = false)
    private Long version = 1L;

    @Column(name = "FEE_LIST", length = 4000)
    private String feeList;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof WithdrawTxn w)) return false;
        return id != null && id.equals(w.id);
    }

    @Override
    public int hashCode() { return getClass().hashCode(); }
}
