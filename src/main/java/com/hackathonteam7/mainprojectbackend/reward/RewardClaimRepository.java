package com.hackathonteam7.mainprojectbackend.reward;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RewardClaimRepository extends JpaRepository<RewardClaim, Long> {

    @EntityGraph(attributePaths = {"reward", "courseEnrollment.course"})
    List<RewardClaim> findAllByUserIdOrderByClaimedAtDescIdDesc(Long userId);

    @EntityGraph(attributePaths = "reward")
    Optional<RewardClaim> findByCourseEnrollmentId(Long courseEnrollmentId);

    long countByRewardId(Long rewardId);

    long countByCourseEnrollmentCourseId(Long courseId);
}
