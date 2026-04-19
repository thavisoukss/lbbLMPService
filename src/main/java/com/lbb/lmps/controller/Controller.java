package com.lbb.lmps.controller;

import com.lbb.lmps.dto.*;
import com.lbb.lmps.service.InquiryOutService;
import com.lbb.lmps.service.MemberListService;
import com.lbb.lmps.service.TransferOutService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping()
@RequiredArgsConstructor
public class Controller {

    private final MemberListService memberListService;
    private final InquiryOutService inquiryOutService;
    private final TransferOutService transferOutService;

    @GetMapping("/help-check")
    public String testService() {
        log.info("test");
        return "******* LMPS Service working *******";
    }

    @GetMapping("/get-member-list")
    public ResponseEntity<MemberListResponse> getMemberList(@RequestHeader("Device-ID") String deviceId) throws Exception {
        log.info(">>> START getMemberList >>>");
        log.info("> NO request body:");
        long start = System.currentTimeMillis();

        MemberListResponse finalResponse = memberListService.getMemberList(deviceId);

        log.info("< Final response: {} | duration_ms={}", finalResponse, System.currentTimeMillis() - start);
        log.info("<<< END getMemberList request <<<");
        return ResponseEntity.ok(finalResponse);
    }

    @PostMapping("/inquiry-out-qr")
    public ResponseEntity<InquiryOutResponse> inquiryOutQr(
            @RequestHeader("Device-ID") String deviceId,
            @RequestParam("qr") String qr) throws Exception {
        log.info(">>> START inquiryOutQr >>>");
        log.info("> qr param: {}", qr);
        long start = System.currentTimeMillis();

        InquiryOutResponse finalResponse = inquiryOutService.inquiryOutQr(qr, deviceId);

        log.info("< Final response: {} | duration_ms={}", finalResponse, System.currentTimeMillis() - start);
        log.info("<<< END inquiryOutQr request <<<");
        return ResponseEntity.ok(finalResponse);
    }

    @PostMapping("/inquiry-out-account")
    public ResponseEntity<InquiryOutResponse> inquiryOutAcct(
            @RequestHeader("Device-ID") String deviceId,
            @RequestBody InquiryOutRequest request) throws Exception {
        log.info(">>> START inquiryOutAcct >>>");
        log.info("> request body: {}", request);
        long start = System.currentTimeMillis();

        InquiryOutResponse finalResponse = inquiryOutService.inquiryOut(request, deviceId);

        log.info("< Final response: {} | duration_ms={}", finalResponse, System.currentTimeMillis() - start);
        log.info("<<< END inquiryOutAcct request <<<");
        return ResponseEntity.ok(finalResponse);
    }

    @PostMapping("/transfer-out-account-quotation-verify")
    public ResponseEntity<TransferOutQrResponse> transferOutAccountQuotationVerify(
            @RequestHeader("Device-ID") String deviceId,
            @RequestBody TransferOutAccountRequest request) throws Exception {
        log.info(">>> START transferOutAccountQuotationVerify >>>");
        log.info("> request body: {}", request);
        long start = System.currentTimeMillis();

        TransferOutQrResponse finalResponse = transferOutService.transferOutAccount(request, deviceId);

        log.info("< Final response: {} | duration_ms={}", finalResponse, System.currentTimeMillis() - start);
        log.info("<<< END transferOutAccountQuotationVerify request <<<");
        return ResponseEntity.ok(finalResponse);
    }

    @PostMapping("/transfer-out-qr-quotation-verify")
    public ResponseEntity<TransferOutQrResponse> transferOutQrQuotationVerify(
            @RequestHeader("Device-ID") String deviceId,
            @RequestBody TransferOutQrRequest request) throws Exception {
        log.info(">>> START transferOutQrQuotationVerify >>>");
        log.info("> request body: {}", request);
        long start = System.currentTimeMillis();

        TransferOutQrResponse finalResponse = transferOutService.transferOutQr(request, deviceId);

        log.info("< Final response: {} | duration_ms={}", finalResponse, System.currentTimeMillis() - start);
        log.info("<<< END transferOutQrQuotationVerify request <<<");
        return ResponseEntity.ok(finalResponse);
    }
}
