package com.lbb.lmps.repository;

import com.lbb.lmps.entity.P2PTxnDetail;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface P2PTxnDetailRepository extends JpaRepository<P2PTxnDetail, String> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT d FROM P2PTxnDetail d WHERE d.txnId = :txnId")
    Optional<P2PTxnDetail> findByIdForUpdate(@Param("txnId") String txnId);
}
