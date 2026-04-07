package com.lbb.lmps.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class SmartQrInfoResponse {
    private String responseCode;
    private String responseMessage;
    private String responseStatus;
    private QrInfoData data;
}
