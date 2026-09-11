package com.hackathonteam7.mainprojectbackend.course.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CourseCreateRequest(
        @NotBlank String name,
        String description,
        @NotNull Boolean isOrdered,
        Long rewardId
) {
}
