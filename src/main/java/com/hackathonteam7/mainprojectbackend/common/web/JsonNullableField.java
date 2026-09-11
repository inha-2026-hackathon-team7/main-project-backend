package com.hackathonteam7.mainprojectbackend.common.web;

/**
 * JSON 요청에서 "키가 없음"(변경 없음)과 "키가 있고 값이 null"(연결 해제)을 구분해야 하는 필드용.
 * 컨트롤러에서 JsonNode 로 원본 바디를 받아 {@link #present}/{@link #value} 를 채워 서비스로 넘긴다.
 */
public record JsonNullableField<T>(boolean present, T value) {

    public static <T> JsonNullableField<T> absent() {
        return new JsonNullableField<>(false, null);
    }

    public static <T> JsonNullableField<T> of(T value) {
        return new JsonNullableField<>(true, value);
    }
}
