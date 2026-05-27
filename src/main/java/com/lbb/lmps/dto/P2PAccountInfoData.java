package com.lbb.lmps.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class P2PAccountInfoData {

    @JsonProperty("account_no")
    private String accountNo;

    @JsonProperty("account_name")
    private String accountName;

    @JsonProperty("account_currency")
    private String accountCurrency;

    @JsonProperty("profile_image")
    private String profileImage;
}
