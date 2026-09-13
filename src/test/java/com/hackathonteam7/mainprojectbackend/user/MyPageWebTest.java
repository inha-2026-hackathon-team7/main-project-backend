package com.hackathonteam7.mainprojectbackend.user;

import com.hackathonteam7.mainprojectbackend.common.error.GlobalExceptionHandler;
import com.hackathonteam7.mainprojectbackend.config.SecurityConfig;
import com.hackathonteam7.mainprojectbackend.course.MyEnrollmentController;
import com.hackathonteam7.mainprojectbackend.course.MyEnrollmentQueryService;
import com.hackathonteam7.mainprojectbackend.course.dto.user.MyEnrollmentItem;
import com.hackathonteam7.mainprojectbackend.security.JwtAuthenticationFilter;
import com.hackathonteam7.mainprojectbackend.security.JwtTokenProvider;
import com.hackathonteam7.mainprojectbackend.user.dto.UserProfileResponse;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest({UserMeController.class, MyEnrollmentController.class})
@Import({SecurityConfig.class, JwtAuthenticationFilter.class, JwtTokenProvider.class, GlobalExceptionHandler.class})
class MyPageWebTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @Autowired JwtTokenProvider jwtTokenProvider;
    @MockitoBean UserProfileService userProfileService;
    @MockitoBean MyEnrollmentQueryService myEnrollmentQueryService;

    @Test
    void profileReturnsOnlyDocumentedFields() throws Exception {
        when(userProfileService.getProfile(42L))
                .thenReturn(new UserProfileResponse(42L, "여행자", "traveler@example.com"));

        var response = mockMvc.perform(get("/users/me")
                        .header("Authorization", "Bearer " + userToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(42))
                .andExpect(jsonPath("$.name").value("여행자"))
                .andExpect(jsonPath("$.email").value("traveler@example.com"))
                .andReturn().getResponse();

        assertThat(objectMapper.readTree(response.getContentAsString()).size()).isEqualTo(3);
        verify(userProfileService).getProfile(42L);
    }

    @Test
    void enrollmentListReturnsStableShapeIncludingNullCompletedAt() throws Exception {
        LocalDateTime startedAt = LocalDateTime.of(2026, 9, 13, 10, 30);
        when(myEnrollmentQueryService.list(42L, "active")).thenReturn(List.of(
                new MyEnrollmentItem(81L, 7L, "서울 산책", "active",
                        new MyEnrollmentItem.Progress(1, 3), startedAt, null)));

        var response = mockMvc.perform(get("/users/me/enrollments")
                        .queryParam("status", "active")
                        .header("Authorization", "Bearer " + userToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].enrollment_id").value(81))
                .andExpect(jsonPath("$[0].course_id").value(7))
                .andExpect(jsonPath("$[0].course_name").value("서울 산책"))
                .andExpect(jsonPath("$[0].status").value("active"))
                .andExpect(jsonPath("$[0].progress.done").value(1))
                .andExpect(jsonPath("$[0].progress.total").value(3))
                .andExpect(jsonPath("$[0].started_at").value("2026-09-13T10:30:00"))
                .andReturn().getResponse();

        var item = objectMapper.readTree(response.getContentAsString()).get(0);
        assertThat(item.size()).isEqualTo(7);
        assertThat(item.has("completed_at")).isTrue();
        assertThat(item.get("completed_at").isNull()).isTrue();
        verify(myEnrollmentQueryService).list(42L, "active");
    }

    @Test
    void myPageEndpointsRequireAuthentication() throws Exception {
        mockMvc.perform(get("/users/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
        mockMvc.perform(get("/users/me/enrollments"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
    }

    private String userToken() {
        return jwtTokenProvider.generateAccessToken(42L, "user@example.com", Role.USER, null);
    }
}
