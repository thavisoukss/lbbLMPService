package com.lbb.lmps.entity;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class LmpsInquiryDetails {
    private String inqId;

    private String reqBody;

    private String respBody;

    private String fromMember;
    private String fromUser;
    private String fromAccount;
    private String toType;
    private String toAccount;
    private String toMember;
    private String reference;

    private String txnDateTime;

    private String result;

    private String originalMessage;

    private String accountName;
    private String accountLocation;
    private String accountPhone;
    private String invoice;
    private String terminal;

    private String amount;

    private String purposeRequired;
    private String purpose;
    private String accountCcy;
    private String lmpsTxnId;

    private String localDate;

    private String txnType;

    private String billData;

    private String memberId;
    private String memberRefId;

    private BigDecimal dstAmount;
    private String dstCcy;

    private BigDecimal srcAmount;
    private String srcCcy;

    private BigDecimal rate;
    private String rateCcy;

    private String accountId;
    private String fromUserFullName;

    private BigDecimal exchangeRate;

    private String billNumber;
    private String qrId;

    // Apply the same world-class pattern here
    private static final ObjectMapper MAPPER = new ObjectMapper()
            .findAndRegisterModules()
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    @Override
    public String toString() {
        try {
            return MAPPER.writeValueAsString(this);
        } catch (JsonProcessingException e) {
            // Throwing a RuntimeException from toString() can crash services. This is safer.
            return "Error converting to JSON: " + e.getMessage();
        }
    }
}