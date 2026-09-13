package com.hackathonteam7.mainprojectbackend.reward;

import com.hackathonteam7.mainprojectbackend.reward.dto.user.UserRewardClaimItem;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RewardClaimQueryService {

    private final RewardClaimRepository rewardClaimRepository;

    public List<UserRewardClaimItem> list(Long userId) {
        LocalDateTime now = LocalDateTime.now();
        return rewardClaimRepository.findAllByUserIdOrderByClaimedAtDescIdDesc(userId).stream()
                .map(claim -> UserRewardClaimItem.from(claim, now))
                .toList();
    }
}
