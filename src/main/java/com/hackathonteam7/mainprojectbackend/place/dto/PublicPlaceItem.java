package com.hackathonteam7.mainprojectbackend.place.dto;

import java.math.BigDecimal;

public record PublicPlaceItem(
        Long id,
        String name,
        String category,
        String description,
        String imageUrl,
        BigDecimal latitude,
        BigDecimal longitude,
        Long regionId,
        String regionName
) {
}
