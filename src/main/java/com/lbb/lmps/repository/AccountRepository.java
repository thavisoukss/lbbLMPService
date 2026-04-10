package com.lbb.lmps.repository;

import com.lbb.lmps.entity.Account;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface AccountRepository extends JpaRepository<Account, Long> {

    @Query("SELECT a.accountNo FROM Account a WHERE a.customerId = :customerId AND a.accountCurrency = 'LAK' AND a.status = 'ACTIVE' AND a.deleteAt IS NULL")
    Optional<String> findAccountNoByCustomerId(@Param("customerId") String customerId);

    @Query(value = "SELECT COALESCE(NULLIF(TRIM(NAME), ''), TRIM(FIRST_NAME_EN || ' ' || LAST_NAME_EN)) FROM CUSTOMER WHERE ID = :customerId", nativeQuery = true)
    Optional<String> findCustomerNameById(@Param("customerId") String customerId);
}