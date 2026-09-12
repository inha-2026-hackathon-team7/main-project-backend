package com.hackathonteam7.mainprojectbackend.course;

import com.hackathonteam7.mainprojectbackend.course.dto.CourseListRow;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CourseRepository extends JpaRepository<Course, Long> {

    Optional<Course> findByIdAndOrganizationId(Long id, Long organizationId);

    @EntityGraph(attributePaths = "reward")
    Optional<Course> findByIdAndStatus(Long id, CourseStatus status);

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

    @EntityGraph(attributePaths = "reward")
    @Query(
            value = """
                    select c
                    from Course c
                    where c.status = :status
                      and (:organizationId is null or c.organization.id = :organizationId)
                      and (:type is null or c.type = :type)
                      and (:regionId is null or exists (
                          select cp.id from CoursePlace cp
                          where cp.course = c and cp.place.region.id = :regionId
                      ))
                    """,
            countQuery = """
                    select count(c)
                    from Course c
                    where c.status = :status
                      and (:organizationId is null or c.organization.id = :organizationId)
                      and (:type is null or c.type = :type)
                      and (:regionId is null or exists (
                          select cp.id from CoursePlace cp
                          where cp.course = c and cp.place.region.id = :regionId
                      ))
                    """)
    Page<Course> findUserCourses(
            @Param("status") CourseStatus status,
            @Param("organizationId") Long organizationId,
            @Param("regionId") Long regionId,
            @Param("type") CourseType type,
            Pageable pageable
    );

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            update Course c
            set c.viewCount = c.viewCount + 1
            where c.id = :courseId and c.status = :status
            """)
    int increaseViewCount(@Param("courseId") Long courseId, @Param("status") CourseStatus status);
}
