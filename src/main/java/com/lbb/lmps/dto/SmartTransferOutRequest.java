package com.lbb.lmps.dto;

import lombok.Data;

@Data
public class SmartTransferOutRequest {
    private String requestId;
    private ClientInfo clientInfo;
    private SecurityContext securityContext;
    private SmartTransferOutData data;
}