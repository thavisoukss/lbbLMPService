package com.lbb.lmps.dto;

import lombok.Data;

@Data
public class SmartBuildQrRequest {
    private ClientInfo clientInfo;
    private SecurityContext securityContext;
    private BuildQrData data;

    @Data
    public static class BuildQrData {
        private String qrFor;
        private String custAccountCcy;
        private String custAccount;
        private String additionalCustomerDataRequest;
    }
}