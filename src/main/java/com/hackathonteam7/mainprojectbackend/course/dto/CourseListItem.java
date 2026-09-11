package com.hackathonteam7.mainprojectbackend.course.dto;

import com.hackathonteam7.mainprojectbackend.course.CourseStatus;
import com.hackathonteam7.mainprojectbackend.course.CourseType;

public record CourseListItem(
        Long id,
        String name,
        CourseType type,
        CourseStatus status,
        Boolean isOrdered,
        Integer viewCount,
        Long placeCount,
        RewardRef reward,
        Long participants
) {
}
