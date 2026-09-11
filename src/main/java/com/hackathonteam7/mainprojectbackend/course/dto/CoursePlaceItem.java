package com.hackathonteam7.mainprojectbackend.course.dto;

import java.math.BigDecimal;

public record CoursePlaceItem(
        Long coursePlaceId,
        Long placeId,
        String name,
        Integer visitOrder,
        BigDecimal latitude,
        BigDecimal longitude
) {
}
