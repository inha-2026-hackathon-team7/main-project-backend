package com.hackathonteam7.mainprojectbackend.course.dto;

import com.hackathonteam7.mainprojectbackend.course.Course;
import com.hackathonteam7.mainprojectbackend.course.CourseStatus;
import com.hackathonteam7.mainprojectbackend.course.CourseType;
import java.util.List;

public record CourseDetailResponse(
        Long id,
        String name,
        String description,
        CourseType type,
        CourseStatus status,
        Boolean isOrdered,
        Integer viewCount,
        RewardRef reward,
        List<CoursePlaceItem> places
) {
    public static CourseDetailResponse from(Course course, List<CoursePlaceItem> places) {
        RewardRef reward = course.getReward() != null
                ? new RewardRef(course.getReward().getId(), course.getReward().getName())
                : null;
        return new CourseDetailResponse(
                course.getId(),
                course.getName(),
                course.getDescription(),
                course.getType(),
                course.getStatus(),
                course.getIsOrdered(),
                course.getViewCount(),
                reward,
                places
        );
    }
}
