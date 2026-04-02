package com.lbb.lmps.dto;

import java.util.List;

public class MemberListResponse {

    private List<MemberData> data;
    private String status;

    public MemberListResponse() {}

    public List<MemberData> getData() { return data; }
    public void setData(List<MemberData> data) { this.data = data; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    @Override
    public String toString() {
        return "MemberListResponse{status='" + status + "', dataSize=" + (data != null ? data.size() : 0) + "}";
    }
}
