package com.lbb.lmps.controller;

import com.lbb.lmps.dto.ClientInfo;
import com.lbb.lmps.dto.MemberListRequest;
import com.lbb.lmps.dto.MemberListResponse;
import com.lbb.lmps.dto.SecurityContext;
import com.lbb.lmps.service.MemberListService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping()
@RequiredArgsConstructor
public class Controller {

    private final MemberListService memberListService;

    @GetMapping("/help-check")
    public String testService() {
        log.info("test");
        return "******* LMPS Service working *******";
    }

    @GetMapping("/get-member-list")
    public ResponseEntity<MemberListResponse> getMemberList(@RequestHeader("Authorization") String authorization) throws Exception {
        log.info(">>> START getMemberList >>>");
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
}
