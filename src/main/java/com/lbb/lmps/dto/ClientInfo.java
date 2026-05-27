package com.lbb.lmps.dto;

public class ClientInfo {

    private String deviceId;
    private String mobileNo;
    private String userId;

    public ClientInfo() {}

    public String getDeviceId() { return deviceId; }
    public void setDeviceId(String deviceId) { this.deviceId = deviceId; }

    public String getMobileNo() { return mobileNo; }
    public void setMobileNo(String mobileNo) { this.mobileNo = mobileNo; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    @Override
    public String toString() {
        return "ClientInfo{deviceId='" + deviceId + "', mobileNo='" + mobileNo + "', userId='" + userId + "'}";
    }
}