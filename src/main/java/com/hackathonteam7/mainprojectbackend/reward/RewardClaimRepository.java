package com.hackathonteam7.mainprojectbackend.reward;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RewardClaimRepository extends JpaRepository<RewardClaim, Long> {

    List<RewardClaim> findAllByUserId(Long userId);

    long countByRewardId(Long rewardId);
}
