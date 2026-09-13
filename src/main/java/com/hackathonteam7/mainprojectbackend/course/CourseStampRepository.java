package com.hackathonteam7.mainprojectbackend.course;

import com.hackathonteam7.mainprojectbackend.course.dto.user.EnrollmentStampCountRow;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CourseStampRepository extends JpaRepository<CourseStamp, Long> {

    List<CourseStamp> findAllByCourseEnrollmentId(Long courseEnrollmentId);

    @Query("""
            select cs.coursePlace.id
            from CourseStamp cs
            where cs.courseEnrollment.id = :enrollmentId
            order by cs.coursePlace.visitOrder, cs.coursePlace.id
            """)
    List<Long> findStampedCoursePlaceIds(@Param("enrollmentId") Long enrollmentId);

    boolean existsByCourseEnrollmentIdAndCoursePlaceId(Long courseEnrollmentId, Long coursePlaceId);

    long countByCourseEnrollmentId(Long courseEnrollmentId);

    @Query("""
            select new com.hackathonteam7.mainprojectbackend.course.dto.user.EnrollmentStampCountRow(
                cs.courseEnrollment.id, count(cs))
            from CourseStamp cs
            where cs.courseEnrollment.id in :enrollmentIds
            group by cs.courseEnrollment.id
            """)
    List<EnrollmentStampCountRow> countByEnrollmentIds(@Param("enrollmentIds") List<Long> enrollmentIds);
}
