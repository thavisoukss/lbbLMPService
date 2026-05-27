package com.lbb.lmps.repository;

import com.lbb.lmps.entity.WithdrawTxn;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface WithdrawTxnRepository extends JpaRepository<WithdrawTxn, Long> {
    Optional<WithdrawTxn> findByNonce(String nonce);
}
