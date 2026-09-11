package com.hackathonteam7.mainprojectbackend.organization;

import com.hackathonteam7.mainprojectbackend.common.audit.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "organizations")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Organization extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(length = 255)
    private String logo;

    @Column(nullable = false, length = 20)
    private OrganizationType type;

    @Builder
    private Organization(String name, String logo, OrganizationType type) {
        this.name = name;
        this.logo = logo;
        this.type = type;
    }

    public void rename(String name) {
        this.name = name;
    }

    public void changeLogo(String logo) {
        this.logo = logo;
    }
}
