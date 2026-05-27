package com.lbb.lmps.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class P2PAccountInfoResponse {

    private String status;
    private P2PAccountInfoData data;

    @Override
    public String toString() {
        return "P2PAccountInfoResponse{status='" + status + "', accountNo=" + (data != null ? data.getAccountNo() : null) + "}";
    }
}
