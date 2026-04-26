package com.lbb.lmps.repository;

import com.lbb.lmps.entity.Account;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface AccountRepository extends JpaRepository<Account, Long> {

    @Query("SELECT a.accountNo FROM Account a WHERE a.customerId = :customerId AND a.accountCurrency = :currencyCode AND a.status = 'ACTIVE' AND a.deleteAt IS NULL")
    Optional<String> findAccountNoByCustomerId(@Param("customerId") String customerId, @Param("currencyCode") String currencyCode);

    @Query("SELECT a FROM Account a WHERE a.customerId = :customerId AND a.accountCurrency = 'LAK' AND a.accountType = 'CURRENT' AND a.status = 'ACTIVE'")
    Optional<Account> findCurrentLakAccount(@Param("customerId") String customerId);

    @Query("SELECT a FROM Account a WHERE a.customerId = :customerId AND a.accountCurrency = 'LBI' AND a.accountType = 'CURRENT'")
    Optional<Account> findLbiCurrentByCustomerId(@Param("customerId") String customerId);
}