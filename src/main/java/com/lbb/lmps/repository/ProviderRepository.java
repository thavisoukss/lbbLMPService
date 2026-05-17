package com.lbb.lmps.repository;

import com.lbb.lmps.entity.Provider;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface ProviderRepository extends JpaRepository<Provider, Long> {

    @Query(nativeQuery = true, value =
            "SELECT PC.ID as channelid, P.PROVIDER_CODE as providercode, P.WITHDRAW_TRAN_TYPE as trantype " +
            "FROM PROVIDERS P " +
            "INNER JOIN PAYMENT_CHANNEL PC ON P.ID = PC.PROVIDER_ID " +
            "WHERE P.PROVIDER_CODE = 'LMPS'")
    Optional<LmpsProviderProjection> findLmpsProviderChannel();

    interface LmpsProviderProjection {
        Long getChannelid();
        String getProvidercode();
        String getTrantype();
    }
}