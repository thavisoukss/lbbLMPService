package com.lbb.lmps.controller;

import com.lbb.lmps.dto.P2PAccountInfoResponse;
import com.lbb.lmps.dto.P2PInquiryRequest;
import com.lbb.lmps.dto.P2PInquiryResponse;
import com.lbb.lmps.service.P2PService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/p2p")
@RequiredArgsConstructor
public class P2PController {

    private final P2PService p2pService;

    @GetMapping("/get-account-info-by-phone")
    public ResponseEntity<P2PAccountInfoResponse> getAccountInfoByPhone(
            @RequestParam("cr_phone") String crPhone) {
        log.info(">>> START getAccountInfoByPhone >>>");
        log.info("> cr_phone: {}", crPhone);
        long start = System.currentTimeMillis();

        P2PAccountInfoResponse finalResponse = p2pService.getAccountInfoByPhone(crPhone);

        log.info("< Final response: {} | duration_ms={}", finalResponse, System.currentTimeMillis() - start);
        log.info("<<< END getAccountInfoByPhone request <<<");
        return ResponseEntity.ok(finalResponse);
    }

    @PostMapping("/inquiry")
    public ResponseEntity<P2PInquiryResponse> inquiry(@RequestBody P2PInquiryRequest request) {
        log.info(">>> START inquiry >>>");
        log.info("> request body: {}", request);
        long start = System.currentTimeMillis();

        P2PInquiryResponse finalResponse = p2pService.inquiry(request);

        log.info("< Final response: {} | duration_ms={}", finalResponse, System.currentTimeMillis() - start);
        log.info("<<< END inquiry request <<<");
        return ResponseEntity.ok(finalResponse);
    }
}
