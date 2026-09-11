package com.hackathonteam7.mainprojectbackend.course;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CourseEnrollmentRepository extends JpaRepository<CourseEnrollment, Long> {

    List<CourseEnrollment> findAllByCourseId(Long courseId);

    Optional<CourseEnrollment> findByCourseIdAndUserId(Long courseId, Long userId);

    long countByCourseId(Long courseId);

    long countByCourseIdAndStatus(Long courseId, CourseEnrollmentStatus status);
}
