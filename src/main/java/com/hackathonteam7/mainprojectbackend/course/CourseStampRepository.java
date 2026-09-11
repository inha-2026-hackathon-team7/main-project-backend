package com.hackathonteam7.mainprojectbackend.course;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CourseStampRepository extends JpaRepository<CourseStamp, Long> {

    List<CourseStamp> findAllByCourseEnrollmentId(Long courseEnrollmentId);
}
