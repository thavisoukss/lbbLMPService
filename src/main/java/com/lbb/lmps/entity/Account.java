package com.lbb.lmps.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.Objects;

@Getter
@Setter
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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Account a)) return false;
        return id != null && id.equals(a.id);
    }

    @Override
    public int hashCode() { return getClass().hashCode(); }
}
