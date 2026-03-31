package com.lbb.lmps.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "LMPS_MEMBER_DETAILS")
@JsonIgnoreProperties(ignoreUnknown = true) // Good practice for entities that might be serialized
@Data // Adds getters, setters, equals, hashCode, and toString
@NoArgsConstructor // Required by JPA
@AllArgsConstructor // Useful for creating instances
public class LmpsMemberDetails {
    @Id
    @Column(name = "MEMBER_ID")
    private String memberId;

    @Column(name = "MEMBER_NAME")
    private String memberName;

    @Column(name = "FT_MODULES")
    private String ftModules;

    @Column(name = "STATUS")
    private String status;
}