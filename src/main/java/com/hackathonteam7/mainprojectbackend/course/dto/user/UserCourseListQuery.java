package com.hackathonteam7.mainprojectbackend.course.dto.user;

import com.hackathonteam7.mainprojectbackend.common.error.ApiException;
import com.hackathonteam7.mainprojectbackend.common.error.ErrorCode;
import com.hackathonteam7.mainprojectbackend.common.error.FieldErrorDetail;
import com.hackathonteam7.mainprojectbackend.course.CourseType;
import java.util.List;
import java.util.Locale;

public record UserCourseListQuery(
        Long organizationId,
        Long regionId,
        CourseType type,
        int page,
        int size
) {

    public static UserCourseListQuery of(
            Long organizationId, Long regionId, String type, int page, int size) {
        return new UserCourseListQuery(organizationId, regionId, parseType(type), page, size);
    }

    private static CourseType parseType(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return CourseType.valueOf(value.strip().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            throw new ApiException(
                    ErrorCode.VALIDATION_FAILED,
                    List.of(new FieldErrorDetail("type", "official, user, ai 중 하나여야 합니다."))
            );
        }
    }
}
