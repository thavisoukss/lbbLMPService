package com.lbb.lmps.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;

@Getter
@Entity
@Table(name = "CUSTOMER")
public class Customer {

    @Id
    @Column(name = "ID")
    private String id;

    @Column(name = "NAME")
    private String name;

    @Column(name = "FIRST_NAME_EN")
    private String firstNameEn;

    @Column(name = "LAST_NAME_EN")
    private String lastNameEn;
}
