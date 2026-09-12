package com.hackathonteam7.mainprojectbackend.course.dto.user;

import com.hackathonteam7.mainprojectbackend.reward.Reward;
import java.util.Locale;

public record UserCourseRewardSummary(Long id, String name, String kind, String imageUrl) {

    public static UserCourseRewardSummary from(Reward reward) {
        if (reward == null) {
            return null;
        }
        return new UserCourseRewardSummary(
                reward.getId(),
                reward.getName(),
                reward.getKind().name().toLowerCase(Locale.ROOT),
                reward.getImageUrl()
        );
    }
}
