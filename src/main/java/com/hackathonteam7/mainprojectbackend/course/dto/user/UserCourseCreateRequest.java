package com.hackathonteam7.mainprojectbackend.course.dto.user;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

public record UserCourseCreateRequest(
        @NotNull Long organizationId,
        @NotBlank @Size(max = 100) String name,
        String description,
        @NotNull @Size(min = 2, message = "장소는 최소 2개 이상 선택해야 합니다.") List<@NotNull Long> placeIds
) {
}
