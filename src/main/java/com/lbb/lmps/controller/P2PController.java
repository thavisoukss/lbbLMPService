package com.lbb.lmps.controller;

import com.lbb.lmps.dto.P2PAccountInfoRequest;
import com.lbb.lmps.dto.P2PAccountInfoResponse;
import com.lbb.lmps.dto.P2PInquiryRequest;
import com.lbb.lmps.dto.P2PInquiryResponse;
import com.lbb.lmps.dto.P2PTransferVerifyRequest;
import com.lbb.lmps.dto.P2PTransferVerifyResponse;
import com.lbb.lmps.service.P2PService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/payment/p2p")
@RequiredArgsConstructor
public class P2PController {

    private final P2PService p2pService;

    @PostMapping("/get-account-info-by-phone")
    public ResponseEntity<P2PAccountInfoResponse> getAccountInfoByPhone(
            @RequestBody P2PAccountInfoRequest request) {
        log.info(">>> START getAccountInfoByPhone >>>");
        log.info("> request body: {}", request);
        long start = System.currentTimeMillis();
        try {
            P2PAccountInfoResponse finalResponse = p2pService.getAccountInfoByPhone(request.getCrPhone());
            log.info("< Final response: {} | duration_ms={}", finalResponse, System.currentTimeMillis() - start);
            return ResponseEntity.ok(finalResponse);
        } finally {
            log.info("<<< END getAccountInfoByPhone request <<<");
        }
    }

    @PostMapping("/inquiry")
    public ResponseEntity<P2PInquiryResponse> inquiry(@RequestBody P2PInquiryRequest request) {
        log.info(">>> START inquiry >>>");
        log.info("> request body: {}", request);
        long start = System.currentTimeMillis();
        try {
            P2PInquiryResponse finalResponse = p2pService.inquiry(request);
            log.info("< Final response: {} | duration_ms={}", finalResponse, System.currentTimeMillis() - start);
            return ResponseEntity.ok(finalResponse);
        } finally {
            log.info("<<< END inquiry request <<<");
        }
    }

    @PostMapping("/transfer-quotation-verify")
    public ResponseEntity<P2PTransferVerifyResponse> transferQuotationVerify(
            @RequestBody P2PTransferVerifyRequest request) {
        log.info(">>> START transferQuotationVerify >>>");
        log.info("> request body: {}", request);
        long start = System.currentTimeMillis();
        try {
            P2PTransferVerifyResponse finalResponse = p2pService.transferQuotationVerify(request);
            log.info("< Final response: {} | duration_ms={}", finalResponse, System.currentTimeMillis() - start);
            return ResponseEntity.ok(finalResponse);
        } finally {
            log.info("<<< END transferQuotationVerify request <<<");
        }
    }
}
