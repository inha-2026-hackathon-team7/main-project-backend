package com.hackathonteam7.mainprojectbackend.course.dto.user;

import java.math.BigDecimal;

public record UserCoursePlaceItem(
        Long coursePlaceId,
        Long placeId,
        String name,
        BigDecimal lat,
        BigDecimal lng,
        Integer visitOrder,
        String imageUrl
) {
}
