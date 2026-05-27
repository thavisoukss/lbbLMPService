package com.lbb.lmps.dto;

import lombok.Data;

@Data
public class SmartQrInfoRequest {
    private ClientInfo clientInfo;
    private SecurityContext securityContext;
    private QrData data;

    @Data
    public static class QrData {
        private String qrString;
    }
}