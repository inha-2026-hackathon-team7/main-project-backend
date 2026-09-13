package com.hackathonteam7.mainprojectbackend.course;

import com.hackathonteam7.mainprojectbackend.common.error.GlobalExceptionHandler;
import com.hackathonteam7.mainprojectbackend.config.SecurityConfig;
import com.hackathonteam7.mainprojectbackend.course.dto.user.UserCourseDetailResponse;
import com.hackathonteam7.mainprojectbackend.course.dto.user.UserCourseListItem;
import com.hackathonteam7.mainprojectbackend.course.dto.user.UserCoursePlaceItem;
import com.hackathonteam7.mainprojectbackend.course.dto.user.UserCourseRewardDetail;
import com.hackathonteam7.mainprojectbackend.course.dto.user.UserCourseRewardSummary;
import com.hackathonteam7.mainprojectbackend.security.JwtAuthenticationFilter;
import com.hackathonteam7.mainprojectbackend.security.JwtTokenProvider;
import com.hackathonteam7.mainprojectbackend.user.Role;
import java.math.BigDecimal;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(UserCourseController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class, JwtTokenProvider.class, GlobalExceptionHandler.class})
class UserCourseWebTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @Autowired JwtTokenProvider jwtTokenProvider;
    @MockitoBean UserCourseQueryService service;

    @Test
    void anonymousListReturnsOnlySpecifiedFieldsAndForwardsFilters() throws Exception {
        when(service.list(any(), any(), any())).thenReturn(List.of(new UserCourseListItem(
                7L, "성수 코스", "official", 12, "https://example.com/place.jpg",
                new UserCourseRewardSummary(3L, "완주 포인트", "point", "https://example.com/reward.jpg"),
                1L, "성수·서울숲", null, 60)));

        var response = mockMvc.perform(get("/courses")
                        .queryParam("organization_id", "2")
                        .queryParam("region_id", "5")
                        .queryParam("type", "official")
                        .queryParam("page", "1")
                        .queryParam("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(7))
                .andExpect(jsonPath("$[0].type").value("official"))
                .andExpect(jsonPath("$[0].reward_summary.kind").value("point"))
                .andExpect(jsonPath("$[0].region_name").value("성수·서울숲"))
                .andExpect(jsonPath("$[0].duration_minutes").value(60))
                .andExpect(jsonPath("$[0].distance_meters").doesNotExist())
                .andReturn().getResponse();

        var item = objectMapper.readTree(response.getContentAsString()).get(0);
        assertThat(item.size()).isEqualTo(9);
        assertThat(item.get("reward_summary").size()).isEqualTo(4);
        verify(service).list(org.mockito.ArgumentMatchers.argThat(query ->
                query.organizationId().equals(2L)
                        && query.regionId().equals(5L)
                        && query.type() == CourseType.OFFICIAL
                        && query.page() == 1
                        && query.size() == 10), any(), any());
    }

    @Test
    void anonymousDetailOmitsEnrollmentAndUsesDocumentedNestedFields() throws Exception {
        when(service.getDetail(7L, null, null, null)).thenReturn(detail(null));

        var response = mockMvc.perform(get("/courses/7"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.places[0].lat").value(37.1234567))
                .andExpect(jsonPath("$.places[0].lng").value(127.1234567))
                .andExpect(jsonPath("$.reward.kind").value("coupon"))
                .andExpect(jsonPath("$.my_enrollment_id").doesNotExist())
                .andExpect(jsonPath("$.region_name").value("성수·서울숲"))
                .andExpect(jsonPath("$.duration_minutes").value(20))
                .andReturn().getResponse();

        var root = objectMapper.readTree(response.getContentAsString());
        assertThat(root.size()).isEqualTo(9);
        assertThat(root.get("places").get(0).size()).isEqualTo(8);
        assertThat(root.get("reward").size()).isEqualTo(7);
    }

    @Test
    void authenticatedDetailPassesUserIdAndReturnsOwnEnrollment() throws Exception {
        String token = jwtTokenProvider.generateAccessToken(42L, "user@example.com", Role.USER, null);
        when(service.getDetail(7L, 42L, null, null)).thenReturn(detail(81L));

        mockMvc.perform(get("/courses/7").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.my_enrollment_id").value(81));
        verify(service).getDetail(7L, 42L, null, null);
    }

    @Test
    void invalidFiltersUseDocumentedValidationError() throws Exception {
        for (String url : List.of(
                "/courses?type=unknown",
                "/courses?page=-1",
                "/courses?size=0",
                "/courses?size=101",
                "/courses?organization_id=text",
                "/courses?region_id=0",
                "/courses/not-a-number")) {
            mockMvc.perform(get(url))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                    .andExpect(jsonPath("$.details").isArray());
        }
        verifyNoInteractions(service);
    }

    @Test
    void publicReadPermissionDoesNotOpenCourseWrites() throws Exception {
        mockMvc.perform(post("/courses/7/enrollments"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
    }

    private UserCourseDetailResponse detail(Long enrollmentId) {
        return new UserCourseDetailResponse(
                7L,
                "성수 코스",
                "성수 일대 코스",
                true,
                List.of(new UserCoursePlaceItem(
                        11L, 12L, "서울숲", new BigDecimal("37.1234567"),
                        new BigDecimal("127.1234567"), 1, "https://example.com/place.jpg", "서울숲의 야외 피크닉 장소")),
                new UserCourseRewardDetail(
                        3L, "할인 쿠폰", "coupon", "카페 할인", "https://example.com/reward.jpg",
                        20, LocalDateTime.of(2026, 12, 31, 23, 59, 59)),
                enrollmentId,
                1L,
                "성수·서울숲",
                null,
                20
        );
    }
}
