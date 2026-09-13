package com.hackathonteam7.mainprojectbackend.course.dto.user;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.hackathonteam7.mainprojectbackend.course.Course;
import java.util.Locale;

public record UserCourseListItem(
        Long id,
        String name,
        String type,
        Integer viewCount,
        String thumbnailUrl,
        UserCourseRewardSummary rewardSummary,
        @JsonInclude(JsonInclude.Include.NON_NULL) Long regionId,
        @JsonInclude(JsonInclude.Include.NON_NULL) String regionName,
        @JsonInclude(JsonInclude.Include.NON_NULL) Double distanceMeters,
        @JsonInclude(JsonInclude.Include.NON_NULL) Integer durationMinutes
) {

    public static UserCourseListItem from(
            Course course,
            UserCourseThumbnailRow representativePlace,
            Double distanceMeters,
            Integer durationMinutes
    ) {
        return new UserCourseListItem(
                course.getId(),
                course.getName(),
                course.getType().name().toLowerCase(Locale.ROOT),
                course.getViewCount(),
                representativePlace == null ? null : representativePlace.imageUrl(),
                UserCourseRewardSummary.from(course.getReward()),
                representativePlace == null ? null : representativePlace.regionId(),
                representativePlace == null ? null : representativePlace.regionName(),
                distanceMeters,
                durationMinutes
        );
    }
}
