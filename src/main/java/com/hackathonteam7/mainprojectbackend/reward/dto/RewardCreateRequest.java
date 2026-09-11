package com.hackathonteam7.mainprojectbackend.reward.dto;

import com.hackathonteam7.mainprojectbackend.reward.RewardKind;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;

public record RewardCreateRequest(
        @NotBlank String name,
        @NotNull RewardKind kind,
        String description,
        String imageUrl,
        @NotNull @Min(0) Integer stock,
        LocalDateTime validUntil
) {
}
