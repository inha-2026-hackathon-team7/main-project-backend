package com.hackathonteam7.mainprojectbackend.course.dto;

/** COURSE_HAS_ENROLLMENTS(409) 의 details — 참가/완주/수령 건수 (CLAUDE.md §6). */
public record EnrollmentCountsDetail(long participants, long completed, long rewardClaimed) {
}
