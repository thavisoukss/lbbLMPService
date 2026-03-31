package com.lbb.lmps.controller;

import com.lbb.lmps.entity.LmpsMemberDetails;
import com.lbb.lmps.model.ApiRequest;
import com.lbb.lmps.model.ApiResponse;
import com.lbb.lmps.model.lmps.LmpsInqOut;
import com.lbb.lmps.model.lmps.LmpsInqOutRequestWrapper;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.MessageSource;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Locale;

@Slf4j
@RestController
@RequestMapping("${LMPS_BASE_URL:/api/lmps}")
@RequiredArgsConstructor
public class LmpsController {
//    private final LmpsServiceImpl lmpsService;
//    private final FundTransferServiceImpl fundTransferService;
//    private final MsmartTxnDetServiceImpl msmartTxnDetService;
//    private final MsmartTxnDetailRepo msmartTxnDetailRepo;

    private final MessageSource msgSource;
//    private final ApiResponseBuilder responseBuilder; // 1. Inject the new builder


    /**
     * LMPS OUT Inquiry then register transaction to msmart_txn_detail
     *
     * @param request
     * @return
     * @throws Exception
     */
    @PostMapping("/out/inquiry/register")
    public ResponseEntity<?> inquiryOutRegister(@Valid @RequestBody LmpsInqOutRequestWrapper request, Locale locale) throws Exception {
        log.info(">>> START LMPS OUT Inquiry Register: ");
        log.info("> request body: {}", request);

        ApiResponse<LmpsInqOut> finalResponse = null;
        // 1. call LMPS inquiry service


        log.info("> final response data: {}", finalResponse.toString());
        log.info("<<< END LMPS OUT Inquiry Register: ");
        return ResponseEntity.ok(finalResponse);
    }

    /**
     * list of LMPS member details
     *
     * @param request
     * @return
     */
    @PostMapping("/member/list")
    public ResponseEntity<?> onGetLmpsMemberDetails(@Valid @RequestBody ApiRequest<Void> request) {
        log.info(">>> START LMPS Member List");
        log.info("> request body: {}", request);

        ApiResponse<List<LmpsMemberDetails>> finalResponse = null;
        // TO DO: implement service to get member list from LMPS

        log.info("> final response data: {}", finalResponse.toString());
        log.info("<<< END LMPS Member List");
        return ResponseEntity.ok(finalResponse);
    }

}