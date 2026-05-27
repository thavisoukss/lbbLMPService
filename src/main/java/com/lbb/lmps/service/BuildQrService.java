package com.lbb.lmps.service;

import com.lbb.lmps.dto.BuildQrResponse;

public interface BuildQrService {
    BuildQrResponse buildStaticQr(String deviceId) throws Exception;
}