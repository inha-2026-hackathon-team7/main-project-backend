package com.hackathonteam7.mainprojectbackend.course.dto;

import com.hackathonteam7.mainprojectbackend.course.CourseStatus;
import com.hackathonteam7.mainprojectbackend.course.CourseType;

/** CourseRepository 의 목록 프로젝션 쿼리 전용 (JPQL "select new" 대상). CourseListItem 으로 변환해 응답한다. */
public record CourseListRow(
        Long id,
        String name,
        CourseType type,
        CourseStatus status,
        Boolean isOrdered,
        Long viewCount,
        Long placeCount,
        Long rewardId,
        String rewardName,
        Long participants
) {
    public CourseListItem toItem() {
        RewardRef reward = rewardId != null ? new RewardRef(rewardId, rewardName) : null;
        return new CourseListItem(id, name, type, status, isOrdered, viewCount, placeCount, reward, participants);
    }
}
