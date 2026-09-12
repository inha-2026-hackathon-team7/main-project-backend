package com.hackathonteam7.mainprojectbackend.course.dto.user;

/** 목록 대표 이미지를 고르기 위한 내부 조회 결과이며 API 응답으로 사용하지 않는다. */
public record UserCourseThumbnailRow(
        Long courseId,
        Boolean ordered,
        Long coursePlaceId,
        Integer visitOrder,
        String imageUrl
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
