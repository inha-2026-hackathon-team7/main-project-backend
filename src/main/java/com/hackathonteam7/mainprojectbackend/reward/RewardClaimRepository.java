package com.hackathonteam7.mainprojectbackend.reward;

import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RewardClaimRepository extends JpaRepository<RewardClaim, Long> {

    @EntityGraph(attributePaths = {"reward", "courseEnrollment.course"})
    List<RewardClaim> findAllByUserIdOrderByClaimedAtDescIdDesc(Long userId);

    @EntityGraph(attributePaths = "reward")
    Optional<RewardClaim> findByCourseEnrollmentId(Long courseEnrollmentId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select rc from RewardClaim rc join fetch rc.reward where rc.id = :id and rc.user.id = :userId")
    Optional<RewardClaim> findByIdAndUserIdForUpdate(@Param("id") Long id, @Param("userId") Long userId);

    long countByRewardId(Long rewardId);

    long countByCourseEnrollmentCourseId(Long courseId);
}
