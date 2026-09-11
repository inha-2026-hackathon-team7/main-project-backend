package com.hackathonteam7.mainprojectbackend.reward.dto;

import com.hackathonteam7.mainprojectbackend.reward.Reward;
import com.hackathonteam7.mainprojectbackend.reward.RewardKind;
import java.time.LocalDateTime;

public record RewardResponse(
        Long id,
        String name,
        RewardKind kind,
        String description,
        Integer stock,
        LocalDateTime validUntil,
        long linkedCourseCount
) {
    public static RewardResponse from(Reward reward, long linkedCourseCount) {
        return new RewardResponse(
                reward.getId(),
                reward.getName(),
                reward.getKind(),
                reward.getDescription(),
                reward.getStock(),
                reward.getValidUntil(),
                linkedCourseCount
        );
    }
}
