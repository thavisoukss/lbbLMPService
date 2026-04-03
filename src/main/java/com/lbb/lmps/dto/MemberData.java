package com.lbb.lmps.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class MemberData {

    private String memberId;
    private String memberName;
    private String applicationId;
    @JsonAlias("memberIin")
    private String iin;
    private String bankNameLa;
    private String bankNameEn;
    private String ftModules;
    private String status;
    private String imageUrl;

}