package com.lbb.lmps.dto;

public class MemberListRequest {

    private ClientInfo clientInfo;
    private SecurityContext securityContext;

    public MemberListRequest() {}

    public ClientInfo getClientInfo() { return clientInfo; }
    public void setClientInfo(ClientInfo clientInfo) { this.clientInfo = clientInfo; }

    public SecurityContext getSecurityContext() { return securityContext; }
    public void setSecurityContext(SecurityContext securityContext) { this.securityContext = securityContext; }

    @Override
    public String toString() {
        return "MemberListRequest{clientInfo=" + clientInfo + ", securityContext=" + securityContext + "}";
    }
}