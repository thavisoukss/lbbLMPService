package com.lbb.lmps.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class QrInfoData {
    private String payloadFormatIndicator;
    private String pointOfInitiation;
    private String applicationId;
    private String iin;
    private String qrPaymentType;
    private String receiverId;
    private String mcc;
    private String txnCurrency;
    private String txnAmount;
    private String countryCode;
    private String name;
    private String city;
    private String purposeOfTxn;
    private String memberId;
    private String memberName;
}
