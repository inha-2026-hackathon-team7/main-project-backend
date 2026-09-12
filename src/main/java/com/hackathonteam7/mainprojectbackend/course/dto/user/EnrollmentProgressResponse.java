package com.hackathonteam7.mainprojectbackend.course.dto.user;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.LocalDateTime;
import java.util.List;

public record EnrollmentProgressResponse(
        String status,
        List<Long> stampedCoursePlaceIds,
        long totalPlaces,
        @JsonInclude(JsonInclude.Include.NON_NULL) UserCoursePlaceItem nextPlace,
        @JsonInclude(JsonInclude.Include.NON_NULL) LocalDateTime completedAt
) {
}
