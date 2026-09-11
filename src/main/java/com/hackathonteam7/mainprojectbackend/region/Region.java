package com.hackathonteam7.mainprojectbackend.region;

import com.hackathonteam7.mainprojectbackend.common.audit.BaseTimeEntity;
import com.hackathonteam7.mainprojectbackend.organization.Organization;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(
        name = "regions",
        uniqueConstraints = @UniqueConstraint(name = "uq_regions_org_name", columnNames = {"organization_id", "name"})
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Region extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organization_id", nullable = false)
    private Organization organization;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(length = 50)
    private String type;

    @Builder
    private Region(Organization organization, String name, String type) {
        this.organization = organization;
        this.name = name;
        this.type = type;
    }

    public void rename(String name, String type) {
        this.name = name;
        this.type = type;
    }
}
