package com.hackathonteam7.mainprojectbackend.reward;

import com.hackathonteam7.mainprojectbackend.common.error.ApiException;
import com.hackathonteam7.mainprojectbackend.common.error.ErrorCode;
import com.hackathonteam7.mainprojectbackend.course.CourseEnrollmentRepository;
import com.hackathonteam7.mainprojectbackend.organization.Organization;
import com.hackathonteam7.mainprojectbackend.organization.OrganizationType;
import java.time.LocalDateTime;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RewardClaimServiceTest {

    @Mock CourseEnrollmentRepository courseEnrollmentRepository;
    @Mock RewardRepository rewardRepository;
    @Mock RewardClaimRepository rewardClaimRepository;
    RewardClaimService service;

    Organization organization;
    Reward reward;

    @BeforeEach
    void setUp() {
        service = new RewardClaimService(courseEnrollmentRepository, rewardRepository, rewardClaimRepository);
        organization = Organization.builder().name("재단").type(OrganizationType.FACILITY).build();
        reward = Reward.builder().organization(organization).name("완주 포인트").kind(RewardKind.POINT).stock(100).build();
        ReflectionTestUtils.setField(reward, "id", 3L);
    }

    @Test
    void redeemMarksClaimedRewardAsUsed() {
        RewardClaim claim = claim(reward, null);
        when(rewardClaimRepository.findByIdAndUserIdForUpdate(31L, 42L)).thenReturn(Optional.of(claim));

        var result = service.redeem(42L, 31L);

        assertThat(result.status()).isEqualTo("used");
    }

    @Test
    void cannotRedeemAlreadyUsedClaim() {
        RewardClaim claim = claim(reward, null);
        claim.changeStatus("used");
        when(rewardClaimRepository.findByIdAndUserIdForUpdate(31L, 42L)).thenReturn(Optional.of(claim));

        assertThatThrownBy(() -> service.redeem(42L, 31L))
                .isInstanceOfSatisfying(ApiException.class,
                        exception -> assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.REWARD_CLAIM_ALREADY_USED));
    }

    @Test
    void cannotRedeemExpiredClaim() {
        RewardClaim claim = claim(reward, LocalDateTime.now().minusDays(1));
        when(rewardClaimRepository.findByIdAndUserIdForUpdate(31L, 42L)).thenReturn(Optional.of(claim));

        assertThatThrownBy(() -> service.redeem(42L, 31L))
                .isInstanceOfSatisfying(ApiException.class,
                        exception -> assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.REWARD_CLAIM_EXPIRED));
    }

    @Test
    void hidesExistenceOfAnotherUsersClaim() {
        when(rewardClaimRepository.findByIdAndUserIdForUpdate(31L, 99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.redeem(99L, 31L))
                .isInstanceOfSatisfying(ApiException.class,
                        exception -> assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.REWARD_CLAIM_NOT_FOUND));
    }

    private RewardClaim claim(Reward reward, LocalDateTime validUntil) {
        RewardClaim claim = RewardClaim.builder().user(null).courseEnrollment(null).reward(reward).validUntil(validUntil).build();
        ReflectionTestUtils.setField(claim, "id", 31L);
        return claim;
    }
}
