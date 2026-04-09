package com.lbb.lmps.service;

import com.lbb.lmps.dto.TransferOutQrRequest;
import com.lbb.lmps.dto.TransferOutQrResponse;

public interface TransferOutService {
    TransferOutQrResponse transferOutQr(TransferOutQrRequest request, String deviceId) throws Exception;
}