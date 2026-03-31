package com.lbb.lmps.model.lmps;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LmpsInqOutRequestData {
    @NotBlank(message = "from user: is required")
    private String fromUser;
    @NotBlank(message = "from account: is required")
    private String fromAcctId;

    @NotBlank(message = "to type: is required")
    private String toType;
    @NotBlank(message = "to account: is required")
    private String toAcctId;
    @NotBlank(message = "to member: is required")
    private String toMember;
}
