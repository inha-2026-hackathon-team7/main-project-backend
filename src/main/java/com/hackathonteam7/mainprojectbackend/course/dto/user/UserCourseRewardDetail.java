package com.hackathonteam7.mainprojectbackend.course.dto.user;

import com.hackathonteam7.mainprojectbackend.reward.Reward;
import java.time.LocalDateTime;
import java.util.Locale;

public record UserCourseRewardDetail(
        Long id,
        String name,
        String kind,
        String description,
        String imageUrl,
        Integer stock,
        LocalDateTime validUntil
) {

    public static UserCourseRewardDetail from(Reward reward) {
        if (reward == null) {
            return null;
        }
        return new UserCourseRewardDetail(
                reward.getId(),
                reward.getName(),
                reward.getKind().name().toLowerCase(Locale.ROOT),
                reward.getDescription(),
                reward.getImageUrl(),
                reward.getStock(),
                reward.getValidUntil()
        );
    }
}
