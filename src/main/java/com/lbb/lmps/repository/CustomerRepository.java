package com.lbb.lmps.repository;

import com.lbb.lmps.entity.Customer;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CustomerRepository extends JpaRepository<Customer, String> {
}
