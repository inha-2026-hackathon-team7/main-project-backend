package com.hackathonteam7.mainprojectbackend.review;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CourseReviewRepository extends JpaRepository<CourseReview, Long> {

    List<CourseReview> findAllByCourseIdOrderByCreatedAtDesc(Long courseId);

    Optional<CourseReview> findFirstByCourseIdOrderByCreatedAtDesc(Long courseId);

    boolean existsByCourseId(Long courseId);
}
