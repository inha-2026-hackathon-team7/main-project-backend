package com.hackathonteam7.mainprojectbackend.course;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CourseEnrollmentRepository extends JpaRepository<CourseEnrollment, Long> {

    List<CourseEnrollment> findAllByCourseId(Long courseId);

    Optional<CourseEnrollment> findByCourseIdAndUserId(Long courseId, Long userId);

    long countByCourseId(Long courseId);

    /** 통계용 단일 group-by 쿼리 (CLAUDE.md §5.6). 각 요소는 [CourseEnrollmentStatus, Long count]. */
    @Query("select ce.status, count(ce) from CourseEnrollment ce where ce.course.id = :courseId group by ce.status")
    List<Object[]> countGroupByStatus(@Param("courseId") Long courseId);
}
