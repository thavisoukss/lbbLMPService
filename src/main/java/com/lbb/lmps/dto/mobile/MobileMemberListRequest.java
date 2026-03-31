package com.lbb.lmps.dto.mobile;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class MobileMemberListRequest {

    @NotBlank
    @JsonProperty("deviceId")
    private String deviceId;

    @NotBlank
    @JsonProperty("userId")
    private String userId;

    @NotBlank
    @JsonProperty("mobileNo")
    private String mobileNo;
}