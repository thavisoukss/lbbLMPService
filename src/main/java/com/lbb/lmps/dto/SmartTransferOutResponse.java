package com.lbb.lmps.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class SmartTransferOutResponse {
    private String responseCode;
    private String responseMessage;
    private String responseStatus;
    private String responseTimestamp;
    private ClientInfo clientInfo;
    private SmartTransferOutData data;
}