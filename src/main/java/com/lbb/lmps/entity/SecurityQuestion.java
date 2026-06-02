package com.lbb.lmps.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

@Entity
@Table(name = "SECURITY_QUESTIONS")
public class SecurityQuestion {

    @Id
    @Column(name = "ID")
    private String id;

    @Column(name = "DESCRIPTION")
    private String description;

    @Column(name = "STATUS")
    private String status;

    @Column(name = "DELETE_AT")
    private LocalDateTime deleteAt;

    public String getId() { return id; }
    public String getDescription() { return description; }
    public String getStatus() { return status; }
    public LocalDateTime getDeleteAt() { return deleteAt; }
}
