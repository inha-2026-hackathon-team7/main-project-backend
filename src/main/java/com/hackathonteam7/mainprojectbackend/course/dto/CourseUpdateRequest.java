package com.hackathonteam7.mainprojectbackend.course.dto;

import com.hackathonteam7.mainprojectbackend.course.CourseStatus;

/** reward_id 는 값이 없음(연결 해제)과 키가 없음(변경 없음)을 구분해야 해서 별도로 처리한다 (CourseController 참고). */
public record CourseUpdateRequest(String name, String description, CourseStatus status, Boolean isOrdered) {
}
