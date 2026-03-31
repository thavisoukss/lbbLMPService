package com.lbb.lmps.model.lmps;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.nnn.msmart.modelclient.lmps.FeeCcy;
import jakarta.validation.constraints.NotBlank;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Builder
@Data

public class LmpsInqOut {
//    @NotBlank(message = "from user: is required")
//    private String fromUser;
//    @NotBlank(message = "from account: is required")
//    private String fromAcctId;
//
//    @NotBlank(message = "to type: is required")
//    private String toType;
//    @NotBlank(message = "to account: is required")
//    private String toAcctId;
//    @NotBlank(message = "from member: is required")
//    private String toMember;

    private BigDecimal txnAmount;
    private String txnId;           //
    @Builder.Default
    private String frommember = "LBB";
    @NotBlank(message = "from user: is required")
    private String fromuser;
    @NotBlank(message = "from account: is required")
    private String fromaccount;
//    @NotBlank(message = "from customer id: is required")
    private String fromCif;         // Source CIF (SIBS require)

    @NotBlank(message = "to type: is required")
    private String toType;
    @NotBlank(message = "to account: is required")
    private String toaccount;
    @NotBlank(message = "to member: is required")
    private String tomember;

    private String reference;
    private String accountname;     // to account name
    private String accountccy;      // to account currency
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private FeeCcy feelist;

}
