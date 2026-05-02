package com.lbb.lmps.repository;

import com.lbb.lmps.entity.P2PTransaction;
import org.springframework.data.jpa.repository.JpaRepository;

public interface P2PTransactionRepository extends JpaRepository<P2PTransaction, String> {
}
