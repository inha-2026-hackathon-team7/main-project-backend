package com.hackathonteam7.mainprojectbackend.course.dto.user;

import java.math.BigDecimal;

/**
 * 코스의 "대표 장소" 한 건을 고르기 위한 내부 조회 결과이며 API 응답으로 사용하지 않는다.
 * 대표 이미지뿐 아니라 지역명·거리 계산용 좌표도 이 대표 장소 기준으로 함께 뽑아 재사용한다.
 */
public record UserCourseThumbnailRow(
        Long courseId,
        Boolean ordered,
        Long coursePlaceId,
        Integer visitOrder,
        String imageUrl,
        Long regionId,
        String regionName,
        BigDecimal latitude,
        BigDecimal longitude
) {

    public boolean precedes(UserCourseThumbnailRow other) {
        if (other == null) {
            return true;
        }
        if (Boolean.TRUE.equals(ordered)) {
            int orderComparison = visitOrder.compareTo(other.visitOrder);
            return orderComparison < 0
                    || (orderComparison == 0 && coursePlaceId.compareTo(other.coursePlaceId) < 0);
        }
        return coursePlaceId.compareTo(other.coursePlaceId) < 0;
    }
}
