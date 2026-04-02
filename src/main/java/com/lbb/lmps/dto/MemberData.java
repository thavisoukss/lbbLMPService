package com.lbb.lmps.dto;

public class MemberData {

    private String memberId;
    private String memberName;
    private String applicationId;
    private String iin;
    private String bankNameLa;
    private String bankNameEn;
    private String ftModules;
    private String status;
    private String imageUrl;

    public MemberData() {}

    public String getMemberId() { return memberId; }
    public void setMemberId(String memberId) { this.memberId = memberId; }

    public String getMemberName() { return memberName; }
    public void setMemberName(String memberName) { this.memberName = memberName; }

    public String getApplicationId() { return applicationId; }
    public void setApplicationId(String applicationId) { this.applicationId = applicationId; }

    public String getIin() { return iin; }
    public void setIin(String iin) { this.iin = iin; }

    public String getBankNameLa() { return bankNameLa; }
    public void setBankNameLa(String bankNameLa) { this.bankNameLa = bankNameLa; }

    public String getBankNameEn() { return bankNameEn; }
    public void setBankNameEn(String bankNameEn) { this.bankNameEn = bankNameEn; }

    public String getFtModules() { return ftModules; }
    public void setFtModules(String ftModules) { this.ftModules = ftModules; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }

    @Override
    public String toString() {
        return "MemberData{memberId='" + memberId + "', memberName='" + memberName + "', status='" + status + "'}";
    }
}