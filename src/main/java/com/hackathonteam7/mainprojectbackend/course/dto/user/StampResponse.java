package com.hackathonteam7.mainprojectbackend.course.dto.user;

import com.hackathonteam7.mainprojectbackend.course.CourseStamp;
import java.time.LocalDateTime;

public record StampResponse(
        Long stampId,
        String placeName,
        LocalDateTime stampedAt,
        Progress progress,
        boolean courseCompleted
) {
    public static StampResponse from(
            CourseStamp stamp,
            String placeName,
            long done,
            long total,
            boolean courseCompleted
    ) {
        return new StampResponse(
                stamp.getId(),
                placeName,
                stamp.getStampedAt(),
                new Progress(done, total),
                courseCompleted
        );
    }

    public record Progress(long done, long total) {
    }
}
