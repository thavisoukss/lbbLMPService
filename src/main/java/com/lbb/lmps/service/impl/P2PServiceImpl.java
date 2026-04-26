package com.lbb.lmps.service.impl;

import com.lbb.lmps.dto.P2PAccountInfoData;
import com.lbb.lmps.dto.P2PAccountInfoResponse;
import com.lbb.lmps.entity.Account;
import com.lbb.lmps.entity.Customer;
import com.lbb.lmps.exception.ResourceNotFoundException;
import com.lbb.lmps.repository.AccountRepository;
import com.lbb.lmps.repository.CustomerRepository;
import com.lbb.lmps.service.MinioStorageService;
import com.lbb.lmps.service.P2PService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class P2PServiceImpl implements P2PService {

    private final CustomerRepository customerRepository;
    private final AccountRepository accountRepository;
    private final MinioStorageService minioStorageService;

    @Override
    @Transactional(readOnly = true)
    public P2PAccountInfoResponse getAccountInfoByPhone(String crPhone) {
        log.info("[getAccountInfoByPhone] looking up customer by phone={}", crPhone);
        long start = System.currentTimeMillis();

        Customer customer = customerRepository.findByPhone(crPhone)
                .orElseThrow(() -> {
                    log.warn("[getAccountInfoByPhone] no customer found for phone={}", crPhone);
                    return new ResourceNotFoundException("AccountInfoNotFound", "Account info not found");
                });

        Account account = accountRepository.findLbiCurrentByCustomerId(customer.getId())
                .orElseThrow(() -> {
                    log.warn("[getAccountInfoByPhone] no LBI CURRENT account for customerId={}", customer.getId());
                    return new ResourceNotFoundException("AccountInfoNotFound", "Account info not found");
                });

        log.info("[getAccountInfoByPhone] account loaded accountNo={} customerId={}", account.getAccountNo(), account.getCustomerId());

        String profileImage = "";
        try {
            profileImage = minioStorageService.getFileURL("images", account.getCustomerId() + "_3.jpg");
        } catch (Exception e) {
            log.warn("[getAccountInfoByPhone] MinIO presign failed customerId={} error={}", account.getCustomerId(), e.getMessage());
        }

        P2PAccountInfoData data = new P2PAccountInfoData();
        data.setAccountNo(account.getAccountNo());
        data.setAccountName(account.getAccountName());
        data.setAccountCurrency(account.getAccountCurrency());
        data.setProfileImage(profileImage);

        P2PAccountInfoResponse response = new P2PAccountInfoResponse();
        response.setStatus("success");
        response.setData(data);

        log.info("[getAccountInfoByPhone] completed phone={} accountNo={} duration_ms={}", crPhone, account.getAccountNo(), System.currentTimeMillis() - start);
        return response;
    }
}
