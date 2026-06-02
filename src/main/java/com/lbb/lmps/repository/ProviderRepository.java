package com.lbb.lmps.repository;

import com.lbb.lmps.entity.PaymentChannel;
import com.lbb.lmps.entity.Provider;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface ProviderRepository extends JpaRepository<Provider, Long> {

    @Query("SELECT pc.id as channelid, p.providerCode as providercode, p.withdrawTranType as trantype " +
           "FROM Provider p " +
           "INNER JOIN PaymentChannel pc ON p.id = pc.providerId " +
           "WHERE p.providerCode = 'LMPS'")
    Optional<LmpsProviderProjection> findLmpsProviderChannel();

    interface LmpsProviderProjection {
        Long getChannelid();
        String getProvidercode();
        String getTrantype();
    }
}