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
    ENROLLMENT_NOT_COMPLETE(HttpStatus.BAD_REQUEST, "완주한 코스의 리워드만 수령할 수 있습니다."),

    // 401
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "인증이 필요합니다."),

    // 403
    FORBIDDEN(HttpStatus.FORBIDDEN, "권한이 없습니다."),

    // 404
    ORGANIZATION_NOT_FOUND(HttpStatus.NOT_FOUND, "조직을 찾을 수 없습니다."),
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "사용자를 찾을 수 없습니다."),
    REGION_NOT_FOUND(HttpStatus.NOT_FOUND, "지역을 찾을 수 없습니다."),
    PLACE_NOT_FOUND(HttpStatus.NOT_FOUND, "장소를 찾을 수 없습니다."),
    QR_PLACE_NOT_FOUND(HttpStatus.NOT_FOUND, "QR 코드에 해당하는 장소를 찾을 수 없습니다."),
    COURSE_NOT_FOUND(HttpStatus.NOT_FOUND, "코스를 찾을 수 없습니다."),
    ENROLLMENT_NOT_FOUND(HttpStatus.NOT_FOUND, "참가 기록을 찾을 수 없습니다."),
    REWARD_NOT_FOUND(HttpStatus.NOT_FOUND, "리워드를 찾을 수 없습니다."),
    REWARD_CLAIM_NOT_FOUND(HttpStatus.NOT_FOUND, "리워드 수령 기록을 찾을 수 없습니다."),

    // 422
    PLACE_NOT_IN_COURSE(HttpStatus.UNPROCESSABLE_CONTENT, "해당 코스에 속하지 않는 장소입니다."),
    STAMP_OUT_OF_RANGE(HttpStatus.UNPROCESSABLE_CONTENT, "장소의 인증 가능 반경을 벗어났습니다."),
    STAMP_ORDER_VIOLATION(HttpStatus.UNPROCESSABLE_CONTENT, "정해진 방문 순서와 일치하지 않습니다."),
    COURSE_HAS_NO_REWARD(HttpStatus.UNPROCESSABLE_CONTENT, "해당 코스에는 수령할 리워드가 없습니다."),

    // 409
    EMAIL_DUPLICATED(HttpStatus.CONFLICT, "이미 가입된 이메일입니다."),
    REGION_HAS_PLACES(HttpStatus.CONFLICT, "하위 place 가 있는 지역은 삭제할 수 없습니다."),
    PLACE_IN_USE(HttpStatus.CONFLICT, "코스에서 참조 중인 Place 는 삭제할 수 없습니다."),
    REWARD_IN_USE(HttpStatus.CONFLICT, "코스에 연결된 리워드는 삭제할 수 없습니다."),
    COURSE_HAS_ENROLLMENTS(HttpStatus.CONFLICT, "참가 기록이 있는 코스는 삭제할 수 없습니다."),
    COURSE_NOT_READY(HttpStatus.CONFLICT, "장소가 등록되지 않은 코스는 시작할 수 없습니다."),
    COURSE_STRUCTURE_LOCKED(HttpStatus.CONFLICT, "참가 기록이 있는 코스의 구성은 변경할 수 없습니다."),
    ENROLLMENT_NOT_ACTIVE(HttpStatus.CONFLICT, "진행 중인 참가에서만 스탬프를 적립할 수 있습니다."),
    STAMP_ALREADY_EXISTS(HttpStatus.CONFLICT, "이미 인증한 장소입니다."),
    REWARD_EXPIRED(HttpStatus.CONFLICT, "수령 기간이 만료된 리워드입니다."),
    REWARD_OUT_OF_STOCK(HttpStatus.CONFLICT, "리워드 재고가 소진되었습니다."),
    REWARD_CLAIM_ALREADY_USED(HttpStatus.CONFLICT, "이미 사용 처리된 리워드입니다."),
    REWARD_CLAIM_EXPIRED(HttpStatus.CONFLICT, "유효기간이 지나 사용할 수 없는 리워드입니다."),
    ENROLLMENT_ALREADY_ENDED(HttpStatus.CONFLICT, "이미 완주한 코스는 포기할 수 없습니다."),
    ALREADY_REVIEWED(HttpStatus.CONFLICT, "이미 검수 처리된 코스입니다."),
    DATA_INTEGRITY_VIOLATION(HttpStatus.CONFLICT, "다른 데이터와의 무결성 제약을 위반했습니다."),

    // 500
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "서버 오류가 발생했습니다.");

    private final HttpStatus status;
    private final String message;
}
