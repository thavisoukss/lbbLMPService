package com.lbb.lmps.service;

import com.lbb.lmps.dto.MemberListRequest;
import com.lbb.lmps.dto.MemberListResponse;

public interface MemberListService {

    MemberListResponse getMemberList(MemberListRequest request) throws Exception;
}
