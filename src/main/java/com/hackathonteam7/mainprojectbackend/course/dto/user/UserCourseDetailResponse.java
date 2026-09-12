package com.hackathonteam7.mainprojectbackend.course.dto.user;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.hackathonteam7.mainprojectbackend.course.Course;
import java.util.List;

public record UserCourseDetailResponse(
        Long id,
        String name,
        String description,
        Boolean isOrdered,
        List<UserCoursePlaceItem> places,
        UserCourseRewardDetail reward,
        @JsonInclude(JsonInclude.Include.NON_NULL) Long myEnrollmentId
) {

    public static UserCourseDetailResponse from(
            Course course, List<UserCoursePlaceItem> places, Long myEnrollmentId) {
        return new UserCourseDetailResponse(
                course.getId(),
                course.getName(),
                course.getDescription(),
                course.getIsOrdered(),
                places,
                UserCourseRewardDetail.from(course.getReward()),
                myEnrollmentId
        );
    }
}
