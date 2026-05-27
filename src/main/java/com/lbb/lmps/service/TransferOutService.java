package com.lbb.lmps.service;

import com.lbb.lmps.dto.TransferOutAccountRequest;
import com.lbb.lmps.dto.TransferOutQrBioRequest;
import com.lbb.lmps.dto.TransferOutQrRequest;
import com.lbb.lmps.dto.TransferOutQrResponse;

public interface TransferOutService {
    TransferOutQrResponse transferOutQr(TransferOutQrRequest request, String deviceId) throws Exception;
    TransferOutQrResponse transferOutAccount(TransferOutAccountRequest request, String deviceId) throws Exception;
    TransferOutQrResponse transferOutQrBio(TransferOutQrBioRequest request, String deviceId) throws Exception;
}