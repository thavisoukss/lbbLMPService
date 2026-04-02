package com.lbb.lmps.controller;

import com.lbb.lmps.dto.MemberListRequest;
import com.lbb.lmps.dto.MemberListResponse;
import com.lbb.lmps.service.MemberListService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("${LMPS_BASE_URL:/api/lmps}")
public class Controller {

    private static final Logger log = LogManager.getLogger(Controller.class);

    private final MemberListService memberListService;

    public Controller(MemberListService memberListService) {
        this.memberListService = memberListService;
    }

    @GetMapping("/help-check")
    public String testService() {
        log.info("test");
        return "******* LMPS Service working *******";
    }

    @PostMapping("/member-list")
    public ResponseEntity<MemberListResponse> getMemberList(@RequestBody MemberListRequest request) throws Exception {
        log.info(">>> START getMemberList >>>");
        log.info("> request body: {}", request);
        long start = System.currentTimeMillis();

        MemberListResponse finalResponse = memberListService.getMemberList(request);

        log.info("< Final response: {} | duration_ms={}", finalResponse, System.currentTimeMillis() - start);
        log.info("<<< END getMemberList request <<<");
        return ResponseEntity.ok(finalResponse);
    }
}
