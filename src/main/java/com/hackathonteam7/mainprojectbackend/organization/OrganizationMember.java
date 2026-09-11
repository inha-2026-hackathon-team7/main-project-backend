package com.hackathonteam7.mainprojectbackend.organization;

import com.hackathonteam7.mainprojectbackend.common.audit.BaseTimeEntity;
import com.hackathonteam7.mainprojectbackend.user.User;
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
        name = "organization_members",
        uniqueConstraints = @UniqueConstraint(name = "uq_org_member", columnNames = {"organization_id", "user_id"})
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class OrganizationMember extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organization_id", nullable = false)
    private Organization organization;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false, length = 30)
    private OrganizationMemberRole role;

    @Builder
    private OrganizationMember(Organization organization, User user, OrganizationMemberRole role) {
        this.organization = organization;
        this.user = user;
        this.role = role;
    }

    public void changeRole(OrganizationMemberRole role) {
        this.role = role;
    }
}
