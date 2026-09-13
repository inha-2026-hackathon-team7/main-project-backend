package com.hackathonteam7.mainprojectbackend.course;

import com.hackathonteam7.mainprojectbackend.course.dto.CoursePlaceItem;
import com.hackathonteam7.mainprojectbackend.course.dto.user.CoursePlaceCountRow;
import com.hackathonteam7.mainprojectbackend.course.dto.user.UserCoursePlaceItem;
import com.hackathonteam7.mainprojectbackend.course.dto.user.UserCourseThumbnailRow;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CoursePlaceRepository extends JpaRepository<CoursePlace, Long> {

    List<CoursePlace> findAllByCourseIdOrderByVisitOrder(Long courseId);

    Optional<CoursePlace> findByCourseIdAndPlaceId(Long courseId, Long placeId);

    long countByPlaceId(Long placeId);

    long countByCourseId(Long courseId);

    @Query("""
            select new com.hackathonteam7.mainprojectbackend.course.dto.user.CoursePlaceCountRow(
                cp.course.id, count(cp))
            from CoursePlace cp
            where cp.course.id in :courseIds
            group by cp.course.id
            """)
    List<CoursePlaceCountRow> countByCourseIds(@Param("courseIds") List<Long> courseIds);

    @Query("select count(distinct cp.course.id) from CoursePlace cp where cp.place.region.id = :regionId")
    long countDistinctCoursesByRegionId(@Param("regionId") Long regionId);

    @Query("select distinct cp.course from CoursePlace cp where cp.place.id = :placeId")
    List<Course> findDistinctCoursesByPlaceId(@Param("placeId") Long placeId);

    @Query("""
            select new com.hackathonteam7.mainprojectbackend.course.dto.CoursePlaceItem(
                cp.id, p.id, p.name, cp.visitOrder, p.latitude, p.longitude)
            from CoursePlace cp join cp.place p
            where cp.course.id = :courseId
            order by cp.visitOrder
            """)
    List<CoursePlaceItem> findPlaceItemsByCourseId(@Param("courseId") Long courseId);

    @Query("""
            select new com.hackathonteam7.mainprojectbackend.course.dto.user.UserCoursePlaceItem(
                cp.id, p.id, p.name, p.latitude, p.longitude, cp.visitOrder, p.imageUrl, p.description)
            from CoursePlace cp join cp.place p
            where cp.course.id = :courseId
            order by cp.visitOrder, cp.id
            """)
    List<UserCoursePlaceItem> findUserPlaceItemsByCourseId(@Param("courseId") Long courseId);

    @Query("""
            select new com.hackathonteam7.mainprojectbackend.course.dto.user.UserCourseThumbnailRow(
                c.id, c.isOrdered, cp.id, cp.visitOrder, p.imageUrl, r.id, r.name, p.latitude, p.longitude)
            from CoursePlace cp join cp.course c join cp.place p join p.region r
            where c.id in :courseIds
            order by c.id, cp.visitOrder, cp.id
            """)
    List<UserCourseThumbnailRow> findThumbnailRowsByCourseIds(@Param("courseIds") List<Long> courseIds);

    @Modifying
    @Query("delete from CoursePlace cp where cp.course.id = :courseId")
    void deleteAllByCourseId(@Param("courseId") Long courseId);
}
