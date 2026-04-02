package com.lbb.lmps.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class SmartMemberListResponse {

    private String responseCode;
    private String responseMessage;
    private String responseStatus;
    private String responseTimestamp;
    private ClientInfo clientInfo;
    private List<MemberData> data;

    public SmartMemberListResponse() {}

    public String getResponseCode() { return responseCode; }
    public void setResponseCode(String responseCode) { this.responseCode = responseCode; }

    public String getResponseMessage() { return responseMessage; }
    public void setResponseMessage(String responseMessage) { this.responseMessage = responseMessage; }

    public String getResponseStatus() { return responseStatus; }
    public void setResponseStatus(String responseStatus) { this.responseStatus = responseStatus; }

    public String getResponseTimestamp() { return responseTimestamp; }
    public void setResponseTimestamp(String responseTimestamp) { this.responseTimestamp = responseTimestamp; }

    public ClientInfo getClientInfo() { return clientInfo; }
    public void setClientInfo(ClientInfo clientInfo) { this.clientInfo = clientInfo; }

    public List<MemberData> getData() { return data; }
    public void setData(List<MemberData> data) { this.data = data; }
}
