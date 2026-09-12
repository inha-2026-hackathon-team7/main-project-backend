package com.hackathonteam7.mainprojectbackend.place;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PlaceRepository extends JpaRepository<Place, Long> {

    List<Place> findAllByOrganizationIdOrderById(Long organizationId);

    List<Place> findAllByOrganizationIdAndRegionIdOrderById(Long organizationId, Long regionId);

    Optional<Place> findByIdAndOrganizationId(Long id, Long organizationId);

    Optional<Place> findByQrcodeString(String qrcodeString);

    long countByRegionId(Long regionId);

    List<Place> findAllByRegionId(Long regionId);
}
