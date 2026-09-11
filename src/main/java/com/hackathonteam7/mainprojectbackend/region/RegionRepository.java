package com.hackathonteam7.mainprojectbackend.region;

import com.hackathonteam7.mainprojectbackend.region.dto.RegionSummary;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RegionRepository extends JpaRepository<Region, Long> {

    Optional<Region> findByIdAndOrganizationId(Long id, Long organizationId);

    @Query("""
            select new com.hackathonteam7.mainprojectbackend.region.dto.RegionSummary(
                r.id, r.name, r.type,
                (select count(p) from Place p where p.region = r),
                (select count(distinct cp.course) from CoursePlace cp where cp.place.region = r))
            from Region r where r.organization.id = :orgId order by r.id
            """)
    List<RegionSummary> findSummaries(@Param("orgId") Long orgId);
}
