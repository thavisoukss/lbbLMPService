package com.lbb.lmps.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name = "CUSTOMER_SECURITY_QUESTIONS")
public class CustomerSecurityQuestion {

    @Id
    @Column(name = "ID")
    private Long id;

    @Column(name = "CUSTOMER_ID")
    private String customerId;

    @Column(name = "SECURITY_QUESTIONS_ID")
    private String securityQuestionsId;

    @Column(name = "ANSWER")
    private String answer;

    @Column(name = "STATUS")
    private String status;

    @Column(name = "CREATED_AT")
    private LocalDateTime createdAt;

    @Column(name = "UPDATED_AT")
    private LocalDateTime updatedAt;

    @Column(name = "DELETE_AT")
    private LocalDateTime deleteAt;

    @Column(name = "TYPE")
    private String type;
}
