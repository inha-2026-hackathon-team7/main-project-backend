package com.hackathonteam7.mainprojectbackend.common.error;

import jakarta.validation.ConstraintViolationException;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ApiException.class)
    public ResponseEntity<ErrorResponse> handleApiException(ApiException e) {
        return ResponseEntity.status(e.getErrorCode().getStatus())
                .body(ErrorResponse.of(e.getErrorCode(), e.getDetails()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException e) {
        List<FieldErrorDetail> details = e.getBindingResult().getFieldErrors().stream()
                .map(fieldError -> new FieldErrorDetail(fieldError.getField(), fieldError.getDefaultMessage()))
                .toList();
        return ResponseEntity.status(ErrorCode.VALIDATION_FAILED.getStatus())
                .body(ErrorResponse.of(ErrorCode.VALIDATION_FAILED, details));
    }

    /** 선제 검사(§6 정상 경로)를 뚫고 나온 마지막 안전망이다. */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponse> handleDataIntegrityViolation(DataIntegrityViolationException e) {
        log.warn("Unhandled data integrity violation", e);
        return ResponseEntity.status(ErrorCode.DATA_INTEGRITY_VIOLATION.getStatus())
                .body(ErrorResponse.of(ErrorCode.DATA_INTEGRITY_VIOLATION));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleUnreadableBody(HttpMessageNotReadableException e) {
        return ResponseEntity.status(ErrorCode.VALIDATION_FAILED.getStatus())
                .body(ErrorResponse.of(ErrorCode.VALIDATION_FAILED));
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorResponse> handleConstraintViolation(ConstraintViolationException e) {
        List<FieldErrorDetail> details = e.getConstraintViolations().stream()
                .map(violation -> new FieldErrorDetail(
                        lastPathSegment(violation.getPropertyPath().toString()),
                        violation.getMessage()))
                .toList();
        return ResponseEntity.status(ErrorCode.VALIDATION_FAILED.getStatus())
                .body(ErrorResponse.of(ErrorCode.VALIDATION_FAILED, details));
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleTypeMismatch(MethodArgumentTypeMismatchException e) {
        List<FieldErrorDetail> details = List.of(
                new FieldErrorDetail(e.getName(), "올바른 형식의 값이어야 합니다."));
        return ResponseEntity.status(ErrorCode.VALIDATION_FAILED.getStatus())
                .body(ErrorResponse.of(ErrorCode.VALIDATION_FAILED, details));
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDenied(AccessDeniedException e) {
        return ResponseEntity.status(ErrorCode.FORBIDDEN.getStatus())
                .body(ErrorResponse.of(ErrorCode.FORBIDDEN));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleException(Exception e) {
        log.error("Unhandled exception", e);
        return ResponseEntity.status(ErrorCode.INTERNAL_SERVER_ERROR.getStatus())
                .body(ErrorResponse.of(ErrorCode.INTERNAL_SERVER_ERROR));
    }

    private String lastPathSegment(String path) {
        int separator = path.lastIndexOf('.');
        return separator >= 0 ? path.substring(separator + 1) : path;
    }
}
