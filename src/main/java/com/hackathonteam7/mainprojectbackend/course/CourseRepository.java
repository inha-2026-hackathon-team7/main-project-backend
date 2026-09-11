package com.hackathonteam7.mainprojectbackend.course;

import com.hackathonteam7.mainprojectbackend.course.dto.CourseListRow;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CourseRepository extends JpaRepository<Course, Long> {

    Optional<Course> findByIdAndOrganizationId(Long id, Long organizationId);

    List<Course> findAllByRewardId(Long rewardId);

    long countByRewardId(Long rewardId);

    @EntityGraph(attributePaths = "creator")
    List<Course> findAllByOrganizationIdAndStatusAndTypeInOrderByCreatedAtDesc(
            Long organizationId, CourseStatus status, List<CourseType> types);

    @Query("""
            select new com.hackathonteam7.mainprojectbackend.course.dto.CourseListRow(
                c.id, c.name, c.type, c.status, c.isOrdered, c.viewCount,
                (select count(cp) from CoursePlace cp where cp.course = c),
                r.id, r.name,
                (select count(ce) from CourseEnrollment ce where ce.course = c))
            from Course c left join c.reward r
            where c.organization.id = :orgId
              and (:type is null or c.type = :type)
              and (:status is null or c.status = :status)
            order by c.id
            """)
    List<CourseListRow> findListRows(@Param("orgId") Long orgId, @Param("type") CourseType type, @Param("status") CourseStatus status);
}
