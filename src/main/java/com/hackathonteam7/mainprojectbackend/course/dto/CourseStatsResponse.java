package com.hackathonteam7.mainprojectbackend.course.dto;

public record CourseStatsResponse(
        Long viewCount,
        long participants,
        long completed,
        long abandoned,
        long rewardClaimed
) {
}
