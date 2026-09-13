package com.hackathonteam7.mainprojectbackend.course.dto.user;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.hackathonteam7.mainprojectbackend.course.Course;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record EnrollmentProgressResponse(
        String status,
        List<Long> stampedCoursePlaceIds,
        long totalPlaces,
        @JsonInclude(JsonInclude.Include.NON_NULL) UserCoursePlaceItem nextPlace,
        @JsonInclude(JsonInclude.Include.NON_NULL) LocalDateTime completedAt,
        EnrollmentCourseSummary course,
        List<EnrollmentPlaceItem> places,
        @JsonInclude(JsonInclude.Include.NON_NULL) UserCourseRewardDetail reward
) {

    public record EnrollmentCourseSummary(Long id, String name, Boolean isOrdered) {
        public static EnrollmentCourseSummary from(Course course) {
            return new EnrollmentCourseSummary(course.getId(), course.getName(), course.getIsOrdered());
        }
    }

    public record EnrollmentPlaceItem(
            Long coursePlaceId,
            Long placeId,
            String name,
            BigDecimal lat,
            BigDecimal lng,
            Integer visitOrder,
            String imageUrl,
            String description,
            boolean stamped
    ) {
        public static EnrollmentPlaceItem from(UserCoursePlaceItem place, boolean stamped) {
            return new EnrollmentPlaceItem(
                    place.coursePlaceId(),
                    place.placeId(),
                    place.name(),
                    place.lat(),
                    place.lng(),
                    place.visitOrder(),
                    place.imageUrl(),
                    place.description(),
                    stamped
            );
        }
    }
}
