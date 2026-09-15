package com.hackathonteam7.mainprojectbackend.place;

import com.hackathonteam7.mainprojectbackend.place.dto.PublicPlaceItem;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PlaceRepository extends JpaRepository<Place, Long> {

    List<Place> findAllByOrganizationIdOrderById(Long organizationId);

    List<Place> findAllByOrganizationIdAndRegionIdOrderById(Long organizationId, Long regionId);

    Optional<Place> findByIdAndOrganizationId(Long id, Long organizationId);

    Optional<Place> findByQrcodeString(String qrcodeString);

    long countByRegionId(Long regionId);

    List<Place> findAllByRegionId(Long regionId);

    @Query("""
            select new com.hackathonteam7.mainprojectbackend.place.dto.PublicPlaceItem(
                p.id, p.name, p.category, p.description, p.imageUrl,
                p.latitude, p.longitude, r.id, r.name)
            from Place p join p.region r
            where p.organization.id = :organizationId
              and (:regionId is null or r.id = :regionId)
            order by p.id
            """)
    List<PublicPlaceItem> findPublicItemsByOrganizationId(
            @Param("organizationId") Long organizationId, @Param("regionId") Long regionId);
}
