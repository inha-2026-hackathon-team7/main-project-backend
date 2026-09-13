package com.hackathonteam7.mainprojectbackend.place.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

/** region_id 가 포함돼 와도 무시한다 (CLAUDE.md §5.3). */
public record PlaceUpdateRequest(
        @NotBlank String name,
        Long regionId,
        @NotNull @DecimalMin("-90") @DecimalMax("90") BigDecimal latitude,
        @NotNull @DecimalMin("-180") @DecimalMax("180") BigDecimal longitude,
        String category,
        String description,
        String imageUrl
) {
}
