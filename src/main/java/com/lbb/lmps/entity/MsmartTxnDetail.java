package com.lbb.lmps.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.nnn.msmart.model.ETxnType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.Date;

@Builder
@Entity
@Table(name = "MSMART_TXN_DETAIL")
@JsonIgnoreProperties(ignoreUnknown = true) // Good practice for entities that might be serialized
@Data // Adds getters, setters, equals, hashCode, and toString
@NoArgsConstructor // Required by JPA
@AllArgsConstructor // Useful for creating instances
public class MsmartTxnDetail {
    @Id
    private String txnId;         // unique ID
    @Enumerated(EnumType.STRING)
    private ETxnType txnType;         // QRPM: QR Merchant, QRLO: QR lmps out, QRBG: QR buy gold
    @Enumerated(EnumType.STRING)
    private EMtxnStatus txnStatus;       // PENDING, COMPLETED, FAILED
    private BigDecimal txnAmount;    // Transaction amount
    @Builder.Default
    private BigDecimal txnFee = BigDecimal.ZERO;       // Transaction fee
    private String toType;       // ACCOUNT, QR
    private String billData;       // Bill data for QR payments
    private String billNumber;     // Bill number for QR payments
    private String description;  // Description or particulars of the transaction
    private String purpose;      // purpose of transaction

    private String deviceId;
    private String fromMobileNo;        // user's mobile no
    private String fromUserId;          // Who initiated the transaction
    private String fromCustId;          // Customer ID from CBS
    private String fromCustName;        // Customer name
    private String fromAcctId;     // Source account
    private String fromAcctCcy;    // Account currency, e.g., NPR, USD

    private String toCustName;      // Customer name
    private String toAcctId;        // Destination account
    private String toAcctCcy;       // Account currency, e.g., NPR, USD
    private String toMemberId;      // For LMPS OUT

    private String module;          // e.g., LMPS, MSMART,
    private String txnChannel;      // e.g., MOBILE, INTERNET
    private Date regDateTime;     // register date time YYYYMMDDHHMMSS
    private Date txnDatetime;     // transaction date time YYYYMMDDHHMMSS

    private String valueDate;       // YYYYMMDD

    private String cbsRefNo;        // CBS Reference number
    private String cbsStatus;        // CBS Reference number
    private String clientRef;       // Client reference number e.g: lmps ref
    private String clientStatus;    // Client status e.g: lmps status

    private Integer retryCount;   // Number of retries for failed transactions
    private String errorDetails;    // Error details if any

}
