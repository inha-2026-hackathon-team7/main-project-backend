package com.hackathonteam7.mainprojectbackend.reward;

import com.hackathonteam7.mainprojectbackend.common.error.ApiException;
import com.hackathonteam7.mainprojectbackend.common.error.ErrorCode;
import com.hackathonteam7.mainprojectbackend.course.CourseEnrollment;
import com.hackathonteam7.mainprojectbackend.course.CourseEnrollmentRepository;
import com.hackathonteam7.mainprojectbackend.course.CourseEnrollmentStatus;
import com.hackathonteam7.mainprojectbackend.reward.dto.user.RewardClaimRequest;
import com.hackathonteam7.mainprojectbackend.reward.dto.user.RewardClaimResponse;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class RewardClaimService {

    private final CourseEnrollmentRepository courseEnrollmentRepository;
    private final RewardRepository rewardRepository;
    private final RewardClaimRepository rewardClaimRepository;

    @Transactional
    public RewardClaimResult claim(Long userId, RewardClaimRequest request) {
        CourseEnrollment enrollment = courseEnrollmentRepository
                .findByIdAndUserIdForUpdate(request.courseEnrollmentId(), userId)
                .orElseThrow(() -> new ApiException(ErrorCode.ENROLLMENT_NOT_FOUND));

        RewardClaim existing = rewardClaimRepository.findByCourseEnrollmentId(enrollment.getId()).orElse(null);
        if (existing != null) {
            return new RewardClaimResult(RewardClaimResponse.from(existing), false);
        }
        if (enrollment.getStatus() != CourseEnrollmentStatus.COMPLETE) {
            throw new ApiException(ErrorCode.ENROLLMENT_NOT_COMPLETE);
        }

        Reward courseReward = enrollment.getCourse().getReward();
        if (courseReward == null) {
            throw new ApiException(ErrorCode.COURSE_HAS_NO_REWARD);
        }
        Reward reward = rewardRepository.findByIdForUpdate(courseReward.getId())
                .orElseThrow(() -> new ApiException(ErrorCode.REWARD_NOT_FOUND));
        LocalDateTime now = LocalDateTime.now();
        if (reward.getValidUntil() != null && !reward.getValidUntil().isAfter(now)) {
            throw new ApiException(ErrorCode.REWARD_EXPIRED);
        }
        if (rewardRepository.decrementStockIfAvailable(reward.getId()) != 1) {
            throw new ApiException(ErrorCode.REWARD_OUT_OF_STOCK);
        }

        RewardClaim claim = rewardClaimRepository.saveAndFlush(
                RewardClaim.builder()
                        .user(enrollment.getUser())
                        .courseEnrollment(enrollment)
                        .reward(reward)
                        .validUntil(reward.getValidUntil())
                        .build()
        );
        return new RewardClaimResult(RewardClaimResponse.from(claim), true);
    }

    @Transactional
    public RewardClaimResponse redeem(Long userId, Long claimId) {
        RewardClaim claim = rewardClaimRepository.findByIdAndUserIdForUpdate(claimId, userId)
                .orElseThrow(() -> new ApiException(ErrorCode.REWARD_CLAIM_NOT_FOUND));

        if ("used".equals(claim.getStatus())) {
            throw new ApiException(ErrorCode.REWARD_CLAIM_ALREADY_USED);
        }
        if (claim.getValidUntil() != null && !claim.getValidUntil().isAfter(LocalDateTime.now())) {
            throw new ApiException(ErrorCode.REWARD_CLAIM_EXPIRED);
        }

        claim.changeStatus("used");
        return RewardClaimResponse.from(claim);
    }

    public record RewardClaimResult(RewardClaimResponse response, boolean created) {
    }
}
