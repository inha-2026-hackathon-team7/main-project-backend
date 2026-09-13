package com.hackathonteam7.mainprojectbackend.course;

import jakarta.persistence.LockModeType;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CourseEnrollmentRepository extends JpaRepository<CourseEnrollment, Long> {

    List<CourseEnrollment> findAllByCourseId(Long courseId);

    Optional<CourseEnrollment> findByCourseIdAndUserId(Long courseId, Long userId);

    @EntityGraph(attributePaths = "course")
    Optional<CourseEnrollment> findByIdAndUserId(Long id, Long userId);

    @EntityGraph(attributePaths = "course")
    List<CourseEnrollment> findAllByUserIdAndStatusInOrderByStartedAtDescIdDesc(
            Long userId, Collection<CourseEnrollmentStatus> statuses);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select ce from CourseEnrollment ce join fetch ce.course where ce.id = :id and ce.user.id = :userId")
    Optional<CourseEnrollment> findByIdAndUserIdForUpdate(
            @Param("id") Long id,
            @Param("userId") Long userId
    );

    Optional<CourseEnrollment> findByCourseIdAndUserIdAndStatus(
            Long courseId, Long userId, CourseEnrollmentStatus status);

    long countByCourseId(Long courseId);

    /** 통계용 단일 group-by 쿼리 (CLAUDE.md §5.6). 각 요소는 [CourseEnrollmentStatus, Long count]. */
    @Query("select ce.status, count(ce) from CourseEnrollment ce where ce.course.id = :courseId group by ce.status")
    List<Object[]> countGroupByStatus(@Param("courseId") Long courseId);
}
