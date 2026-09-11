package com.hackathonteam7.mainprojectbackend.region;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RegionRepository extends JpaRepository<Region, Long> {

    List<Region> findAllByOrganizationIdOrderById(Long organizationId);

    Optional<Region> findByIdAndOrganizationId(Long id, Long organizationId);
}
