package com.hackathonteam7.mainprojectbackend.common.error;

/** REGION_HAS_PLACES / PLACE_IN_USE / REWARD_IN_USE 차단 모달의 "참조 목록" 항목 (CLAUDE.md §6). */
public record ErrorDetail(Long id, String name, String note) {
}
