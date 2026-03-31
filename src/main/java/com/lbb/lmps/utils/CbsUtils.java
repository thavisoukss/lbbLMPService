package com.lbb.lmps.utils;

import com.nnn.msmart.entity.MsmartTxnDetail;
import com.nnn.msmart.model.cbs.EMsgType;
import com.nnn.msmart.model.cbs.EProCode;
import com.nnn.msmart.modelclient.ApiCbsTransferRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class CbsUtils {
    private final CommonInfo commonInfo;
    public ApiCbsTransferRequest prepareCbsTransferRequest(EMsgType msgType, EProCode proCode, MsmartTxnDetail msmartTxnDetail) {
        String reqId = commonInfo.genRRNO();
        String reqDatetime = commonInfo.genDt();

        if (msgType == EMsgType.INPUT_REQUEST) {
            switch (proCode) {
                case EProCode.PC011100 -> {
                    // lmps out transfer
                    ApiCbsTransferRequest requestCbsTransfer = ApiCbsTransferRequest.builder()
                            .msgType(EMsgType.INPUT_REQUEST)           // Standard for financial transaction request
                            .proCode(proCode)                    // Processing code for transfer
                            .reqId(reqId)
                            .reqDatetime(reqDatetime)
                            .amount1(msmartTxnDetail.getTxnAmount().toPlainString())
                            .currency1(msmartTxnDetail.getFromAcctCcy())
                            .accepterTerId("LMPSOUT") // Assuming from account is the terminal
                            .currency1(msmartTxnDetail.getFromAcctCcy())
                            .acctNo1(msmartTxnDetail.getFromAcctId())
                            .cif1(msmartTxnDetail.getFromCustId())
                            .txnDesc("LMPS OUT | " +msmartTxnDetail.getFromAcctId() + " | " + msmartTxnDetail.getToAcctId() + " | "+msmartTxnDetail.getClientRef() + " | " + msmartTxnDetail.getPurpose())
                            .txnFee(msmartTxnDetail.getTxnFee().toPlainString())
                            .privUse1(msmartTxnDetail.getTxnId())           // Internal ref; msmart ref id, spp ref id, etc
                            .privUse2(msmartTxnDetail.getClientRef())       // From service ref e.g: msmart ref, LMPS ref; THIS CASE USE FOR LMPS REF
                            .privUse3(msmartTxnDetail.getToMemberId())      // Transaction Purpose
                            .privUse4(msmartTxnDetail.getToCustName())      // Bill number e.g: merchant bill, edl bill, etc
                            .build();
                    return  requestCbsTransfer;
                }
                case  EProCode.PC011200 -> {
                    log.info(proCode.toString());
                    return null;
                }
                default -> {
                    return null;
                }
            }
        } else {
            // revert message
            ApiCbsTransferRequest requestCbsTransfer = ApiCbsTransferRequest.builder()
                    .msgType(EMsgType.REVERSAL_REQUEST)           // Standard for financial transaction request
                    .proCode(proCode)                    // Processing code for transfer
                    .reqId(reqId)
                    .reqDatetime(reqDatetime)
                    .privUse1(msmartTxnDetail.getTxnId())           // From service ref e.g: msmart ref id
                    .privUse7(msmartTxnDetail.getCbsRefNo())
                    .build();
            return  requestCbsTransfer;

        }

    }

}
