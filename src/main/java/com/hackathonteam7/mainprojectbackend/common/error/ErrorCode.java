package com.hackathonteam7.mainprojectbackend.common.error;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {

    // 400
    VALIDATION_FAILED(HttpStatus.BAD_REQUEST, "요청 값이 올바르지 않습니다."),
    INVALID_VISIT_ORDER(HttpStatus.BAD_REQUEST, "visit_order 는 1부터 N까지 연속된 값이어야 합니다."),
    DUPLICATE_PLACE(HttpStatus.BAD_REQUEST, "동일한 place 가 중복됐습니다."),

    // 401
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "인증이 필요합니다."),

    // 403
    FORBIDDEN(HttpStatus.FORBIDDEN, "권한이 없습니다."),

    // 404
    ORGANIZATION_NOT_FOUND(HttpStatus.NOT_FOUND, "조직을 찾을 수 없습니다."),
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "사용자를 찾을 수 없습니다."),
    REGION_NOT_FOUND(HttpStatus.NOT_FOUND, "지역을 찾을 수 없습니다."),
    PLACE_NOT_FOUND(HttpStatus.NOT_FOUND, "장소를 찾을 수 없습니다."),
    COURSE_NOT_FOUND(HttpStatus.NOT_FOUND, "코스를 찾을 수 없습니다."),
    ENROLLMENT_NOT_FOUND(HttpStatus.NOT_FOUND, "참가 기록을 찾을 수 없습니다."),
    REWARD_NOT_FOUND(HttpStatus.NOT_FOUND, "리워드를 찾을 수 없습니다."),

    // 409
    EMAIL_DUPLICATED(HttpStatus.CONFLICT, "이미 가입된 이메일입니다."),
    REGION_HAS_PLACES(HttpStatus.CONFLICT, "하위 place 가 있는 지역은 삭제할 수 없습니다."),
    PLACE_IN_USE(HttpStatus.CONFLICT, "코스에서 참조 중인 Place 는 삭제할 수 없습니다."),
    REWARD_IN_USE(HttpStatus.CONFLICT, "코스에 연결된 리워드는 삭제할 수 없습니다."),
    COURSE_HAS_ENROLLMENTS(HttpStatus.CONFLICT, "참가 기록이 있는 코스는 삭제할 수 없습니다."),
    COURSE_NOT_READY(HttpStatus.CONFLICT, "장소가 등록되지 않은 코스는 시작할 수 없습니다."),
    COURSE_STRUCTURE_LOCKED(HttpStatus.CONFLICT, "참가 기록이 있는 코스의 구성은 변경할 수 없습니다."),
    ALREADY_REVIEWED(HttpStatus.CONFLICT, "이미 검수 처리된 코스입니다."),
    DATA_INTEGRITY_VIOLATION(HttpStatus.CONFLICT, "다른 데이터와의 무결성 제약을 위반했습니다."),

    // 500
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "서버 오류가 발생했습니다.");

    private final HttpStatus status;
    private final String message;
}
