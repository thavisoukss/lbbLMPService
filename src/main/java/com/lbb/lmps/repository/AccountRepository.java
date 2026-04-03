package com.lbb.lmps.repository;

import com.lbb.lmps.entity.Account;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AccountRepository extends JpaRepository<Account, String> {

    Optional<Account> findByCustomerId(String customerId);
}
