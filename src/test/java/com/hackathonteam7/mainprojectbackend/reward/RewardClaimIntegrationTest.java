package com.hackathonteam7.mainprojectbackend.reward;

import com.hackathonteam7.mainprojectbackend.common.error.ApiException;
import com.hackathonteam7.mainprojectbackend.course.Course;
import com.hackathonteam7.mainprojectbackend.course.CourseEnrollment;
import com.hackathonteam7.mainprojectbackend.course.CourseEnrollmentRepository;
import com.hackathonteam7.mainprojectbackend.course.CourseEnrollmentStatus;
import com.hackathonteam7.mainprojectbackend.course.CourseRepository;
import com.hackathonteam7.mainprojectbackend.course.CourseStatus;
import com.hackathonteam7.mainprojectbackend.course.CourseType;
import com.hackathonteam7.mainprojectbackend.reward.dto.user.RewardClaimRequest;
import com.hackathonteam7.mainprojectbackend.support.IntegrationTestSupport;
import com.hackathonteam7.mainprojectbackend.user.Role;
import com.hackathonteam7.mainprojectbackend.user.User;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import tools.jackson.databind.ObjectMapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class RewardClaimIntegrationTest extends IntegrationTestSupport {

    @Autowired RewardRepository rewardRepository;
    @Autowired RewardClaimRepository rewardClaimRepository;
    @Autowired RewardClaimService rewardClaimService;
    @Autowired CourseRepository courseRepository;
    @Autowired CourseEnrollmentRepository courseEnrollmentRepository;
    @Autowired ObjectMapper objectMapper;

    @Test
    void claimsRewardAndReturnsItFromRewardBox() throws Exception {
        User participant = user("claim-success");
        Reward reward = reward(2, LocalDateTime.now().plusDays(30));
        CourseEnrollment enrollment = enrollment(participant, course(reward), true);
        String token = token(participant);

        var response = mockMvc.perform(post("/reward-claims")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body(enrollment.getId())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.claim_id").isNumber())
                .andExpect(jsonPath("$.reward_name").value("완주 리워드"))
                .andExpect(jsonPath("$.status").value("claimed"))
                .andExpect(jsonPath("$.claimed_at").exists())
                .andReturn().getResponse();
        long claimId = objectMapper.readTree(response.getContentAsString()).get("claim_id").asLong();

        RewardClaim claim = rewardClaimRepository.findByCourseEnrollmentId(enrollment.getId()).orElseThrow();
        assertThat(claim.getId()).isEqualTo(claimId);
        assertThat(claim.getUser().getId()).isEqualTo(participant.getId());
        assertThat(claim.getReward().getId()).isEqualTo(reward.getId());
        assertThat(claim.getValidUntil()).isEqualTo(reward.getValidUntil().withNano(0));
        assertThat(rewardRepository.findById(reward.getId()).orElseThrow().getStock()).isEqualTo(1);

        mockMvc.perform(get("/users/me/reward-claims")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].claim_id").value(claimId))
                .andExpect(jsonPath("$[0].course_id").value(enrollment.getCourse().getId()))
                .andExpect(jsonPath("$[0].course_title").value(enrollment.getCourse().getName()))
                .andExpect(jsonPath("$[0].reward_name").value("완주 리워드"))
                .andExpect(jsonPath("$[0].image_url").value("reward.jpg"))
                .andExpect(jsonPath("$[0].status").value("claimed"))
                .andExpect(jsonPath("$[0].valid_until").exists())
                .andExpect(jsonPath("$[0].claimed_at").exists());
    }

    @Test
    void repeatedClaimIsIdempotentAndDoesNotDecrementAgain() throws Exception {
        User participant = user("claim-repeat");
        Reward reward = reward(2, LocalDateTime.now().plusDays(30));
        CourseEnrollment enrollment = enrollment(participant, course(reward), true);
        String token = token(participant);

        var first = mockMvc.perform(post("/reward-claims")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body(enrollment.getId())))
                .andExpect(status().isCreated())
                .andReturn().getResponse();
        long firstClaimId = objectMapper.readTree(first.getContentAsString()).get("claim_id").asLong();

        mockMvc.perform(post("/reward-claims")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body(enrollment.getId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.claim_id").value(firstClaimId));

        assertThat(rewardRepository.findById(reward.getId()).orElseThrow().getStock()).isEqualTo(1);
        assertThat(rewardClaimRepository.countByRewardId(reward.getId())).isEqualTo(1);
    }

    @Test
    void rejectsForeignIncompleteNoRewardExpiredAndOutOfStockClaims() throws Exception {
        User participant = user("claim-errors");
        User other = user("claim-foreign");

        Reward available = reward(3, LocalDateTime.now().plusDays(30));
        CourseEnrollment active = enrollment(participant, course(available), false);
        claim(active, participant)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("ENROLLMENT_NOT_COMPLETE"));

        claim(active, other)
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("ENROLLMENT_NOT_FOUND"));

        CourseEnrollment noReward = enrollment(participant, course(null), true);
        claim(noReward, participant)
                .andExpect(status().is(422))
                .andExpect(jsonPath("$.code").value("COURSE_HAS_NO_REWARD"));

        Reward expired = reward(3, LocalDateTime.now().minusDays(1));
        CourseEnrollment expiredEnrollment = enrollment(participant, course(expired), true);
        claim(expiredEnrollment, participant)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("REWARD_EXPIRED"));

        Reward empty = reward(0, LocalDateTime.now().plusDays(30));
        CourseEnrollment emptyEnrollment = enrollment(participant, course(empty), true);
        claim(emptyEnrollment, participant)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("REWARD_OUT_OF_STOCK"));

        assertThat(rewardRepository.findById(available.getId()).orElseThrow().getStock()).isEqualTo(3);
        assertThat(rewardClaimRepository.findAllByUserIdOrderByClaimedAtDescIdDesc(participant.getId())).isEmpty();
    }

    @Test
    void rewardBoxDerivesExpiredStatusWithoutChangingStoredClaim() {
        User participant = user("claim-expired-box");
        Reward reward = reward(1, LocalDateTime.now().minusDays(1));
        CourseEnrollment enrollment = enrollment(participant, course(reward), true);
        RewardClaim claim = rewardClaimRepository.save(RewardClaim.builder()
                .user(participant)
                .courseEnrollment(enrollment)
                .reward(reward)
                .validUntil(reward.getValidUntil())
                .build());

        var items = new RewardClaimQueryService(rewardClaimRepository).list(participant.getId());

        assertThat(items).hasSize(1);
        assertThat(items.getFirst().status()).isEqualTo("expired");
        assertThat(rewardClaimRepository.findById(claim.getId()).orElseThrow().getStatus()).isEqualTo("claimed");
    }

    @Test
    void serializesRepeatedClaimForSameEnrollment() throws Exception {
        User participant = user("claim-concurrent-same");
        Reward reward = reward(2, LocalDateTime.now().plusDays(30));
        CourseEnrollment enrollment = enrollment(participant, course(reward), true);

        List<String> results = runConcurrently(
                () -> claimResult(enrollment, participant),
                () -> claimResult(enrollment, participant));

        assertThat(results).containsExactlyInAnyOrder("CREATED", "EXISTING");
        assertThat(rewardClaimRepository.countByRewardId(reward.getId())).isEqualTo(1);
        assertThat(rewardRepository.findById(reward.getId()).orElseThrow().getStock()).isEqualTo(1);
    }

    @Test
    void redeemMarksClaimAsUsedAndRejectsSecondRedeem() throws Exception {
        User participant = user("redeem-success");
        Reward reward = reward(2, LocalDateTime.now().plusDays(30));
        CourseEnrollment enrollment = enrollment(participant, course(reward), true);
        String token = token(participant);

        var claimResponse = mockMvc.perform(post("/reward-claims")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body(enrollment.getId())))
                .andExpect(status().isCreated())
                .andReturn().getResponse();
        long claimId = objectMapper.readTree(claimResponse.getContentAsString()).get("claim_id").asLong();

        mockMvc.perform(post("/reward-claims/{claimId}/redeem", claimId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.claim_id").value(claimId))
                .andExpect(jsonPath("$.status").value("used"));
        assertThat(rewardClaimRepository.findById(claimId).orElseThrow().getStatus()).isEqualTo("used");

        mockMvc.perform(post("/reward-claims/{claimId}/redeem", claimId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("REWARD_CLAIM_ALREADY_USED"));
    }

    @Test
    void redeemHidesForeignClaimExistence() throws Exception {
        User participant = user("redeem-owner");
        User other = user("redeem-stranger");
        Reward reward = reward(2, LocalDateTime.now().plusDays(30));
        CourseEnrollment enrollment = enrollment(participant, course(reward), true);
        String ownerToken = token(participant);
        String otherToken = token(other);

        var claimResponse = mockMvc.perform(post("/reward-claims")
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body(enrollment.getId())))
                .andExpect(status().isCreated())
                .andReturn().getResponse();
        long claimId = objectMapper.readTree(claimResponse.getContentAsString()).get("claim_id").asLong();

        mockMvc.perform(post("/reward-claims/{claimId}/redeem", claimId)
                        .header("Authorization", "Bearer " + otherToken))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("REWARD_CLAIM_NOT_FOUND"));
    }

    @Test
    void redeemRejectsExpiredClaim() throws Exception {
        User participant = user("redeem-expired");
        Reward reward = reward(1, LocalDateTime.now().minusDays(1));
        CourseEnrollment enrollment = enrollment(participant, course(reward), true);
        RewardClaim claim = rewardClaimRepository.save(RewardClaim.builder()
                .user(participant)
                .courseEnrollment(enrollment)
                .reward(reward)
                .validUntil(reward.getValidUntil())
                .build());
        String token = token(participant);

        mockMvc.perform(post("/reward-claims/{claimId}/redeem", claim.getId())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("REWARD_CLAIM_EXPIRED"));
        assertThat(rewardClaimRepository.findById(claim.getId()).orElseThrow().getStatus()).isEqualTo("claimed");
    }

    @Test
    void onlyOneEnrollmentGetsLastStock() throws Exception {
        Reward reward = reward(1, LocalDateTime.now().plusDays(30));
        Course course = course(reward);
        User firstUser = user("claim-last-first");
        User secondUser = user("claim-last-second");
        CourseEnrollment firstEnrollment = enrollment(firstUser, course, true);
        CourseEnrollment secondEnrollment = enrollment(secondUser, course, true);

        List<String> results = runConcurrently(
                () -> claimResult(firstEnrollment, firstUser),
                () -> claimResult(secondEnrollment, secondUser));

        assertThat(results).containsExactlyInAnyOrder("CREATED", "REWARD_OUT_OF_STOCK");
        assertThat(rewardClaimRepository.countByRewardId(reward.getId())).isEqualTo(1);
        assertThat(rewardRepository.findById(reward.getId()).orElseThrow().getStock()).isZero();
    }

    private org.springframework.test.web.servlet.ResultActions claim(
            CourseEnrollment enrollment,
            User participant
    ) throws Exception {
        return mockMvc.perform(post("/reward-claims")
                .header("Authorization", "Bearer " + token(participant))
                .contentType(MediaType.APPLICATION_JSON)
                .content(body(enrollment.getId())));
    }

    private String claimResult(CourseEnrollment enrollment, User participant) {
        try {
            var result = rewardClaimService.claim(
                    participant.getId(), new RewardClaimRequest(enrollment.getId()));
            return result.created() ? "CREATED" : "EXISTING";
        } catch (ApiException exception) {
            return exception.getErrorCode().name();
        }
    }

    private List<String> runConcurrently(Task firstTask, Task secondTask) throws Exception {
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            Future<String> first = executor.submit(() -> execute(ready, start, firstTask));
            Future<String> second = executor.submit(() -> execute(ready, start, secondTask));
            assertThat(ready.await(5, TimeUnit.SECONDS)).isTrue();
            start.countDown();
            return List.of(first.get(10, TimeUnit.SECONDS), second.get(10, TimeUnit.SECONDS));
        } finally {
            executor.shutdownNow();
        }
    }

    private String execute(CountDownLatch ready, CountDownLatch start, Task task) throws Exception {
        ready.countDown();
        start.await();
        return task.run();
    }

    private Reward reward(int stock, LocalDateTime validUntil) {
        return rewardRepository.save(Reward.builder()
                .organization(organization)
                .name("완주 리워드")
                .kind(RewardKind.COUPON)
                .description("완주 보상")
                .imageUrl("reward.jpg")
                .stock(stock)
                .validUntil(validUntil)
                .build());
    }

    private Course course(Reward reward) {
        return courseRepository.save(Course.builder()
                .organization(organization)
                .reward(reward)
                .name("리워드 코스 " + UUID.randomUUID())
                .type(CourseType.OFFICIAL)
                .status(CourseStatus.PUBLISHED)
                .isOrdered(true)
                .build());
    }

    private CourseEnrollment enrollment(User participant, Course course, boolean complete) {
        CourseEnrollment enrollment = courseEnrollmentRepository.save(
                CourseEnrollment.builder().course(course).user(participant).build());
        if (complete) {
            enrollment.complete();
            return courseEnrollmentRepository.save(enrollment);
        }
        assertThat(enrollment.getStatus()).isEqualTo(CourseEnrollmentStatus.ACTIVE);
        return enrollment;
    }

    private User user(String prefix) {
        return userRepository.save(User.builder()
                .name(prefix)
                .email(prefix + "-" + UUID.randomUUID() + "@example.com")
                .password(passwordEncoder.encode("password123"))
                .role(Role.USER)
                .build());
    }

    private String token(User user) {
        return jwtTokenProvider.generateAccessToken(user.getId(), user.getEmail(), user.getRole(), null);
    }

    private String body(Long enrollmentId) {
        return """
                {"course_enrollment_id":%d}
                """.formatted(enrollmentId);
    }

    @FunctionalInterface
    private interface Task {
        String run();
    }
}
