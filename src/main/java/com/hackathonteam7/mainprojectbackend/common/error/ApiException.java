package com.hackathonteam7.mainprojectbackend.common.error;

import java.util.List;
import lombok.Getter;

@Getter
public class ApiException extends RuntimeException {

    private final ErrorCode errorCode;
    private final List<?> details;

    public ApiException(ErrorCode errorCode) {
        this(errorCode, List.of());
    }

    public ApiException(ErrorCode errorCode, List<?> details) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
        this.details = details;
    }
}
