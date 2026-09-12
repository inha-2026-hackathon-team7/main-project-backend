package com.hackathonteam7.mainprojectbackend.course.dto.user;

import com.hackathonteam7.mainprojectbackend.course.CourseEnrollment;
import java.time.LocalDateTime;
import java.util.Locale;

public record EnrollmentStartResponse(
        Long enrollmentId,
        String status,
        LocalDateTime startedAt
) {

    public static EnrollmentStartResponse from(CourseEnrollment enrollment) {
        return new EnrollmentStartResponse(
                enrollment.getId(),
                enrollment.getStatus().name().toLowerCase(Locale.ROOT),
                enrollment.getStartedAt()
        );
    }
}
