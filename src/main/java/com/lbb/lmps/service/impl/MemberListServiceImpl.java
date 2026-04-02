package com.lbb.lmps.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.lbb.lmps.dto.MemberListRequest;
import com.lbb.lmps.dto.MemberListResponse;
import com.lbb.lmps.dto.SmartMemberListResponse;
import com.lbb.lmps.remote.ApiMSmart;
import com.lbb.lmps.service.MemberListService;
import org.springframework.stereotype.Service;

@Service
public class MemberListServiceImpl implements MemberListService {

    private static final ObjectMapper MAPPER = new ObjectMapper()
            .findAndRegisterModules()
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    private final ApiMSmart apiMSmart;

    public MemberListServiceImpl(ApiMSmart apiMSmart) {
        this.apiMSmart = apiMSmart;
    }

    @Override
    public MemberListResponse getMemberList(MemberListRequest request) throws Exception {
        String rawResponse = apiMSmart.callMemberList(request);

        SmartMemberListResponse smartResponse = MAPPER.readValue(rawResponse, SmartMemberListResponse.class);

        MemberListResponse response = new MemberListResponse();
        response.setData(smartResponse.getData());
        response.setStatus("SUCCESS".equalsIgnoreCase(smartResponse.getResponseStatus()) ? "success" : "failed");

        return response;
    }
}
