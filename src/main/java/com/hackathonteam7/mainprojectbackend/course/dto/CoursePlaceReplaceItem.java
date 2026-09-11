package com.hackathonteam7.mainprojectbackend.course.dto;

import jakarta.validation.constraints.NotNull;

public record CoursePlaceReplaceItem(@NotNull Long placeId, @NotNull Integer visitOrder) {
}
