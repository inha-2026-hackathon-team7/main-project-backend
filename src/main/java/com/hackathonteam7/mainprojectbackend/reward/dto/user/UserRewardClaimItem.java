package com.hackathonteam7.mainprojectbackend.reward.dto.user;

import com.hackathonteam7.mainprojectbackend.reward.RewardClaim;
import java.time.LocalDateTime;

public record UserRewardClaimItem(
        Long claimId,
        Long courseId,
        String courseTitle,
        String rewardName,
        String imageUrl,
        String status,
        LocalDateTime validUntil,
        LocalDateTime claimedAt
) {
    public static UserRewardClaimItem from(RewardClaim claim, LocalDateTime now) {
        String effectiveStatus = claim.getStatus();
        if (!"used".equals(effectiveStatus)
                && claim.getValidUntil() != null
                && !claim.getValidUntil().isAfter(now)) {
            effectiveStatus = "expired";
        }
        return new UserRewardClaimItem(
                claim.getId(),
                claim.getCourseEnrollment().getCourse().getId(),
                claim.getCourseEnrollment().getCourse().getName(),
                claim.getReward().getName(),
                claim.getReward().getImageUrl(),
                effectiveStatus,
                claim.getValidUntil(),
                claim.getClaimedAt()
        );
    }
}
