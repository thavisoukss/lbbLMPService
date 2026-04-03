package com.lbb.lmps.service;

import com.lbb.lmps.dto.InquiryOutRequest;
import com.lbb.lmps.dto.InquiryOutResponse;

public interface InquiryOutService {
    InquiryOutResponse inquiryOut(InquiryOutRequest request, String deviceId) throws Exception;
}
