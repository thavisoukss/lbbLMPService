package com.lbb.lmps.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name = "PROVIDERS")
public class Provider {

    @Id
    @Column(name = "ID")
    private Long id;

    @Column(name = "PROVIDER_CODE", nullable = false, length = 50)
    private String providerCode;

    @Column(name = "NAME_EN", nullable = false, length = 100)
    private String nameEn;

    @Column(name = "NAME_LA", length = 100)
    private String nameLa;

    @Column(name = "API_ENDPOINT", length = 4000)
    private String apiEndpoint;

    @Column(name = "API_KEY", length = 200)
    private String apiKey;

    @Column(name = "API_SECRET", length = 200)
    private String apiSecret;

    @Column(name = "GL_CODE", length = 20)
    private String glCode;

    @Column(name = "CIF")
    private Long cif;

    @Column(name = "STATUS", nullable = false, length = 20)
    private String status;

    @Column(name = "CREATED_AT", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "UPDATED_AT")
    private LocalDateTime updatedAt;

    @Column(name = "DELETED_AT")
    private LocalDateTime deletedAt;

    @Column(name = "TRAN_TYPE", length = 10)
    private String tranType;

    @Column(name = "DRSETTLE_MTD", length = 10)
    private String drSettleMtd;

    @Column(name = "CRSETTLE_MTD", length = 10)
    private String crSettleMtd;

    @Column(name = "ADDRESS", length = 255)
    private String address;

    @Column(name = "TOPUP_TRAN_TYPE", length = 10)
    private String topupTranType;

    @Column(name = "TOPUP_DRSETTLE_MTD", length = 10)
    private String topupDrSettleMtd;

    @Column(name = "TOPUP_CRSETTLE_MTD", length = 10)
    private String topupCrSettleMtd;

    @Column(name = "WITHDRAW_TRAN_TYPE", length = 20)
    private String withdrawTranType;

    @Column(name = "WITHDRAW_DRSETTLE_MTD", length = 20)
    private String withdrawDrSettleMtd;

    @Column(name = "WITHDRAW_CRSETTLE_MTD", length = 20)
    private String withdrawCrSettleMtd;
}