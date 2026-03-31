package com.lbb.lmps.controller;

import com.lbb.lmps.dto.mobile.*;
import com.lbb.lmps.model.ApiRequest;
import com.lbb.lmps.model.base.ClientInfo;
import com.lbb.lmps.model.ft.TransDetails;
import com.lbb.lmps.model.lmps.LmpsInqOut;
import com.lbb.lmps.model.lmps.LmpsInqOutRequestWrapper;
import com.lbb.lmps.model.qr.CustQrInfo;
import com.lbb.lmps.model.qr.EmvQrDetail;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

/**
 * CoverController — receives mobile app requests, adapts them to the
 * internal request format, and forwards to the copied m-smart controllers.
 *
 * Mobile app  →  CoverController (/api/lmps/**)
 *                     ↓  build ApiRequest / wrappers
 *             Copied controllers  (/lmps/**, /qr/**, /ft/**)
 */
@Log4j2
@RestController
@RequestMapping("${LMPS_BASE_URL:/api/lmps}")
@RequiredArgsConstructor
public class CoverController {

    private final RestTemplate restTemplate;

    @Value("${server.port:8084}")
    private int serverPort;

    private static final String BASE_URL_TEMPLATE = "http://localhost:%d";

    // ─────────────────────────────────────────────
    // 1. GET MEMBER LIST
    // ─────────────────────────────────────────────
    @PostMapping("/member/list")
    public ResponseEntity<?> getMemberList(
            @Valid @RequestBody MobileMemberListRequest request,
            HttpServletRequest httpReq) {

        log.info(">>> START GET MEMBER LIST >>>");
        log.info("> request body: {}", request);
        long start = System.currentTimeMillis();

        ApiRequest<Void> internalRequest = ApiRequest.<Void>builder()
                .clientInfo(buildClientInfo(request.getDeviceId(), request.getUserId(), request.getMobileNo()))
                .build();

        ResponseEntity<?> finalResponse = forward(
                "/lmps/member/list", internalRequest, httpReq);

        log.info("< Final response: {} | duration_ms={}", finalResponse.getStatusCode(), System.currentTimeMillis() - start);
        log.info("<<< END GET MEMBER LIST request <<<");
        return finalResponse;
    }

    // ─────────────────────────────────────────────
    // 2. BUILD QR (Static / Dynamic)
    // ─────────────────────────────────────────────
    @PostMapping("/qr/generate")
    public ResponseEntity<?> generateQr(
            @Valid @RequestBody MobileQrGenerateRequest request,
            HttpServletRequest httpReq) {

        log.info(">>> START BUILD QR >>>");
        log.info("> request body: {}", request);
        long start = System.currentTimeMillis();

        CustQrInfo qrInfo = CustQrInfo.builder()
                .qrFor(request.getQrFor())
                .custAccount(request.getCustAccount())
                .custAccountCcy(request.getCustAccountCcy())
                .txnAmount(request.getTxnAmount())
                .purposeOfTxn(request.getPurposeOfTxn())
                .deviceId(request.getDeviceId())
                .build();

        ApiRequest<CustQrInfo> internalRequest = ApiRequest.<CustQrInfo>builder()
                .clientInfo(buildClientInfo(request.getDeviceId(), request.getUserId(), request.getMobileNo()))
                .data(qrInfo)
                .build();

        ResponseEntity<?> finalResponse = forward("/qr/generate", internalRequest, httpReq);

        log.info("< Final response: {} | duration_ms={}", finalResponse.getStatusCode(), System.currentTimeMillis() - start);
        log.info("<<< END BUILD QR request <<<");
        return finalResponse;
    }

    // ─────────────────────────────────────────────
    // 3. GET QR INFO
    // ─────────────────────────────────────────────
    @PostMapping("/qr/info")
    public ResponseEntity<?> getQrInfo(
            @Valid @RequestBody MobileQrInfoRequest request,
            HttpServletRequest httpReq) {

        log.info(">>> START GET QR INFO >>>");
        log.info("> request body: {}", request);
        long start = System.currentTimeMillis();

        EmvQrDetail emvQrDetail = EmvQrDetail.builder()
                .qrString(request.getQrString())
                .deviceId(request.getDeviceId())
                .build();

        ApiRequest<EmvQrDetail> internalRequest = ApiRequest.<EmvQrDetail>builder()
                .clientInfo(buildClientInfo(request.getDeviceId(), request.getUserId(), request.getMobileNo()))
                .data(emvQrDetail)
                .build();

        ResponseEntity<?> finalResponse = forward("/qr/info", internalRequest, httpReq);

        log.info("< Final response: {} | duration_ms={}", finalResponse.getStatusCode(), System.currentTimeMillis() - start);
        log.info("<<< END GET QR INFO request <<<");
        return finalResponse;
    }

