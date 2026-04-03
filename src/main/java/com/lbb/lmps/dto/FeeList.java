package com.lbb.lmps.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class FeeList {
    private List<FeeEntry> LAK;
    private List<FeeEntry> THB;
    private List<FeeEntry> USD;
}