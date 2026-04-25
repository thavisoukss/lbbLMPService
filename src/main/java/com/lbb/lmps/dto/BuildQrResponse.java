package com.lbb.lmps.dto;

import lombok.Data;

@Data
public class BuildQrResponse {
    private String status;
    private BuildQrData data;

    @Data
    public static class BuildQrData {
        private String accountName;
        private String accountNo;
        private String currency;
        private String qrString;
    }
}
