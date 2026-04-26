package com.lbb.lmps.service;

import com.lbb.lmps.dto.P2PAccountInfoResponse;

public interface P2PService {

    P2PAccountInfoResponse getAccountInfoByPhone(String crPhone);
}