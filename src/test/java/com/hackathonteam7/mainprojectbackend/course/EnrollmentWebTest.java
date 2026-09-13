package com.hackathonteam7.mainprojectbackend.course;

import com.hackathonteam7.mainprojectbackend.common.error.GlobalExceptionHandler;
import com.hackathonteam7.mainprojectbackend.config.SecurityConfig;
import com.hackathonteam7.mainprojectbackend.course.dto.user.EnrollmentProgressResponse;
import com.hackathonteam7.mainprojectbackend.course.dto.user.EnrollmentProgressResponse.EnrollmentCourseSummary;
import com.hackathonteam7.mainprojectbackend.course.dto.user.EnrollmentProgressResponse.EnrollmentPlaceItem;
import com.hackathonteam7.mainprojectbackend.course.dto.user.EnrollmentStartResponse;
import com.hackathonteam7.mainprojectbackend.course.dto.user.UserCoursePlaceItem;
import com.hackathonteam7.mainprojectbackend.course.dto.user.UserCourseRewardDetail;
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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(EnrollmentController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class, JwtTokenProvider.class, GlobalExceptionHandler.class})
class EnrollmentWebTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @Autowired JwtTokenProvider jwtTokenProvider;
    @MockitoBean EnrollmentService enrollmentService;
    @MockitoBean EnrollmentQueryService enrollmentQueryService;

    @Test
    void newEnrollmentReturnsCreatedAndOnlyDocumentedFields() throws Exception {
        LocalDateTime startedAt = LocalDateTime.of(2026, 9, 13, 10, 30);
        when(enrollmentService.start(7L, 42L)).thenReturn(new EnrollmentService.EnrollmentStartResult(
                new EnrollmentStartResponse(81L, "active", startedAt), true));

        var response = mockMvc.perform(post("/courses/7/enrollments")
                        .header("Authorization", "Bearer " + userToken()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.enrollment_id").value(81))
                .andExpect(jsonPath("$.status").value("active"))
                .andExpect(jsonPath("$.started_at").value("2026-09-13T10:30:00"))
                .andReturn().getResponse();

        assertThat(objectMapper.readTree(response.getContentAsString()).size()).isEqualTo(3);
        verify(enrollmentService).start(7L, 42L);
    }

    @Test
    void existingEnrollmentReturnsOkWithSameContract() throws Exception {
        when(enrollmentService.start(7L, 42L)).thenReturn(new EnrollmentService.EnrollmentStartResult(
                new EnrollmentStartResponse(81L, "complete", LocalDateTime.of(2026, 9, 1, 9, 0)), false));

        mockMvc.perform(post("/courses/7/enrollments")
                        .header("Authorization", "Bearer " + userToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.enrollment_id").value(81))
                .andExpect(jsonPath("$.status").value("complete"));
    }

    @Test
    void progressReturnsDocumentedFieldsAndReusesCoursePlaceShapeForNextPlace() throws Exception {
        UserCoursePlaceItem nextPlace = new UserCoursePlaceItem(
                12L, 102L, "두 번째 장소", new BigDecimal("37.1234567"),
                new BigDecimal("127.1234567"), 2, "place.jpg", "설명");
        EnrollmentCourseSummary course = new EnrollmentCourseSummary(7L, "성수 코스", true);
        List<EnrollmentPlaceItem> places = List.of(
                new EnrollmentPlaceItem(11L, 101L, "첫 번째 장소", BigDecimal.ONE, BigDecimal.TEN, 1, null, null, true),
                new EnrollmentPlaceItem(12L, 102L, "두 번째 장소", new BigDecimal("37.1234567"),
                        new BigDecimal("127.1234567"), 2, "place.jpg", "설명", false));
        UserCourseRewardDetail reward = new UserCourseRewardDetail(
                3L, "완주 포인트", "point", "설명", null, 100, null);
        when(enrollmentQueryService.getProgress(81L, 42L)).thenReturn(new EnrollmentProgressResponse(
                "active", List.of(11L), 3, nextPlace, null, course, places, reward));

        var response = mockMvc.perform(get("/enrollments/81")
                        .header("Authorization", "Bearer " + userToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("active"))
                .andExpect(jsonPath("$.stamped_course_place_ids[0]").value(11))
                .andExpect(jsonPath("$.total_places").value(3))
                .andExpect(jsonPath("$.next_place.course_place_id").value(12))
                .andExpect(jsonPath("$.next_place.visit_order").value(2))
                .andExpect(jsonPath("$.completed_at").doesNotExist())
                .andExpect(jsonPath("$.course.id").value(7))
                .andExpect(jsonPath("$.course.name").value("성수 코스"))
                .andExpect(jsonPath("$.places[0].stamped").value(true))
                .andExpect(jsonPath("$.places[1].stamped").value(false))
                .andExpect(jsonPath("$.reward.kind").value("point"))
                .andReturn().getResponse();

        assertThat(objectMapper.readTree(response.getContentAsString()).size()).isEqualTo(7);
        verify(enrollmentQueryService).getProgress(81L, 42L);
    }

    @Test
    void anonymousAndInvalidIdentifiersAreRejectedBeforeService() throws Exception {
        mockMvc.perform(post("/courses/7/enrollments"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
        mockMvc.perform(get("/enrollments/81"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
        mockMvc.perform(get("/enrollments/0")
                        .header("Authorization", "Bearer " + userToken()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
    }

    private String userToken() {
        return jwtTokenProvider.generateAccessToken(42L, "user@example.com", Role.USER, null);
    }
}
