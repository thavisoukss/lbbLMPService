package com.lbb.lmps.model.lmps;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Data;

@Data
public class LmpsOutTrf {
    private static final ObjectMapper mapper = new ObjectMapper();
    private String lmpsRef;
    private String txnId;
    private String toType;
    private String txnType;     // LMPS
    private String txnAmount;
    private String txnFee;
    private String txnCcy;
    private String purpose;
    private String fromUserId;
    private String fromCustName;
    private String fromAcctId;
    private String toCustName;
    private String toAcctId;
    private String toMemberId;
    @Override
    public String toString() {
        try {
            return mapper.writeValueAsString(this);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }
}
