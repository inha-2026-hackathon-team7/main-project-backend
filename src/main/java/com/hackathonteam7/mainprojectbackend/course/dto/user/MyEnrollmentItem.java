package com.hackathonteam7.mainprojectbackend.course.dto.user;

import com.hackathonteam7.mainprojectbackend.course.CourseEnrollment;
import java.time.LocalDateTime;
import java.util.Locale;

public record MyEnrollmentItem(
        Long enrollmentId,
        Long courseId,
        String courseName,
        String status,
        Progress progress,
        LocalDateTime startedAt,
        LocalDateTime completedAt
) {

    public static MyEnrollmentItem from(CourseEnrollment enrollment, long done, long total) {
        return new MyEnrollmentItem(
                enrollment.getId(),
                enrollment.getCourse().getId(),
                enrollment.getCourse().getName(),
                enrollment.getStatus().name().toLowerCase(Locale.ROOT),
                new Progress(done, total),
                enrollment.getStartedAt(),
                enrollment.getCompletedAt()
        );
    }

    public record Progress(long done, long total) {
    }
}