    // ─────────────────────────────────────────────
    // 4. OUTWARD QR / ACCOUNT INQUIRY
    // ─────────────────────────────────────────────
    @PostMapping("/out/inquiry/register")
    public ResponseEntity<?> outwardInquiry(
            @Valid @RequestBody MobileLmpsInquiryRequest request,
            HttpServletRequest httpReq) {

        log.info(">>> START OUTWARD INQUIRY >>>");
        log.info("> request body: {}", request);
        long start = System.currentTimeMillis();

        LmpsInqOut inqData = LmpsInqOut.builder()
                .txnId(request.getTxnId())
                .txnAmount(request.getTxnAmount())
                .fromuser(request.getUserId())
                .fromaccount(request.getFromAccount())
                .fromCif(request.getFromCif())
                .toType(request.getToType())
                .toaccount(request.getToAccount())
                .tomember(request.getToMember())
                .build();

        LmpsInqOutRequestWrapper internalRequest = LmpsInqOutRequestWrapper.builder()
                .clientInfo(buildClientInfo(request.getDeviceId(), request.getUserId(), request.getMobileNo()))
                .data(inqData)
                .build();

        ResponseEntity<?> finalResponse = forward("/lmps/out/inquiry/register", internalRequest, httpReq);

        log.info("< Final response: {} | duration_ms={}", finalResponse.getStatusCode(), System.currentTimeMillis() - start);
        log.info("<<< END OUTWARD INQUIRY request <<<");
        return finalResponse;
    }

    // ─────────────────────────────────────────────
    // 5. TRANSFER OUT (QR Quotation / Bio / Account)
    // ─────────────────────────────────────────────
    @PostMapping("/out/transfer")
    public ResponseEntity<?> outTransfer(
            @Valid @RequestBody MobileTransferRequest request,
            HttpServletRequest httpReq) {

        log.info(">>> START TRANSFER OUT >>>");
        log.info("> request body: {}", request);
        long start = System.currentTimeMillis();

        TransDetails transDetails = TransDetails.builder()
                .txnId(request.getTxnId())
                .txnType(request.getTxnType())
                .fromAcctId(request.getFromAcctId())
                .fromCif(request.getFromCif())
                .fromCustName(request.getFromCustName())
                .fromUserId(request.getUserId())
                .toType(request.getToType())
                .toAcctId(request.getToAcctId())
                .toCif(request.getToCif())
                .toCustName(request.getToCustName())
                .toMemberId(request.getToMemberId())
                .txnAmount(request.getTxnAmount())
                .txnCcy(request.getTxnCcy())
                .purpose(request.getPurpose())
                .build();

        ApiRequest<TransDetails> internalRequest = ApiRequest.<TransDetails>builder()
                .clientInfo(buildClientInfo(request.getDeviceId(), request.getUserId(), request.getMobileNo()))
                .data(transDetails)
                .build();

        ResponseEntity<?> finalResponse = forward("/lmps/out/transfer", internalRequest, httpReq);

        log.info("< Final response: {} | duration_ms={}", finalResponse.getStatusCode(), System.currentTimeMillis() - start);
        log.info("<<< END TRANSFER OUT request <<<");
        return finalResponse;
    }

    // ─────────────────────────────────────────────
    // 6. FT REGISTER (internal transaction record)
    // ─────────────────────────────────────────────
    @PostMapping("/ft/register")
    public ResponseEntity<?> ftRegister(
            @Valid @RequestBody MobileTransferRequest request,
            HttpServletRequest httpReq) {

        log.info(">>> START FT REGISTER >>>");
        log.info("> request body: {}", request);
        long start = System.currentTimeMillis();

        TransDetails transDetails = TransDetails.builder()
                .txnId(request.getTxnId())
                .txnType(request.getTxnType())
                .fromAcctId(request.getFromAcctId())
                .fromCif(request.getFromCif())
                .fromCustName(request.getFromCustName())
                .fromUserId(request.getUserId())
                .toType(request.getToType())
                .toAcctId(request.getToAcctId())
                .toCif(request.getToCif())
                .toCustName(request.getToCustName())
                .toMemberId(request.getToMemberId())
                .txnAmount(request.getTxnAmount())
                .txnCcy(request.getTxnCcy())
                .purpose(request.getPurpose())
                .build();

        ApiRequest<TransDetails> internalRequest = ApiRequest.<TransDetails>builder()
                .clientInfo(buildClientInfo(request.getDeviceId(), request.getUserId(), request.getMobileNo()))
                .data(transDetails)
                .build();

        ResponseEntity<?> finalResponse = forward("/ft/register", internalRequest, httpReq);

        log.info("< Final response: {} | duration_ms={}", finalResponse.getStatusCode(), System.currentTimeMillis() - start);
        log.info("<<< END FT REGISTER request <<<");
        return finalResponse;
    }

    // ─────────────────────────────────────────────
    // Helpers
    // ─────────────────────────────────────────────

    /** Build ClientInfo from the flat mobile request fields. */
    private ClientInfo buildClientInfo(String deviceId, String userId, String mobileNo) {
        return ClientInfo.builder()
                .deviceId(deviceId)
                .userId(userId)
                .mobileNo(mobileNo)
                .platform("MOBILE_APP")
                .build();
    }

    /**
     * Forward the adjusted request to an internal controller endpoint,
     * passing the original Authorization header through.
     */
    private ResponseEntity<?> forward(String path, Object body, HttpServletRequest httpReq) {
        String url = String.format(BASE_URL_TEMPLATE, serverPort) + path;

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        String authHeader = httpReq.getHeader(HttpHeaders.AUTHORIZATION);
        if (authHeader != null) {
            headers.set(HttpHeaders.AUTHORIZATION, authHeader);
        }

        HttpEntity<Object> entity = new HttpEntity<>(body, headers);
        return restTemplate.exchange(url, HttpMethod.POST, entity, Object.class);
    }
}