package com.hackathonteam7.mainprojectbackend.organization;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrganizationMemberRepository extends JpaRepository<OrganizationMember, Long> {

    List<OrganizationMember> findAllByOrganizationId(Long organizationId);

    Optional<OrganizationMember> findByOrganizationIdAndUserId(Long organizationId, Long userId);

    Optional<OrganizationMember> findFirstByUserIdOrderById(Long userId);
}
