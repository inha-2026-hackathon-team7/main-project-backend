package com.hackathonteam7.mainprojectbackend.reward.dto.user;

import com.hackathonteam7.mainprojectbackend.reward.RewardClaim;
import java.time.LocalDateTime;

public record RewardClaimResponse(
        Long claimId,
        String rewardName,
        String status,
        LocalDateTime claimedAt
) {
    public static RewardClaimResponse from(RewardClaim claim) {
        return new RewardClaimResponse(
                claim.getId(),
                claim.getReward().getName(),
                claim.getStatus(),
                claim.getClaimedAt()
        );
    }
}
