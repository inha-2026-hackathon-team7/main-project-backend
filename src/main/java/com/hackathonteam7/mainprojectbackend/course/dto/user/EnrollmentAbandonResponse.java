package com.hackathonteam7.mainprojectbackend.course.dto.user;

import com.hackathonteam7.mainprojectbackend.course.CourseEnrollment;
import java.util.Locale;

public record EnrollmentAbandonResponse(Long enrollmentId, String status) {

    public static EnrollmentAbandonResponse from(CourseEnrollment enrollment) {
        return new EnrollmentAbandonResponse(
                enrollment.getId(),
                enrollment.getStatus().name().toLowerCase(Locale.ROOT)
        );
    }
}
