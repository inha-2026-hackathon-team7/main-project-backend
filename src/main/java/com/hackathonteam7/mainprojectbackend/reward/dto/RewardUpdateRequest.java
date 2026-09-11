package com.hackathonteam7.mainprojectbackend.reward.dto;

import jakarta.validation.constraints.Min;
import java.time.LocalDateTime;

public record RewardUpdateRequest(String name, @Min(0) Integer stock, LocalDateTime validUntil) {
}
