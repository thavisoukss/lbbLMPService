package com.lbb.lmps.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "SECURITY_QUESTIONS")
public class SecurityQuestion {

    @Id
    @Column(name = "ID")
    private String id;

    @Column(name = "DESCRIPTION")
    private String description;

    public String getId() { return id; }
    public String getDescription() { return description; }
}
