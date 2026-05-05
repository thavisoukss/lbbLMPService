package com.lbb.lmps.controller;

import com.lbb.lmps.dto.*;
import com.lbb.lmps.service.BuildQrService;
import com.lbb.lmps.service.InquiryOutService;
import com.lbb.lmps.service.MemberListService;
import com.lbb.lmps.service.TransferOutService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/payment/lmps")
@RequiredArgsConstructor
public class LmpsController {

    private final MemberListService memberListService;
    private final InquiryOutService inquiryOutService;
    private final TransferOutService transferOutService;
    private final BuildQrService buildQrService;

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
        try {
            MemberListResponse finalResponse = memberListService.getMemberList(deviceId);
            log.info("< Final response: {} | duration_ms={}", finalResponse, System.currentTimeMillis() - start);
            return ResponseEntity.ok(finalResponse);
        } finally {
            log.info("<<< END getMemberList request <<<");
        }
    }

    @PostMapping("/inquiry-out-qr")
    public ResponseEntity<InquiryOutResponse> inquiryOutQr(
            @RequestHeader("Device-ID") String deviceId,
            @RequestParam("qr") String qr) throws Exception {
        log.info(">>> START inquiryOutQr >>>");
        log.info("> qr param: {}", qr);
        long start = System.currentTimeMillis();
        try {
            InquiryOutResponse finalResponse = inquiryOutService.inquiryOutQr(qr, deviceId);
            log.info("< Final response: {} | duration_ms={}", finalResponse, System.currentTimeMillis() - start);
            return ResponseEntity.ok(finalResponse);
        } finally {
            log.info("<<< END inquiryOutQr request <<<");
        }
    }

    @PostMapping("/inquiry-out-account")
    public ResponseEntity<InquiryOutResponse> inquiryOutAcct(
            @RequestHeader("Device-ID") String deviceId,
            @RequestBody InquiryOutRequest request) throws Exception {
        log.info(">>> START inquiryOutAcct >>>");
        log.info("> request body: {}", request);
        long start = System.currentTimeMillis();
        try {
            InquiryOutResponse finalResponse = inquiryOutService.inquiryOut(request, deviceId);
            log.info("< Final response: {} | duration_ms={}", finalResponse, System.currentTimeMillis() - start);
            return ResponseEntity.ok(finalResponse);
        } finally {
            log.info("<<< END inquiryOutAcct request <<<");
        }
    }

    @PostMapping("/transfer-out-account-quotation-verify")
    public ResponseEntity<TransferOutQrResponse> transferOutAccountQuotationVerify(
            @RequestHeader("Device-ID") String deviceId,
            @Valid @RequestBody TransferOutAccountRequest request) throws Exception {
        log.info(">>> START transferOutAccountQuotationVerify >>>");
        log.info("> request body: {}", request);
        long start = System.currentTimeMillis();
        try {
            TransferOutQrResponse finalResponse = transferOutService.transferOutAccount(request, deviceId);
            log.info("< Final response: {} | duration_ms={}", finalResponse, System.currentTimeMillis() - start);
            return ResponseEntity.ok(finalResponse);
        } finally {
            log.info("<<< END transferOutAccountQuotationVerify request <<<");
        }
    }

    @PostMapping("/build-static-qr")
    public ResponseEntity<BuildQrResponse> buildStaticQr(
            @RequestHeader("Device-ID") String deviceId) throws Exception {
        log.info(">>> START buildStaticQr >>>");
        log.info("> Device-ID: {}", deviceId);
        long start = System.currentTimeMillis();
        try {
            BuildQrResponse finalResponse = buildQrService.buildStaticQr(deviceId);
            log.info("< Final response: {} | duration_ms={}", finalResponse, System.currentTimeMillis() - start);
            return ResponseEntity.ok(finalResponse);
        } finally {
            log.info("<<< END buildStaticQr request <<<");
        }
    }

    @PostMapping("/transfer-out-qr-bio-verify")
    public ResponseEntity<TransferOutQrResponse> transferOutQrBioVerify(
            @RequestHeader("Device-ID") String deviceId,
            @Valid @RequestBody TransferOutQrBioRequest request) throws Exception {
        log.info(">>> START transferOutQrBioVerify >>>");
        log.info("> request body: {}", request);
        long start = System.currentTimeMillis();
        try {
            TransferOutQrResponse finalResponse = transferOutService.transferOutQrBio(request, deviceId);
            log.info("< Final response: {} | duration_ms={}", finalResponse, System.currentTimeMillis() - start);
            return ResponseEntity.ok(finalResponse);
        } finally {
            log.info("<<< END transferOutQrBioVerify request <<<");
        }
    }

    @PostMapping("/transfer-out-qr-quotation-verify")
    public ResponseEntity<TransferOutQrResponse> transferOutQrQuotationVerify(
            @RequestHeader("Device-ID") String deviceId,
            @Valid @RequestBody TransferOutQrRequest request) throws Exception {
        log.info(">>> START transferOutQrQuotationVerify >>>");
        log.info("> request body: {}", request);
        long start = System.currentTimeMillis();
        try {
            TransferOutQrResponse finalResponse = transferOutService.transferOutQr(request, deviceId);
            log.info("< Final response: {} | duration_ms={}", finalResponse, System.currentTimeMillis() - start);
            return ResponseEntity.ok(finalResponse);
        } finally {
            log.info("<<< END transferOutQrQuotationVerify request <<<");
        }
    }

}
