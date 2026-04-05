package com.lbb.lmps.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Data;

@Data
public class InquiryOutRequest {
    // Create a single, static mapper for efficiency.
    private static final ObjectMapper MAPPER = new ObjectMapper();
    @JsonProperty("to_account")
    private String toAccount;

    @JsonProperty("to_member")
    private String toMember;
    @Override
    public String toString() {
        try {
            return MAPPER.writeValueAsString(this);
        } catch (JsonProcessingException e) {
            return "Error converting to JSON: " + e.getMessage();
        }
    }
}
