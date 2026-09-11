package com.hackathonteam7.mainprojectbackend.course.dto;

import java.time.LocalDateTime;

/** ai_confidence 는 현재 스키마에 저장 위치가 없어 항상 null 로 내려간다. */
public record CoursePendingItem(
        Long id,
        String name,
        String creator,
        LocalDateTime createdAt,
        Long placeCount,
        Double aiConfidence
) {
}
