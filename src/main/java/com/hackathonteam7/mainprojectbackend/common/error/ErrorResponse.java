package com.hackathonteam7.mainprojectbackend.common.error;

import java.util.List;

public record ErrorResponse(String code, String message, List<?> details) {

    public static ErrorResponse of(ErrorCode errorCode) {
        return new ErrorResponse(errorCode.name(), errorCode.getMessage(), List.of());
    }

    public static ErrorResponse of(ErrorCode errorCode, List<?> details) {
        return new ErrorResponse(errorCode.name(), errorCode.getMessage(), details);
    }
}
