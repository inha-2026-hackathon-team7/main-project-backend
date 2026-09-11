package com.hackathonteam7.mainprojectbackend.course;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CoursePlaceRepository extends JpaRepository<CoursePlace, Long> {

    List<CoursePlace> findAllByCourseIdOrderByVisitOrder(Long courseId);

    long countByPlaceId(Long placeId);

    @Query("select count(distinct cp.course.id) from CoursePlace cp where cp.place.region.id = :regionId")
    long countDistinctCoursesByRegionId(@Param("regionId") Long regionId);

    @Query("select distinct cp.course from CoursePlace cp where cp.place.id = :placeId")
    List<Course> findDistinctCoursesByPlaceId(@Param("placeId") Long placeId);

    @Modifying
    @Query("delete from CoursePlace cp where cp.course.id = :courseId")
    void deleteAllByCourseId(@Param("courseId") Long courseId);
}
