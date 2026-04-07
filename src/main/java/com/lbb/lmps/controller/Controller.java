package com.lbb.lmps.controller;

import com.lbb.lmps.dto.*;
import com.lbb.lmps.service.InquiryOutService;
import com.lbb.lmps.service.MemberListService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping()
@RequiredArgsConstructor
public class Controller {

    private final MemberListService memberListService;
    private final InquiryOutService inquiryOutService;

    @GetMapping("/help-check")
    public String testService() {
        log.info("test");
        return "******* LMPS Service working *******";
    }

    @GetMapping("/get-member-list")
    public ResponseEntity<MemberListResponse> getMemberList(@RequestHeader("Authorization") String authorization,
                                                            HttpServletRequest httpRequest) throws Exception {
        log.info(">>> START getMemberList >>>");
        log.info("> request headers: {}", Collections.list(httpRequest.getHeaderNames()).stream().collect(Collectors.toMap(h -> h, h -> httpRequest.getHeader(h))));
        log.info("> NO request body:");
        long start = System.currentTimeMillis();

        ClientInfo clientInfo = new ClientInfo();
        clientInfo.setDeviceId("iPhone14-ABCD1234EFGH5678");
        clientInfo.setMobileNo("2055555999");
        clientInfo.setUserId("user001");

        SecurityContext securityContext = new SecurityContext();
        securityContext.setChannel("XXX");

        MemberListRequest request = new MemberListRequest();
        request.setClientInfo(clientInfo);
        request.setSecurityContext(securityContext);

        MemberListResponse finalResponse = memberListService.getMemberList(request);

        log.info("< Final response: {} | duration_ms={}", finalResponse, System.currentTimeMillis() - start);
        log.info("<<< END getMemberList request <<<");
        return ResponseEntity.ok(finalResponse);
    }

    @PostMapping("/inquiry-out-qr")
    public ResponseEntity<InquiryOutResponse> inquiryOutQr(
            @RequestHeader("Device-ID") String deviceId,
            @RequestParam("qr") String qr,
            HttpServletRequest httpRequest) throws Exception {
        log.info(">>> START inquiryOutQr >>>");
        log.info("> request headers: {}", Collections.list(httpRequest.getHeaderNames()).stream().collect(Collectors.toMap(h -> h, h -> httpRequest.getHeader(h))));
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
            @RequestBody InquiryOutRequest request,
            HttpServletRequest httpRequest) throws Exception {
        log.info(">>> START inquiryOutAcct >>>");
        log.info("> request headers: {}", Collections.list(httpRequest.getHeaderNames()).stream().collect(Collectors.toMap(h -> h, h -> httpRequest.getHeader(h))));
        log.info("> request body: {}", request);
        long start = System.currentTimeMillis();

        InquiryOutResponse finalResponse = inquiryOutService.inquiryOut(request, deviceId);

        log.info("< Final response: {} | duration_ms={}", finalResponse, System.currentTimeMillis() - start);
        log.info("<<< END inquiryOutAcct request <<<");
        return ResponseEntity.ok(finalResponse);
    }
}
