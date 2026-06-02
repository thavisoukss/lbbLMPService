package com.lbb.lmps.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "PAYMENT_CHANNEL")
public class PaymentChannel {

    @Id
    @Column(name = "ID")
    private Long id;

    @Column(name = "PROVIDER_ID")
    private Long providerId;
}
