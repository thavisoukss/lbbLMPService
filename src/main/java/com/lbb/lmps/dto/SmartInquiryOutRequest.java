package com.lbb.lmps.dto;

import lombok.Data;

@Data
public class SmartInquiryOutRequest {
    private ClientInfo clientInfo;
    private SecurityContext securityContext;
    private SmartInquiryDataRequest data;
}