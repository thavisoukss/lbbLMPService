package com.lbb.lmps.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class CbsInternalTransferResponse {
    private String code;
    private int status;
    private String responseType;
    private String message;
    private long journalNo;
    private Details details;

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Details {
        private long seqNo;
        private String acctNo;
        private List<TfrDetail> tfrDetailList;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class TfrDetail {
        private long drSeqNo;
        private long crSeqNo;
    }
}
