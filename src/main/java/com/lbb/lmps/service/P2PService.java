package com.lbb.lmps.service;

import com.lbb.lmps.dto.P2PAccountInfoResponse;
import com.lbb.lmps.dto.P2PInquiryRequest;
import com.lbb.lmps.dto.P2PInquiryResponse;
import com.lbb.lmps.dto.P2PTransferVerifyRequest;
import com.lbb.lmps.dto.P2PTransferVerifyResponse;

public interface P2PService {

    P2PAccountInfoResponse getAccountInfoByPhone(String crPhone);

    P2PInquiryResponse inquiry(P2PInquiryRequest request);

    P2PTransferVerifyResponse transferQuotationVerify(P2PTransferVerifyRequest request);
}