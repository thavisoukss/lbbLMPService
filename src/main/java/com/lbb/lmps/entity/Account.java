package com.lbb.lmps.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "ACCOUNT")
public class Account {

    @Id
    @Column(name = "ID")
    private Long id;

    @Column(name = "ACCOUNT_NO")
    private String accountNo;

    @Column(name = "CUSTOMER_ID")
    private String customerId;

    @Column(name = "STATUS")
    private String status;

    @Column(name = "ACCOUNT_CURRENCY")
    private String accountCurrency;

    @Column(name = "DELETE_AT")
    private LocalDateTime deleteAt;
}
