package com.lbb.lmps.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class FeeList {
    @JsonProperty("LAK")
    private List<FeeEntry> LAK;
    @JsonProperty("THB")
    private List<FeeEntry> THB;
    @JsonProperty("USD")
    private List<FeeEntry> USD;
}