package com.hackathonteam7.mainprojectbackend.reward;

import com.hackathonteam7.mainprojectbackend.common.error.GlobalExceptionHandler;
import com.hackathonteam7.mainprojectbackend.config.SecurityConfig;
import com.hackathonteam7.mainprojectbackend.reward.dto.user.RewardClaimRequest;
import com.hackathonteam7.mainprojectbackend.reward.dto.user.RewardClaimResponse;
import com.hackathonteam7.mainprojectbackend.reward.dto.user.UserRewardClaimItem;
import com.hackathonteam7.mainprojectbackend.security.JwtAuthenticationFilter;
import com.hackathonteam7.mainprojectbackend.security.JwtTokenProvider;
import com.hackathonteam7.mainprojectbackend.user.Role;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(RewardClaimController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class, JwtTokenProvider.class, GlobalExceptionHandler.class})
class RewardClaimWebTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @Autowired JwtTokenProvider jwtTokenProvider;
    @MockitoBean RewardClaimService rewardClaimService;
    @MockitoBean RewardClaimQueryService rewardClaimQueryService;

    @Test
    void newClaimReturnsCreatedAndOnlyDocumentedFields() throws Exception {
        LocalDateTime claimedAt = LocalDateTime.of(2026, 9, 13, 10, 30);
        RewardClaimRequest request = new RewardClaimRequest(81L);
        when(rewardClaimService.claim(42L, request)).thenReturn(new RewardClaimService.RewardClaimResult(
                new RewardClaimResponse(31L, "커피 교환권", "claimed", claimedAt), true));

        var response = mockMvc.perform(post("/reward-claims")
                        .header("Authorization", "Bearer " + userToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"course_enrollment_id":81}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.claim_id").value(31))
                .andExpect(jsonPath("$.reward_name").value("커피 교환권"))
                .andExpect(jsonPath("$.status").value("claimed"))
                .andExpect(jsonPath("$.claimed_at").value("2026-09-13T10:30:00"))
                .andReturn().getResponse();

        assertThat(objectMapper.readTree(response.getContentAsString()).size()).isEqualTo(4);
        verify(rewardClaimService).claim(42L, request);
    }

    @Test
    void repeatedClaimReturnsOk() throws Exception {
        RewardClaimRequest request = new RewardClaimRequest(81L);
        when(rewardClaimService.claim(42L, request)).thenReturn(new RewardClaimService.RewardClaimResult(
                new RewardClaimResponse(31L, "커피 교환권", "claimed", LocalDateTime.now()), false));

        mockMvc.perform(post("/reward-claims")
                        .header("Authorization", "Bearer " + userToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"course_enrollment_id":81}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.claim_id").value(31));
    }

    @Test
    void rewardBoxReturnsDocumentedArrayItems() throws Exception {
        LocalDateTime claimedAt = LocalDateTime.of(2026, 9, 13, 10, 30);
        LocalDateTime validUntil = LocalDateTime.of(2026, 10, 31, 23, 59, 59);
        when(rewardClaimQueryService.list(42L)).thenReturn(List.of(
                new UserRewardClaimItem(
                        31L, 9L, "성수 로컬 크래프트 투어", "커피 교환권", "reward.jpg", "claimed", validUntil, claimedAt)));

        var response = mockMvc.perform(get("/users/me/reward-claims")
                        .header("Authorization", "Bearer " + userToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].claim_id").value(31))
                .andExpect(jsonPath("$[0].course_id").value(9))
                .andExpect(jsonPath("$[0].course_title").value("성수 로컬 크래프트 투어"))
                .andExpect(jsonPath("$[0].reward_name").value("커피 교환권"))
                .andExpect(jsonPath("$[0].image_url").value("reward.jpg"))
                .andExpect(jsonPath("$[0].status").value("claimed"))
                .andExpect(jsonPath("$[0].valid_until").value("2026-10-31T23:59:59"))
                .andExpect(jsonPath("$[0].claimed_at").value("2026-09-13T10:30:00"))
                .andReturn().getResponse();

        assertThat(objectMapper.readTree(response.getContentAsString()).get(0).size()).isEqualTo(8);
        verify(rewardClaimQueryService).list(42L);
    }

    @Test
    void authenticationAndRequestValidationAreApplied() throws Exception {
        mockMvc.perform(post("/reward-claims")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"course_enrollment_id":81}
                                """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));

        mockMvc.perform(get("/users/me/reward-claims"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));

        mockMvc.perform(post("/reward-claims")
                        .header("Authorization", "Bearer " + userToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"course_enrollment_id":0}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
    }

    private String userToken() {
        return jwtTokenProvider.generateAccessToken(42L, "user@example.com", Role.USER, null);
    }
}
