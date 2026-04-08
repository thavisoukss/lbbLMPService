package com.lbb.lmps.service;

import com.lbb.lmps.dto.MemberListResponse;

public interface MemberListService {

    MemberListResponse getMemberList(String deviceId) throws Exception;
}
