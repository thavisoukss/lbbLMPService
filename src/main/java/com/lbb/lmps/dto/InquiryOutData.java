package com.lbb.lmps.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.math.BigDecimal;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class InquiryOutData {
    private BigDecimal txnAmount;
    private String txnId;
    private String frommember;
    private String fromuser;
    private String fromaccount;
    private String toType;
    private String toaccount;
    private String tomember;
    private String tomembername;
    private String tomemberimage;
    private String reference;
    private String accountname;
    private String accountccy;
    private FeeList feelist;
}
