package com.hackathonteam7.mainprojectbackend.place.dto;

import com.hackathonteam7.mainprojectbackend.place.Place;
import java.math.BigDecimal;

public record PlaceResponse(
        Long id,
        String name,
        Long regionId,
        BigDecimal latitude,
        BigDecimal longitude,
        String category,
        String description,
        String imageUrl,
        String qrcodeString
) {
    public static PlaceResponse from(Place place) {
        return new PlaceResponse(
                place.getId(),
                place.getName(),
                place.getRegion().getId(),
                place.getLatitude(),
                place.getLongitude(),
                place.getCategory(),
                place.getDescription(),
                place.getImageUrl(),
                place.getQrcodeString()
        );
    }
}
