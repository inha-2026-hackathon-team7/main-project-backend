package com.hackathonteam7.mainprojectbackend.course;

import org.springframework.data.jpa.repository.JpaRepository;

public interface CourseViewRepository extends JpaRepository<CourseView, Long> {

    boolean existsByCourseIdAndUserId(Long courseId, Long userId);
}
