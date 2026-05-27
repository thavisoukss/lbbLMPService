package com.lbb.lmps.repository;

import com.lbb.lmps.entity.LmpsTxnDetail;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface LmpsTxnDetailRepository extends JpaRepository<LmpsTxnDetail, Long> {
    Optional<LmpsTxnDetail> findByTransactionId(String transactionId);
}