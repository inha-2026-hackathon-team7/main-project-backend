package com.hackathonteam7.mainprojectbackend.course.dto.user;

import com.hackathonteam7.mainprojectbackend.course.Course;
import java.util.Locale;

public record UserCourseListItem(
        Long id,
        String name,
        String type,
        Integer viewCount,
        String thumbnailUrl,
        UserCourseRewardSummary rewardSummary
) {

    public static UserCourseListItem from(Course course, String thumbnailUrl) {
        return new UserCourseListItem(
                course.getId(),
                course.getName(),
                course.getType().name().toLowerCase(Locale.ROOT),
                course.getViewCount(),
                thumbnailUrl,
                UserCourseRewardSummary.from(course.getReward())
        );
    }
}
