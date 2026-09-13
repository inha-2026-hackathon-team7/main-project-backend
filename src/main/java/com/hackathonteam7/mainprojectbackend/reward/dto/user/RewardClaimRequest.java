package com.hackathonteam7.mainprojectbackend.reward.dto.user;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record RewardClaimRequest(
        @NotNull @Positive Long courseEnrollmentId
) {
}
